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

public class MainActivity extends AppCompatActivity implements MoviesFragment.Callback {
    // mSort is the sort currently stored in the Shared Preferences.
    private String mSort;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSort = mPrefs.getString(this.getString(R.string.pref_sort_order_key),
                this.getString(R.string.pref_sort_order_default));

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(Uri movieUri) {
        Intent intent = new Intent(this, DetailActivity.class)
                .setData(movieUri);
        startActivity(intent);
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
