package com.example.joanna.cinema;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.example.joanna.cinema.sync.CinemaSyncService;

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
    public static final String DATA_SET_CHANGED = "dataSetChanged";

    private GridLayoutManager layoutManager;
    private MovieAdapter movieAdapter;
    private SwipeRefreshLayout swipeView;
    private RecyclerView recyclerView;
    private SharedPreferences sharedPreferences;

    //Broadcast receiver that listens to Sync Adapter broadcasts.
    private BroadcastReceiver syncFinishedReceiver = new BroadcastReceiver() {
        public boolean dataSetChanged;

        @Override
        public void onReceive(Context context, Intent intent) {
            dataSetChanged = intent.getBooleanExtra(DATA_SET_CHANGED, false);
            finishUpdate(dataSetChanged);
        }
    };
    private MainActivity mainActivity;


    public MoviesFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        getActivity().registerReceiver(syncFinishedReceiver, new IntentFilter(CinemaSyncService.SYNC_FINISHED));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mainActivity = (MainActivity) getActivity();

        // Get the SwipeRefreshLayout in the rootView and set it to update the movies upon pull.
        swipeView = (SwipeRefreshLayout) rootView.findViewById(R.id.refreshMovies);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateMovies();
            }
        });

        // Set up the recycler view.
        recyclerView = (RecyclerView) rootView.findViewById(R.id.gridview_movies);
        recyclerView.setHasFixedSize(true);
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        recyclerView.getItemAnimator().setChangeDuration(0);

        // Decide which how to configure the layout.
        layoutManager = new GridLayoutManager(getContext(), getResources().getInteger(R.integer.movies_fragment_columns));

        // Set the layout of the recycler view to grid.
        recyclerView.setLayoutManager(layoutManager);

        // Create a new movie adapter and add it to the recycler view.
        movieAdapter = new MovieAdapter(getActivity(), null, new MovieAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Long movie_id) {
                if (movie_id != null) {
                    ((Callback) getActivity())
                            .onItemSelected(getDetailsUri(movie_id));
                }
            }
        });
        recyclerView.setAdapter(movieAdapter);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(MOVIE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(syncFinishedReceiver);
        super.onDestroy();
    }

    /**
     * Function that creates and executes a FetchMovieTask.
     */
    private void updateMovies() {
        //Now Syncing
        CinemaSyncAdapter.syncImmediately(getActivity());
    }

    /**
     * Function that is called after the sync adapter is done.
     * @param dataSetChanged
     */
    public void finishUpdate(boolean dataSetChanged){
        if (dataSetChanged) {
            movieAdapter.notifyDataSetChanged();
        }
        swipeView.setRefreshing(false);
    }

    /**
     * Function that determines which table we should query.
     * @param movie_id
     * @return
     */
    private Uri getDetailsUri(Long movie_id) {
        String defaultSort = getActivity().getString(R.string.pref_sort_order_default);
        String favoritesSort = getActivity().getString(R.string.pref_sort_order_favorites);
        String sort = sharedPreferences.getString(getActivity().getString(R.string.pref_sort_order_key),
                defaultSort);
        Uri detailsUri;
        if (sort.equals(favoritesSort)) {
            detailsUri = MovieProvider.Favorites.withId(movie_id);
        } else {
            detailsUri = MovieProvider.Movies.withId(movie_id);
        }
        return detailsUri;
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

    /**
     * LOADER OVERRIDES
     *
     * @param id
     * @param args
     * @return
     */
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

        // This is just so the first item of the recyclerview iss selected when using a tablet.
        if(mainActivity.mTwoPane) {
            final int pos = 0;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    recyclerView.findViewHolderForAdapterPosition(pos).itemView.performClick();
                }
            }, 1);
        }

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
