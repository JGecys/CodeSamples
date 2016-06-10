package com.sneakybox.kaunobiblioteka.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.sneakybox.kaunobiblioteka.BaseActivity;
import com.sneakybox.kaunobiblioteka.Config;
import com.sneakybox.kaunobiblioteka.R;
import com.sneakybox.kaunobiblioteka.TutorialActivity;
import com.sneakybox.kaunobiblioteka.about.AboutFragment;
import com.sneakybox.kaunobiblioteka.ask.AskActivity;
import com.sneakybox.kaunobiblioteka.contacts.ContactsFragment;
import com.sneakybox.kaunobiblioteka.events.EventActivity;
import com.sneakybox.kaunobiblioteka.events.EventDayListActivity;
import com.sneakybox.kaunobiblioteka.events.EventsFragment;
import com.sneakybox.kaunobiblioteka.events.OnEventItemInteractionListener;
import com.sneakybox.kaunobiblioteka.events.calendar.MonthView;
import com.sneakybox.kaunobiblioteka.maps.MapsFragment;
import com.sneakybox.kaunobiblioteka.models.Event;
import com.sneakybox.kaunobiblioteka.models.News;
import com.sneakybox.kaunobiblioteka.news.NewsActivity;
import com.sneakybox.kaunobiblioteka.news.NewsFragment;
import com.sneakybox.kaunobiblioteka.poll.PollActivity;
import com.sneakybox.kaunobiblioteka.settings.SettingsActivity;
import com.sneakybox.kaunobiblioteka.tests.Question;

import butterknife.Bind;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnEventItemInteractionListener,
        NewsFragment.OnNewsItemInteractionListener,
        MonthView.MonthViewListener {

    private DrawerLayout drawerLayout;
    @Bind(R.id.nav_view)
    NavigationView navigationView;

    @Bind(R.id.content)
    View content;

    private long backButtonPressedAt = 0;

    private int currentFragment = R.id.nav_news;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (!getPreferences().isTutorialFinished()) {
            TutorialActivity.start(this);
        }
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content, NewsFragment.newInstance())
                    .commit();
        }
        ImageView navigationBackground = new ImageView(this);
        navigationBackground.setImageResource(R.drawable.bg_navigation);
        navigationBackground.setScaleType(ImageView.ScaleType.CENTER_CROP);
        navigationView.addView(navigationBackground, 0);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(currentFragment);
    }

    @Override
    public void setSupportActionBar(Toolbar toolbar) {
        super.setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggleButton = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.setDrawerListener(toggleButton);
        toggleButton.syncState();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            long current = System.currentTimeMillis();
            if (current - backButtonPressedAt > 2000) { // Press twice to close
                backButtonPressedAt = current;
                Snackbar.make(content, R.string.toast_backbutton, Snackbar.LENGTH_SHORT).show();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == currentFragment) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        if (!navigateToId(id)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return false;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean navigateToId(int id) {
        switch (id) {
            case R.id.nav_news:
                replaceContent(NewsFragment.newInstance());
                currentFragment = id;
                break;
            case R.id.nav_LIBIS:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.URL_CATALOG)));
                drawerLayout.closeDrawer(GravityCompat.START);
                return false;
            case R.id.nav_guide:
                replaceContent(MapsFragment.newInstance());
                currentFragment = id;
                break;
            case R.id.nav_events:
                replaceContent(EventsFragment.newInstance());
                currentFragment = id;
                break;
            case R.id.nav_settings:
                startActivity(new Intent(this, SettingsActivity.class));
//                navigationView.setCheckedItem(currentFragment);
                drawerLayout.closeDrawer(GravityCompat.START);
                return false;
            case R.id.nav_contacts:
                replaceContent(ContactsFragment.newInstance());
                currentFragment = id;
                break;
            case R.id.nav_ask:
                startActivity(new Intent(this, AskActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
                return false;
            case R.id.nav_poll:
                Question poll = getPreferences().getPoll();
                if (poll == null || poll.title == null || poll.title.isEmpty()) {
                    Toast.makeText(this, R.string.text_no_poll, Toast.LENGTH_LONG).show();
                } else {
                    startActivity(new Intent(this, PollActivity.class));
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return false;
            case R.id.nav_about:
                replaceContent(AboutFragment.newInstance());
                currentFragment = id;
                break;
            default:
                Snackbar.make(content, "Not implemented", Snackbar.LENGTH_SHORT).show();
                break;
        }
        return true;
    }

    private void replaceContent(Fragment fragment) {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.content, fragment)
                .commit();
    }

    @Override
    public void onEventItemInteraction(Event item) {
        EventActivity.open(this, item.id);
    }

    @Override
    public void onNewsItemInteraction(News item) {
        NewsActivity.open(this, item.id);
    }

    @Override
    public void onDayClicked(int year, int month, int day) {
        EventDayListActivity.open(this, year, month, day);
    }
}