package com.example.joanna.cinema.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.joanna.cinema.Constants;
import com.example.joanna.cinema.data.MovieContract;
import com.example.joanna.cinema.data.MovieProvider;

import java.util.Vector;

/**
 * Created by joanna on 29/07/16.
 */
public class AddFavoritesService extends IntentService {
    private String LOG_TAG = AddFavoritesService.class.getSimpleName();
    public static String MOVIE_DETAILS = "movie_details";
    // Defines and instantiates an object for handling status updates.
    private LocalBroadcastManager mBroadcaster;

    public AddFavoritesService() {
        super("Cinema : Add Favorite");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.hasExtra(MOVIE_DETAILS)) {
            Bundle movie_details = intent.getBundleExtra(MOVIE_DETAILS);
            try {
                String movie_id = movie_details.getString(MovieContract.FavoriteColumns.COLUMN_MOVIE_ID);

                // Insert the new movie information into the database
                Vector<ContentValues> cVVector = new Vector<ContentValues>(1);

                ContentValues favoriteValues = new ContentValues();
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_MOVIE_ID,
                        movie_id);
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_TITLE,
                        movie_details.getString(MovieContract.FavoriteColumns.COLUMN_TITLE));
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_RELEASE_DATE,
                        movie_details.getString(MovieContract.FavoriteColumns.COLUMN_RELEASE_DATE));
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_DURATION,
                        movie_details.getString(MovieContract.FavoriteColumns.COLUMN_DURATION));
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_POSTER,
                        movie_details.getString(MovieContract.FavoriteColumns.COLUMN_POSTER));
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_POPULARITY,
                        movie_details.getString(MovieContract.FavoriteColumns.COLUMN_POPULARITY));
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_VOTE_AVERAGE,
                        movie_details.getString(MovieContract.FavoriteColumns.COLUMN_VOTE_AVERAGE));
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_OVERVIEW,
                        movie_details.getString(MovieContract.FavoriteColumns.COLUMN_OVERVIEW));

                cVVector.add(favoriteValues);
                // Proceed to insert new data.
                int rowsInserted = this.getContentResolver().bulkInsert(
                        MovieProvider.Favorites.LIST_CONTENT_URI,
                        cVVector.toArray(new ContentValues[cVVector.size()])
                );


                ContentValues update = favoriteValues;
                update.put(MovieContract.MovieColumns.COLUMN_FAVORITE, 1);
                String[] where = {movie_id};

                int rowUpdated = this.getContentResolver().update(
                        MovieProvider.Movies.LIST_CONTENT_URI,
                        update,
                        "movie_id=?",
                        where
                        );

                if (rowsInserted != 0 && rowUpdated != 0 ) {

                    Log.d(LOG_TAG, "Add Favorite Complete. " + rowsInserted + " Inserted");
                    mBroadcaster = LocalBroadcastManager.getInstance(this);

                    Intent localIntent = new Intent();

                    // The Intent contains the custom broadcast action for this app
                    localIntent.setAction(Constants.BROADCAST_ACTION);

                    // Puts the status into the Intent
                    localIntent.putExtra(Constants.ADD_STATUS, Constants.STATE_ACTION_COMPLETE);
                    mBroadcaster.sendBroadcast(localIntent);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
        }
        return;

    }
}
