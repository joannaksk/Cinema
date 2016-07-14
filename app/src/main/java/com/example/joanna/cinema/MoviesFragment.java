package com.example.joanna.cinema;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.GridView;

import com.example.joanna.cinema.data.MovieContract;
import com.example.joanna.cinema.data.MovieProvider;

import java.util.List;
import java.util.Vector;

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbMovies;
import info.movito.themoviedbapi.model.MovieDb;

/**
 * A placeholder fragment containing a simple view.
 */
public class MoviesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final int MOVIE_LOADER = 0;
    private static final String[] MOVIE_COLUMNS = {
            MovieContract.MovieColumns._ID,
            MovieContract.MovieColumns.COLUMN_POSTER,
    };

    MovieAdapter movieAdapter;


    public MoviesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        GridView gridView = (GridView) rootView.findViewById(R.id.gridview_movies);
        movieAdapter = new MovieAdapter(getActivity(), null, 0);
        gridView.setAdapter( movieAdapter);

//        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            public void onItemClick(AdapterView<?> parent, View v,
//                                    int position, long id) {
//                Intent intent = new Intent(getActivity(), DetailActivity.class);
//                startActivity(intent);
//            }
//        });
        return rootView;
    }

    private void updateMovies() {
        FetchMovieTask movieTask = new FetchMovieTask(getActivity());
        movieTask.execute();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateMovies();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(MOVIE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        Uri movieUri = MovieProvider.Movies.LIST_CONTENT_URI;
        String defaultSort = MovieContract.MovieColumns.COLUMN_POPULARITY + " DESC";

        return new CursorLoader(
                getActivity(),
                movieUri,
                MOVIE_COLUMNS,
                null,
                null,
                defaultSort);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        movieAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        movieAdapter.swapCursor(null);
    }

    public static class FetchMovieTask extends AsyncTask<Void, Void, Void> {
        static final String LOG_TAG = FetchMovieTask.class.getSimpleName();
        private final Context mContext;

        public FetchMovieTask(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                // Try to fetch the movies.
                TmdbApi tmdbApi = new TmdbApi(BuildConfig.MOVIE_DB_API_KEY);
                TmdbMovies movies = tmdbApi.getMovies();
                List<MovieDb> movies_list = movies.getPopularMovies("en", 0).getResults();

                // Insert the new movie information into the database
                Vector<ContentValues> cVVector = new Vector<ContentValues>(movies_list.size());

                for (MovieDb movie : movies_list) {
                    ContentValues movieValues = new ContentValues();

                    movieValues.put(MovieContract.MovieColumns.COLUMN_MOVIE_ID, movie.getImdbID());
                    movieValues.put(MovieContract.MovieColumns.COLUMN_TITLE, movie.getTitle());
                    movieValues.put(MovieContract.MovieColumns.COLUMN_RELEASE_DATE, movie.getReleaseDate());
                    movieValues.put(MovieContract.MovieColumns.COLUMN_DURATION, movie.getRuntime());
                    movieValues.put(MovieContract.MovieColumns.COLUMN_POSTER, movie.getPosterPath());
                    movieValues.put(MovieContract.MovieColumns.COLUMN_POPULARITY, movie.getPopularity());
                    movieValues.put(MovieContract.MovieColumns.COLUMN_VOTE_AVERAGE, movie.getVoteAverage());
                    movieValues.put(MovieContract.MovieColumns.COLUMN_OVERVIEW, movie.getOverview());

                    cVVector.add(movieValues);
                }

                // add to database
                if ( cVVector.size() > 0 ) {
                    // Student: call bulkInsert to add the weatherEntries to the database here
                    int rowsInserted = mContext.getContentResolver().bulkInsert(
                            MovieProvider.Movies.LIST_CONTENT_URI,
                            cVVector.toArray(new ContentValues[cVVector.size()])
                    );
                }
                Log.d(LOG_TAG, "FetchMovieTask Complete. " + cVVector.size() + " Inserted");
                return null;
            }
            catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }
    }
}
