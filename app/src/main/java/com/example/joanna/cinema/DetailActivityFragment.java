package com.example.joanna.cinema;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joanna.cinema.data.MovieContract;
import com.example.joanna.cinema.service.AddFavoritesService;
import com.squareup.picasso.Picasso;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    static final String DETAIL_URI = "URI";

    private static final int MOVIE_DETAIL_LOADER = 0;
    public static final String DETAILS_SCROLL_POSITION = "DETAILS_SCROLL_POSITION";
    private Uri dUri;

    private static final String[] MOVIE_DETAIL_COLUMNS = {
            MovieContract.MovieColumns._ID,
            MovieContract.MovieColumns.COLUMN_MOVIE_ID,
            MovieContract.MovieColumns.COLUMN_TITLE,
            MovieContract.MovieColumns.COLUMN_RELEASE_DATE,
            MovieContract.MovieColumns.COLUMN_DURATION,
            MovieContract.MovieColumns.COLUMN_POSTER,
            MovieContract.MovieColumns.COLUMN_POPULARITY,
            MovieContract.MovieColumns.COLUMN_VOTE_AVERAGE,
            MovieContract.MovieColumns.COLUMN_OVERVIEW,
            MovieContract.MovieColumns.COLUMN_FAVORITE
    };

    private static final String[] FAVORITE_DETAIL_COLUMNS = {
            MovieContract.FavoriteColumns._ID,
            MovieContract.FavoriteColumns.COLUMN_MOVIE_ID,
            MovieContract.FavoriteColumns.COLUMN_TITLE,
            MovieContract.FavoriteColumns.COLUMN_RELEASE_DATE,
            MovieContract.FavoriteColumns.COLUMN_DURATION,
            MovieContract.FavoriteColumns.COLUMN_POSTER,
            MovieContract.FavoriteColumns.COLUMN_POPULARITY,
            MovieContract.FavoriteColumns.COLUMN_VOTE_AVERAGE,
            MovieContract.FavoriteColumns.COLUMN_OVERVIEW,
    };

    static final int COL_MOVIE_DBID = 0;
    static final int COL_MOVIE_ID = 1;
    static final int COL_MOVIE_TITLE = 2;
    static final int COL_MOVIE_RELEASE_DATE = 3;
    static final int COL_MOVIE_DURATION = 4;
    static final int COL_MOVIE_POSTER = 5;
    static final int COL_MOVIE_POPULARITY = 6;
    static final int COL_MOVIE_VOTE_AVERAGE = 7;
    static final int COL_MOVIE_OVERVIEW = 8;
    static final int COL_MOVIE_FAVORITE = 9;

    private TextView movieTitleTextView;
    private ImageView posterView;
    private TextView yearTextView;
    private TextView durationTextView;
    private TextView voteAverageTextView;
    private TextView overviewTextView;
    private Cursor detailsCursor;
    private Button favoritesbutton;

    // An instance of the status broadcast receiver
    DownloadStateReceiver mDownloadStateReceiver;
    private ScrollView scrollView;
    public int[] position;

    public DetailActivityFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Creates an intent filter for DownloadStateReceiver that intercepts broadcast Intents
         */

        // The filter's action is BROADCAST_ACTION
        IntentFilter statusIntentFilter = new IntentFilter(
                Constants.BROADCAST_ACTION);

        // Sets the filter's category to DEFAULT
        statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);

        // Instantiates a new DownloadStateReceiver
        mDownloadStateReceiver = new DownloadStateReceiver();

        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mDownloadStateReceiver,
                statusIntentFilter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        scrollView = (ScrollView) rootView.findViewById(R.id.details_scroll_view);

        if (savedInstanceState != null) {
            position = savedInstanceState.getIntArray(DETAILS_SCROLL_POSITION);
            if (position != null) {
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.scrollTo(position[0], position[1]);
                    }
                });
            }
        }

        movieTitleTextView = (TextView) rootView.findViewById(R.id.textview_movie_title);
        posterView = (ImageView) rootView.findViewById(R.id.posterView);
        yearTextView = (TextView) rootView.findViewById(R.id.textView_year);
        durationTextView = (TextView) rootView.findViewById(R.id.textView_duration);
        voteAverageTextView = (TextView) rootView.findViewById(R.id.textView_vote_average);
        overviewTextView = (TextView) rootView.findViewById(R.id.textView_overview);

        // Get the Favorites Button and set a Listener.
        favoritesbutton = (Button) rootView.findViewById(R.id.favoritesbutton);
        favoritesbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFavorite();
            }
        });

        Bundle arguments = getArguments();
        if (arguments != null) {
            dUri = arguments.getParcelable(DetailActivityFragment.DETAIL_URI);

            // Add the extras fragment.
            String movie_id = dUri.getLastPathSegment();
            ExtrasFragment extras = new ExtrasFragment();
            Bundle bundle = new Bundle();
            bundle.putString("movie_id", movie_id);
            extras.setArguments(bundle);
            this.getChildFragmentManager().beginTransaction().add(R.id.extras_fragment, extras).commit();
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(MOVIE_DETAIL_LOADER, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray(DETAILS_SCROLL_POSITION,
                new int[]{scrollView.getScrollX(), scrollView.getScrollY()});
    }

    private boolean usedFavoriteUri(){
        if(dUri.toString().contains("favorite")) {
            return true;
        }
        return false;
    }

    private void addFavorite() {
        Intent intent = new Intent(getActivity(), AddFavoritesService.class);
        Bundle details = parcelData(detailsCursor);
        intent.putExtra(AddFavoritesService.MOVIE_DETAILS, details);
        getActivity().startService(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(LOG_TAG, "In onCreateLoader");

        String[] COLUMNS;
        if(usedFavoriteUri()){
            COLUMNS = FAVORITE_DETAIL_COLUMNS;
        } else {
            COLUMNS = MOVIE_DETAIL_COLUMNS;
        }


        if ( null != dUri ) {
            return new CursorLoader(
                    getActivity(),
                    dUri,
                    COLUMNS,
                    null,
                    null,
                    null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(LOG_TAG, "In onLoadFinished");
        if (!data.moveToFirst()) { return; }
        detailsCursor = data;

        // Title
        String title = data.getString(COL_MOVIE_TITLE);
        movieTitleTextView.setText(title);
        // Set the Content Description for icons so Talkback can read it out.
        posterView.setContentDescription(title);

        // Poster.
        String base_path = BuildConfig.MOVIE_POSTER_BASE_PATH;
        String poster_path = data.getString(COL_MOVIE_POSTER);
        Uri poster_uri = Uri.parse(base_path+poster_path);
        if (poster_uri != null) {
            Picasso
                    .with(getContext())
                    .load(poster_uri)
                    .into(posterView);
        } else {
            //Todo Add placeholder image
            posterView.setImageResource(R.drawable.ic_game_of_thrones);
        }

        // Year
        String date = data.getString(COL_MOVIE_RELEASE_DATE);
        String year = date.substring(0,4);
        yearTextView.setText(year);

        // Duration
        String duration = data.getString(COL_MOVIE_DURATION);
        durationTextView.setText(duration+"min");

        // Vote Average
        String vote_average = data.getString(COL_MOVIE_VOTE_AVERAGE);
        voteAverageTextView.setText(vote_average+"/10");

        // Overview
        String overview = data.getString(COL_MOVIE_OVERVIEW);
        overviewTextView.setText(overview);

        // Hide favoritesbutton if this is already a fave.
        if (usedFavoriteUri() || (!usedFavoriteUri() && data.getInt(COL_MOVIE_FAVORITE) != 0)) {
            favoritesbutton.setVisibility(View.GONE);
        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onDestroy() {
        // If the DownloadStateReceiver still exists, unregister it and set it to null
        if (mDownloadStateReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDownloadStateReceiver);
            mDownloadStateReceiver = null;
        }
        super.onDestroy();
    }

    private Bundle parcelData(Cursor cursor) {
        Bundle bundle = new Bundle();
        bundle.putString(MovieContract.FavoriteColumns.COLUMN_MOVIE_ID, cursor.getString(COL_MOVIE_ID));
        bundle.putString(MovieContract.FavoriteColumns.COLUMN_TITLE, cursor.getString(COL_MOVIE_TITLE));
        bundle.putString(MovieContract.FavoriteColumns.COLUMN_RELEASE_DATE, cursor.getString(COL_MOVIE_RELEASE_DATE));
        bundle.putString(MovieContract.FavoriteColumns.COLUMN_DURATION, cursor.getString(COL_MOVIE_DURATION));
        bundle.putString(MovieContract.FavoriteColumns.COLUMN_POSTER, cursor.getString(COL_MOVIE_POSTER));
        bundle.putString(MovieContract.FavoriteColumns.COLUMN_POPULARITY, cursor.getString(COL_MOVIE_POPULARITY));
        bundle.putString(MovieContract.FavoriteColumns.COLUMN_VOTE_AVERAGE, cursor.getString(COL_MOVIE_VOTE_AVERAGE));
        bundle.putString(MovieContract.FavoriteColumns.COLUMN_OVERVIEW, cursor.getString(COL_MOVIE_OVERVIEW));
        return bundle;
    }

    /**
     * This class uses the BroadcastReceiver framework to detect and handle status messages from
     * the service that downloads adds Favorites.
     */
    private class DownloadStateReceiver extends BroadcastReceiver {

        private DownloadStateReceiver() {

            // prevents instantiation by other packages.
        }
        /**
         *
         * This method is called by the system when a broadcast Intent is matched by this class'
         * intent filters
         *
         * @param context An Android context
         * @param intent The incoming broadcast Intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            /*
             * Gets the status from the Intent's extended data, and chooses the appropriate action
             */
            if (intent.getBooleanExtra(Constants.ADD_STATUS, Constants.STATE_ACTION_INCOMPLETE)) {
                favoritesbutton.setVisibility(View.GONE);
                Toast favoriteAdded = Toast.makeText(getActivity(), "Favorite Added", Toast.LENGTH_SHORT);
                favoriteAdded.show();
            }
        }
    }
}
