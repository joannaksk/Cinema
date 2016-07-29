package com.example.joanna.cinema;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
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
import com.example.joanna.cinema.sync.CinemaSyncAdapter;

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
        //Now Syncing
        CinemaSyncAdapter.syncImmediately(getActivity());
        movieAdapter.notifyDataSetChanged();
        swipeView.setRefreshing(false);
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
}
