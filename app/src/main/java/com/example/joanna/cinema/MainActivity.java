package com.example.joanna.cinema;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.example.joanna.cinema.sync.CinemaSyncAdapter;

public class MainActivity extends AppCompatActivity implements MoviesFragment.Callback {
    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    // mSort is the sort currently stored in the Shared Preferences.
    private String mSort;
    private SharedPreferences mPrefs;
    public boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSort = mPrefs.getString(this.getString(R.string.pref_sort_order_key),
                this.getString(R.string.pref_sort_order_default));

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (findViewById(R.id.movie_detail_container) != null) {
            mTwoPane = true;
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.movie_detail_container, new DetailActivityFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
            getSupportActionBar().setElevation(0f);
        }

        CinemaSyncAdapter.initializeSyncAdapter(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(Uri movieUri) {

        if (mTwoPane) {
            Bundle args = new Bundle();
            args.putParcelable(DetailActivityFragment.DETAIL_URI, movieUri);

            DetailActivityFragment fragment = new DetailActivityFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.movie_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(movieUri);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        String sort = mPrefs.getString(this.getString(R.string.pref_sort_order_key),
                this.getString(R.string.pref_sort_order_default));
        String currentSort = mPrefs.getString("current_sort",
                this.getString(R.string.pref_sort_order_default));

        // If the sort is different from the last stored current sort, update the movies fragment.
        if (sort != null && !sort.equals(currentSort)) {
            // update the sort in our movies pane using the fragment manager
            MoviesFragment mf = (MoviesFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
            if ( null != mf ) {
                mf.onSortChanged();
            }
            // Change the value of mSort.
            mSort = sort;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // I'm persisting mSort under a different name so that I can check for the original value
        // later. This is safe even on devices that may destroy the activity directly after onStop()
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putString("current_sort", mSort);
        ed.commit();
    }
}
