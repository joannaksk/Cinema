package com.example.joanna.cinema.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.joanna.cinema.BuildConfig;
import com.example.joanna.cinema.R;
import com.example.joanna.cinema.data.MovieContract;
import com.example.joanna.cinema.data.MovieProvider;

import java.util.List;
import java.util.Vector;

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbMovies;
import info.movito.themoviedbapi.model.MovieDb;

import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.credits;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.images;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.releases;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.reviews;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.similar;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.videos;

/**
 * Created by joanna on 29/07/16.
 */
public class CinemaSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final int SYNC_INTERVAL = 1000 * 60 * 60 * 24;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;

    final String LOG_TAG = CinemaSyncAdapter.class.getSimpleName();
    private final Context mContext;
    private SharedPreferences sharedPreferences;

    public CinemaSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        this.mContext = context;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        try {
            // Try to fetch the movies.
            TmdbApi tmdbApi = new TmdbApi(BuildConfig.MOVIE_DB_API_KEY);
            TmdbMovies movies = tmdbApi.getMovies();
            List<MovieDb> movies_list;

            // Decide whether to fetch the most popular or the top rated movies.
            String defaultSort = mContext.getString(R.string.pref_sort_order_default);
            String sort = sharedPreferences.getString(mContext.getString(R.string.pref_sort_order_key),
                    defaultSort);

            // Changed this because I was comparing references instead of the actual text.
            if (sort.equals(defaultSort)) {
                movies_list  = movies.getPopularMovies("en", 0).getResults();
                Log.d(LOG_TAG, "FetchMovieTask Complete. Popular Inserted");
            } else {
                movies_list = movies.getTopRatedMovies("en", 0).getResults();
                Log.d(LOG_TAG, "FetchMovieTask Complete. Top Rated Inserted");
            }

            // Insert the new movie information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(movies_list.size());

            for (MovieDb movie : movies_list) {
                MovieDb movie_details = movies.getMovie(movie.getId(), "en", credits, videos, releases, images, similar, reviews);
                ContentValues movieValues = new ContentValues();

                movieValues.put(MovieContract.MovieColumns.COLUMN_MOVIE_ID, movie.getId());
                movieValues.put(MovieContract.MovieColumns.COLUMN_TITLE, movie.getTitle());
                movieValues.put(MovieContract.MovieColumns.COLUMN_RELEASE_DATE, movie.getReleaseDate());
                movieValues.put(MovieContract.MovieColumns.COLUMN_DURATION, movie_details.getRuntime());
                movieValues.put(MovieContract.MovieColumns.COLUMN_POSTER, movie.getPosterPath());
                movieValues.put(MovieContract.MovieColumns.COLUMN_POPULARITY, movie.getPopularity());
                movieValues.put(MovieContract.MovieColumns.COLUMN_VOTE_AVERAGE, movie.getVoteAverage());
                movieValues.put(MovieContract.MovieColumns.COLUMN_OVERVIEW, movie.getOverview());

                // If this movie_id exists in favorites, tag this as a favorites.
                Uri query_uri = ContentUris.withAppendedId(MovieProvider.Favorites.LIST_CONTENT_URI, movie.getId());
                String[] title = {MovieContract.MovieColumns._ID, MovieContract.MovieColumns.COLUMN_TITLE};
                Cursor favorite = mContext.getContentResolver().query(query_uri, title, null, null, null);
                if (favorite.getCount() != 0) {
                    favorite.moveToFirst();
                    if (favorite.getString(1) != null) {
                        String name = favorite.getString(1);
                        movieValues.put(MovieContract.MovieColumns.COLUMN_FAVORITE, 1);
                        Log.d(LOG_TAG, name + "is a favorite");
                    }
                }
                favorite.close();

                cVVector.add(movieValues);
                Log.d(LOG_TAG, "FetchMovieTask Adding ID: " + movie.getId() + " : " + movie.getTitle() + " To Be Inserted");
            }

            // Add new data to database
            if ( cVVector.size() > 0 ) {
                // Clear the database because I only want to have 20 items in there at any time.
                int rowsDeleted = mContext.getContentResolver().delete(MovieProvider.Movies.LIST_CONTENT_URI, null, null);
                Log.d(LOG_TAG, "FetchMovieTask Deleting Old Data: " + rowsDeleted + " Deleted");
                // Proceed to insert new data.
                int rowsInserted = mContext.getContentResolver().bulkInsert(
                        MovieProvider.Movies.LIST_CONTENT_URI,
                        cVVector.toArray(new ContentValues[cVVector.size()])
                );
                Log.d(LOG_TAG, "FetchMovieTask Complete. " + rowsInserted + " Inserted");
            }
            return;
        }
        catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);

        }
        return newAccount;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        CinemaSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}