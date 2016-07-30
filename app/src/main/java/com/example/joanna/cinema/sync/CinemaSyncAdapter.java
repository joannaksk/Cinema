package com.example.joanna.cinema.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.example.joanna.cinema.BuildConfig;
import com.example.joanna.cinema.MainActivity;
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

    private static final String[] NOTIFY_MOVIE_PROJECTION = {
            MovieContract.MovieColumns._ID,
            MovieContract.MovieColumns.COLUMN_MOVIE_ID,
            MovieContract.MovieColumns.COLUMN_TITLE,
            MovieContract.MovieColumns.COLUMN_RELEASE_DATE,
    };
    private static final int INDEX_MOVIE_TITLE = 2;
    private static final int INDEX_MOVIE_RELEASE_DATE = 3;
    private static final int MOVIE_NOTIFICATION_ID = 1806;
    public static final String DATA_SET_CHANGED = "dataSetChanged";
    public static  int SYNC_INTERVAL;
    public static  int SYNC_FLEXTIME;
    private long start_of_sync;
    private long end_of_sync;
    private long time_from_last_sync;
    private boolean first_sync;

    final String LOG_TAG = CinemaSyncAdapter.class.getSimpleName();
    private final Context mContext;
    private static SharedPreferences sharedPreferences;
    private boolean dataSetChanged;

    public CinemaSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        this.mContext = context;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        start_of_sync = System.currentTimeMillis();
        Log.d(LOG_TAG, "Start of Sync : " + start_of_sync);

        first_sync = (end_of_sync == 0)? true : false;

        dataSetChanged = false;

        if (end_of_sync != 0) {
           time_from_last_sync  = start_of_sync - end_of_sync;
        }

        if (first_sync || (time_from_last_sync >= 10000)) {
            try {
                // Try to fetch the movies.
                TmdbApi tmdbApi = new TmdbApi(BuildConfig.MOVIE_DB_API_KEY);
                TmdbMovies movies = tmdbApi.getMovies();
                List<MovieDb> movies_list;

                // Decide whether to fetch the most popular or the top rated movies.
                String defaultSort = mContext.getString(R.string.pref_sort_order_default);
                sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                String sort = sharedPreferences.getString(mContext.getString(R.string.pref_sort_order_key),
                        defaultSort);

                // Changed this because I was comparing references instead of the actual text.
                if (sort.equals(defaultSort)) {
                    movies_list = movies.getPopularMovies("en", 0).getResults();
                    Log.d(LOG_TAG, "Popular Inserted");
                } else {
                    movies_list = movies.getTopRatedMovies("en", 0).getResults();
                    Log.d(LOG_TAG, "Top Rated Inserted");
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
                if (cVVector.size() > 0) {
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

                dataSetChanged = true;
                notifyMovie();

                end_of_sync = System.currentTimeMillis();
                Log.d(LOG_TAG, "End of Sync : " + end_of_sync);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
        }
        Intent i = new Intent(CinemaSyncService.ACTION_SYNC_FINISHED);
        i.putExtra(DATA_SET_CHANGED, dataSetChanged);
        mContext.sendBroadcast(i);
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
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SYNC_INTERVAL = Integer.valueOf(
                sharedPreferences.getString(context.getString(R.string.pref_sync_frequency_key),
                        context.getString(R.string.pref_sync_frequency_default)));
        SYNC_FLEXTIME = SYNC_INTERVAL/3;
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

    private void notifyMovie() {
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = sharedPreferences.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if (displayNotifications) {

            String defaultSort = mContext.getString(R.string.pref_sort_order_default);
            String favoritesSort = mContext.getString(R.string.pref_sort_order_favorites);
            String sort = sharedPreferences.getString(mContext.getString(R.string.pref_sort_order_key),
                    defaultSort);
            if (!sort.equals(favoritesSort)) {

                Uri movieUri = MovieProvider.Movies.LIST_CONTENT_URI;

                // we'll query our contentProvider, as always
                Cursor cursor = context.getContentResolver().query(movieUri, NOTIFY_MOVIE_PROJECTION, null, null, null);

                if (cursor.moveToFirst()) {
                    String movie_title = cursor.getString(INDEX_MOVIE_TITLE);
                    String movie_release_date = cursor.getString(INDEX_MOVIE_RELEASE_DATE);

                    String title = context.getString(R.string.app_name);

                    String sort_title = (sort.equals(defaultSort))? "Most Popular" : "Top Rated";

                    // Define the text of the forecast.
                    String contentText = String.format(context.getString(R.string.format_notification),
                            sort_title,
                            movie_title,
                            movie_release_date
                    );

                    //build your notification here.

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_cinema_notification)
                            .setContentTitle(title)
                            .setContentText(contentText);
                    Intent intent = new Intent(context, MainActivity.class);

                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addParentStack(MainActivity.class);
                    stackBuilder.addNextIntent(intent);

                    PendingIntent pendingIntent = stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
                    builder.setContentIntent(pendingIntent);
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(MOVIE_NOTIFICATION_ID, builder.build());

                }

            }
        }
    }
}
