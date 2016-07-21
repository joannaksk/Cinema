package com.example.joanna.cinema;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    static final String LOG_TAG = MoviesFragment.class.getSimpleName();

    private static final int MOVIE_LOADER = 0;
    private static final String[] MOVIE_COLUMNS = {
            MovieContract.MovieColumns._ID,
            MovieContract.MovieColumns.COLUMN_MOVIE_ID,
            MovieContract.MovieColumns.COLUMN_POSTER,
            MovieContract.MovieColumns.COLUMN_TITLE,
//            MovieContract.MovieColumns.COLUMN_POPULARITY,
//            MovieContract.MovieColumns.COLUMN_VOTE_AVERAGE,
    };
    static final int COL_MOVIE_DBID = 0;
    static final int COL_MOVIE_ID = 1;
    static final int COL_MOVIE_POSTER = 2;
//    static final int COL_MOVIE_TITLE = 3;
//    static final int COL_MOVIE_POPULARITY = 4;
//    static final int COL_MOVIE_VOTE_AVERAGE = 5;

    private MovieAdapter movieAdapter;
    private SwipeRefreshLayout swipeView;


    public MoviesFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        swipeView = (SwipeRefreshLayout) rootView.findViewById(R.id.refreshMovies);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateMovies();
            }
        });

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.gridview_movies);
        recyclerView.setHasFixedSize(true);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);


        movieAdapter = new MovieAdapter(getActivity(), null, new MovieAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Long movie_id) {
                if (movie_id != null) {
                    ((Callback) getActivity())
                            .onItemSelected(MovieProvider.Movies.withId(movie_id));                }
            }
        });
        recyclerView.setAdapter(movieAdapter);


        return rootView;
    }

    private void updateMovies() {
        FetchMovieTask movieTask = new FetchMovieTask(getActivity());
        movieTask.execute();
    }

//    @Override
//    public void onStart() {
//        super.onStart();
//        updateMovies();
//    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(MOVIE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String defaultSort = getActivity().getString(R.string.pref_sort_order_default);
        String sort = sharedPreferences.getString(getActivity().getString(R.string.pref_sort_order_key),
                defaultSort);
        Uri movieUri = MovieProvider.Movies.LIST_CONTENT_URI;

        return new CursorLoader(
                getActivity(),
                movieUri,
                MOVIE_COLUMNS,
                null,
                null,
                sort);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(LOG_TAG, "Cursor size : " + data.getCount());
        movieAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        movieAdapter.changeCursor(null);
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri movieUri);
    }


    public  class FetchMovieTask extends AsyncTask<Void, Void, Void> {
        final String LOG_TAG = FetchMovieTask.class.getSimpleName();
        private final Context mContext;

        public FetchMovieTask(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            movieAdapter.notifyDataSetChanged();
            swipeView.setRefreshing(false);

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

                    movieValues.put(MovieContract.MovieColumns.COLUMN_MOVIE_ID, movie.getId());
                    movieValues.put(MovieContract.MovieColumns.COLUMN_TITLE, movie.getTitle());
                    movieValues.put(MovieContract.MovieColumns.COLUMN_RELEASE_DATE, movie.getReleaseDate());
                    movieValues.put(MovieContract.MovieColumns.COLUMN_DURATION, movie.getRuntime());
                    movieValues.put(MovieContract.MovieColumns.COLUMN_POSTER, movie.getPosterPath());
                    movieValues.put(MovieContract.MovieColumns.COLUMN_POPULARITY, movie.getPopularity());
                    movieValues.put(MovieContract.MovieColumns.COLUMN_VOTE_AVERAGE, movie.getVoteAverage());
                    movieValues.put(MovieContract.MovieColumns.COLUMN_OVERVIEW, movie.getOverview());

                    cVVector.add(movieValues);
                    Log.d(LOG_TAG, "FetchMovieTask Adding ID: " + movie.getId() + " : " + movie.getTitle() + " To Be Inserted");
                }

                // add to database
                if ( cVVector.size() > 0 ) {
                    int rowsInserted = mContext.getContentResolver().bulkInsert(
                            MovieProvider.Movies.LIST_CONTENT_URI,
                            cVVector.toArray(new ContentValues[cVVector.size()])
                    );
                    Log.d(LOG_TAG, "FetchMovieTask Complete. " + rowsInserted + " Inserted");
                }
                return null;
            }
            catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            // This will only happen if there was an error getting or parsing the movies.
            return null;
        }
    }
}
