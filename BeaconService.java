package com.sneakybox.kaunobiblioteka.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.test.mock.MockApplication;
import android.util.Log;

import com.sneakybox.kaunobiblioteka.BaseApplication;
import com.sneakybox.kaunobiblioteka.BuildConfig;
import com.sneakybox.kaunobiblioteka.models.Department;
import com.sneakybox.kaunobiblioteka.tools.database.DatabaseInterface;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class BeaconService extends Service implements BeaconConsumer, RangeNotifier {
    private static final String TAG = "BeaconService";

    public static final String BEACON_BROADCAST = BuildConfig.APPLICATION_ID + ".BEACON_BROADCAST";
    public static final String EXTRA_MAJOR_ID = "majorId";
    public static final String EXTRA_MINOR_ID = "minorId";

    public static final long BEACON_FOREGROUND_UPDATE = 2500;
    public static final long BEACON_BACKGROUND_UPDATE = 15000;

    private static final String BEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"; // kontakt v2.2
    private static final String BEACON_ID1 = ...;


    private BeaconManager beaconManager;
    private Region baseRegion;

    private Beacon previouslyFoundBeacon;
    private int beaconNotFoundCount = 0;

    private boolean notifiedAboutSound = false;
    private DatabaseInterface database;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        database = ((BaseApplication) getApplication()).getDatabase();
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BEACON_LAYOUT));
        beaconManager.bind(this);
    }

    @Override
    public void onDestroy() {
        stopRangingBeaconsInRegion();
        beaconManager.unbind(this);
        super.onDestroy();
    }


    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setBackgroundBetweenScanPeriod(BEACON_BACKGROUND_UPDATE);
        beaconManager.setBackgroundScanPeriod(200);
        beaconManager.setForegroundScanPeriod(BEACON_FOREGROUND_UPDATE);
        beaconManager.setForegroundBetweenScanPeriod(100);
        beaconManager.setRangeNotifier(this);
        startRangingBeaconsInRegion();
    }

    public void startRangingBeaconsInRegion() {
        try {
            initBaseRegion();
            BeaconManager.setRegionExitPeriod(60000); // check every minute
            beaconManager.setMonitorNotifier(new MonitorNotifier() {
                @Override
                public void didEnterRegion(Region region) {

                }

                @Override
                public void didExitRegion(Region region) {
                    //If exit region send null broadcast
                    previouslyFoundBeacon = null;
                    Intent intent = new Intent(BEACON_BROADCAST);
                    sendBroadcast(intent);
                }

                @Override
                public void didDetermineStateForRegion(int i, Region region) {

                }
            });
            beaconManager.startRangingBeaconsInRegion(baseRegion);
        } catch (RemoteException cause) {
            Log.w(TAG, cause);
        }
    }

    private void initBaseRegion() {
        if (baseRegion == null) {
            baseRegion = new Region(
                    BuildConfig.APPLICATION_ID + ".baseRegion",
                    Identifier.parse(BEACON_ID1),
                    null,
                    null
            );
        }
    }

    public void stopRangingBeaconsInRegion() {
        try {
            beaconManager.stopRangingBeaconsInRegion(baseRegion);
        } catch (RemoteException cause) {
            Log.w(TAG, cause);
        }
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        if (region.equals(baseRegion)) {
            if (!beacons.isEmpty()) {
                Beacon closest = Collections.max(beacons, new Comparator<Beacon>() {
                    @Override
                    public int compare(Beacon lhs, Beacon rhs) {
                        return Double.compare(lhs.getRssi(), rhs.getRssi());
                    }
                });
                if (previouslyFoundBeacon == null
                        || !(previouslyFoundBeacon.getId2().equals(closest.getId2())
                        && previouslyFoundBeacon.getId3().equals(closest.getId3()))) {
                    onBeaconFound(closest);
                    previouslyFoundBeacon = closest;
                    beaconNotFoundCount = 0;
                }
            } else {
                if (previouslyFoundBeacon != null && ++beaconNotFoundCount > 20) {
                    previouslyFoundBeacon = null;
                }
            }

        }
    }

    protected void onBeaconFound(Beacon closest) {
        String minorId = closest.getId3().toString();
        String majorId = closest.getId2().toString();
        if (!notifiedAboutSound) {
            database.queryDepartments(majorId, minorId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Department>() {
                        @Override
                        public void call(Department department) {
                            if (department != null) {
                                SoundMuteNotification.notify(BeaconService.this);
                                notifiedAboutSound = true;
                            }
                        }
                    });
        }
        Intent intent = new Intent(BEACON_BROADCAST);
        intent.putExtra(EXTRA_MAJOR_ID, majorId);
        intent.putExtra(EXTRA_MINOR_ID, minorId);
        sendBroadcast(intent);
    }

}