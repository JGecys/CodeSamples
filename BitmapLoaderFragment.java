package com.sneakybox.jasiunudvaromuziejus.fragments;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class BitmapLoaderFragment extends BaseFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bitmaps = new ArrayList<>();
    }

    @Override
    public void onDestroyView() {
        if (bitmaps != null) {
            for (Bitmap bit : bitmaps) {
                if (bit != null) {
                    bit.recycle();
                }
            }
            bitmaps.clear();
        }
        bitmaps = null;
        super.onDestroyView();
    }

    private ArrayList<Bitmap> bitmaps;

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

        private final WeakReference<ImageView> imageViewReference;
        protected String path = null;
        private OnWorkDoneListener onWorkDoneListener = null;

        public BitmapWorkerTask(ImageView imagesLinearLayout) {
            imageViewReference = new WeakReference<>(imagesLinearLayout);
        }

        public final void setOnWorkDoneListener(OnWorkDoneListener listener){
            onWorkDoneListener = listener;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            path = params[0];
            if (getActivity() != null) {
                try {
                    InputStream input = getExpansionFile(path);
                    if (input != null) {
                        Bitmap bitmap = decodeBitmap(input);
                        input.close();
                        return bitmap;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        protected Bitmap decodeBitmap(InputStream stream){
            return BitmapFactory.decodeStream(stream);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap.recycle();
                bitmap = null;
            }

            if (imageViewReference.get() != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask =
                        getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask) {
                    imageView.setImageBitmap(bitmap);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        imageView.setAlpha(0f);
                        imageView.animate().alpha(1f).setDuration(400).start();
                    }
                    if(bitmaps != null) bitmaps.add(bitmap);
                    else{
                        bitmap.recycle();
                    }
                }
                if(onWorkDoneListener != null){
                    onWorkDoneListener.OnWorkDone(imageView);
                }
            }

        }
    }

    public class BitmapWorkerTaskScaled extends BitmapWorkerTask{

        private final int width;

        public BitmapWorkerTaskScaled(ImageView imagesLinearLayout, int requiredWidth) {
            super(imagesLinearLayout);
            this.width = requiredWidth;
        }

        @Override
        protected Bitmap decodeBitmap(InputStream stream) {
            return decodeSampledBitmapFromResource(stream, path, width);
        }
    }

    public interface OnWorkDoneListener{
        void OnWorkDone(ImageView view);
    }

    public void loadBitmap(String path, ImageView imageView, OnWorkDoneListener listener){
        if (cancelPotentialWork(path, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            task.setOnWorkDoneListener(listener);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(getResources(), null, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(path);
        }
    }

    public void loadBitmap(String path, ImageView imageView, int requiredWidth, OnWorkDoneListener listener){
        if (cancelPotentialWork(path, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTaskScaled(imageView, requiredWidth);
            task.setOnWorkDoneListener(listener);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(getResources(), null, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(path);
        }
    }


    public static boolean cancelPotentialWork(String path, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.path;
            if (bitmapData == null || !bitmapData.equals(path)) {
                bitmapWorkerTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth) {
        // Raw height and width of image
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (width > reqWidth) {
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public Bitmap decodeSampledBitmapFromResource(InputStream stream, String path, int reqWidth) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, options);

        InputStream secondStream = getExpansionFile(path);

        Bitmap result = null;
        try {
        // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            result = BitmapFactory.decodeStream(secondStream, null, options);
            secondStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

}