package com.example.joanna.cinema;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.joanna.cinema.data.MovieContract;
import com.squareup.picasso.Picasso;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    static final String DETAIL_URI = "URI";

    private static final int MOVIE_DETAIL_LOADER = 0;
    private Uri dUri;

    private static final String[] MOVIE_DETAIL_COLUMNS = {
            MovieContract.MovieColumns._ID,
            MovieContract.MovieColumns.COLUMN_TITLE,
            MovieContract.MovieColumns.COLUMN_RELEASE_DATE,
            MovieContract.MovieColumns.COLUMN_DURATION,
            MovieContract.MovieColumns.COLUMN_POSTER,
            MovieContract.MovieColumns.COLUMN_POPULARITY,
            MovieContract.MovieColumns.COLUMN_VOTE_AVERAGE,
            MovieContract.MovieColumns.COLUMN_OVERVIEW,
    };
    static final int COL_MOVIE_DBID = 0;
    static final int COL_MOVIE_TITLE = 1;
    static final int COL_MOVIE_RELEASEE_DATE = 2;
    static final int COL_MOVIE_DURATION = 3;
    static final int COL_MOVIE_POSTER = 4;
    static final int COL_MOVIE_POPULARITY = 5;
    static final int COL_MOVIE_VOTE_AVERAGE = 6;
    static final int COL_MOVIE_OVERVIEW = 7;

    private TextView movieTitleTextView;
    private ImageView posterView;
    private TextView yearTextView;
    private TextView durationTextView;
    private TextView voteAverageTextView;
    private TextView overviewTextView;

    public DetailActivityFragment() {
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(MOVIE_DETAIL_LOADER, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        Bundle arguments = getArguments();
        if (arguments != null) {
            dUri = arguments.getParcelable(DetailActivityFragment.DETAIL_URI);
        }

        movieTitleTextView = (TextView) rootView.findViewById(R.id.textview_movie_title);
        posterView = (ImageView) rootView.findViewById(R.id.posterView);
        yearTextView = (TextView) rootView.findViewById(R.id.textView_year);
        durationTextView = (TextView) rootView.findViewById(R.id.textView_duration);
        voteAverageTextView = (TextView) rootView.findViewById(R.id.textView_vote_average);
        overviewTextView = (TextView) rootView.findViewById(R.id.textView_overview);

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(LOG_TAG, "In onCreateLoader");
        if ( null != dUri ) {
            return new CursorLoader(
                    getActivity(),
                    dUri,
                    MOVIE_DETAIL_COLUMNS,
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
        String date = data.getString(COL_MOVIE_RELEASEE_DATE);
        String year = date.substring(0,4);
        yearTextView.setText(year);

        // Duration
        String duration = data.getString(COL_MOVIE_DURATION);
        durationTextView.setText(duration+"min");

        // Vote Average
        String vote_average = data.getString(COL_MOVIE_VOTE_AVERAGE);
        voteAverageTextView.setText(vote_average+"/10");

        // Popularity
        String popularity = data.getString(COL_MOVIE_POPULARITY);

        // Overview
        String overview = data.getString(COL_MOVIE_OVERVIEW);
        overviewTextView.setText(overview);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
