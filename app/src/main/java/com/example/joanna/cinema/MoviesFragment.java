package com.example.joanna.cinema;

import android.content.ContentUris;
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
import android.support.v7.widget.SimpleItemAnimator;
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

import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.credits;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.images;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.releases;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.reviews;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.similar;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.videos;

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
    };
    static final int COL_MOVIE_DBID = 0;
    static final int COL_MOVIE_ID = 1;
    static final int COL_MOVIE_POSTER = 2;

    private MovieAdapter movieAdapter;
    private SwipeRefreshLayout swipeView;
    private SharedPreferences sharedPreferences;


    public MoviesFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get the SwipeRefreshLayout in the rootView and set it to update the movies upon pull.
        swipeView = (SwipeRefreshLayout) rootView.findViewById(R.id.refreshMovies);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateMovies();
            }
        });

        // Set up the recycler view.
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.gridview_movies);
        recyclerView.setHasFixedSize(true);
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        recyclerView.getItemAnimator().setChangeDuration(0);

        // Set the layout of the recycler view to grid.
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);

        // Create a new movie adapter and add it to the recycler view.
        movieAdapter = new MovieAdapter(getActivity(), null, new MovieAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Long movie_id) {
                if (movie_id != null) {
                    ((Callback) getActivity())
                            .onItemSelected(MovieProvider.Movies.withId(movie_id));
                }
            }
        });
        recyclerView.setAdapter(movieAdapter);

        return rootView;
    }

    /**
     * Function that creates and executes a FetchMovieTask.
     */
    private void updateMovies() {
        FetchMovieTask movieTask = new FetchMovieTask(getActivity());
        movieTask.execute();
    }

    /**
     * Callback function that runs a new FetchMovieTask and restarts the Movie Loader.
     */
    void onSortChanged( ) {
        // If the sort has changed to favorites, destroy the loader and re initialise.
        // otherwise.
        String defaultSort = getActivity().getString(R.string.pref_sort_order_default);
        String favoritesSort = getActivity().getString(R.string.pref_sort_order_favorites);
        String sort = sharedPreferences.getString(getActivity().getString(R.string.pref_sort_order_key),
                defaultSort);
        if (!sort.equals(favoritesSort)) {
            updateMovies();
        }
        getLoaderManager().restartLoader(MOVIE_LOADER, null, this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(MOVIE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String defaultSort = getActivity().getString(R.string.pref_sort_order_default);
        String favoritesSort = getActivity().getString(R.string.pref_sort_order_favorites);
        String sort = sharedPreferences.getString(getActivity().getString(R.string.pref_sort_order_key),
                defaultSort);
        Uri movieUri;
        if (sort.equals(favoritesSort)) {
            movieUri = MovieProvider.Favorites.LIST_CONTENT_URI;
        } else {
            movieUri = MovieProvider.Movies.LIST_CONTENT_URI;
        }


        return new CursorLoader(
                getActivity(),
                movieUri,
                MOVIE_COLUMNS,
                null,
                null,
                null);
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
                List<MovieDb> movies_list;

                // Decide whether to fetch the most popular or the top rated movies.
                String defaultSort = getActivity().getString(R.string.pref_sort_order_default);
                String sort = sharedPreferences.getString(getActivity().getString(R.string.pref_sort_order_key),
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
