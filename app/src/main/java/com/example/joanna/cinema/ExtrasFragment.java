package com.example.joanna.cinema;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.List;

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbMovies;
import info.movito.themoviedbapi.model.MovieDb;
import info.movito.themoviedbapi.model.Reviews;
import info.movito.themoviedbapi.model.Video;

import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.credits;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.images;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.releases;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.reviews;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.similar;
import static info.movito.themoviedbapi.TmdbMovies.MovieMethod.videos;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ExtrasFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class ExtrasFragment extends Fragment implements LoaderManager.LoaderCallbacks<List[]> {

    public static String first_movie_key;
    private LinearLayout extras_layout;

    private VideoAdapter videoAdapter;
    private RecyclerView videoView;
    private ReviewsAdapter reviewsAdapter;
    private RecyclerView reviewsView;

    private OnFragmentInteractionListener mListener;
    private static String movie_id;

    private final int EXTRAS_LOADER = 1 ;
    private MenuItem item;
    private ShareActionProvider mShareActionProvider;

    public ExtrasFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_extrasfragment, menu);
        // Locate MenuItem with ShareActionProvider
        item = menu.findItem(R.id.action_share);
        item.setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        movie_id = getArguments().getString("movie_id");
        getLoaderManager().initLoader(EXTRAS_LOADER, null, this).forceLoad();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_extras, container, false);

        extras_layout = (LinearLayout) rootView;
        LinearLayoutManager layout = new LinearLayoutManager(getActivity());
        layout.setOrientation(LinearLayoutManager.VERTICAL);
        videoView  = (RecyclerView)rootView.findViewById(R.id.list_videos);
        videoView.setHasFixedSize(true);
        videoView.setLayoutManager(layout);

        LinearLayoutManager layout1 = new LinearLayoutManager(getActivity());
        layout1.setOrientation(LinearLayoutManager.VERTICAL);
        reviewsView  = (RecyclerView)rootView.findViewById(R.id.list_reviews);
        reviewsView.setHasFixedSize(true);
        reviewsView.setLayoutManager(layout1);

        // Initialise the adapter for the trailers with a null List.
        videoAdapter =  new VideoAdapter(getActivity(),  null, new VideoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String movie_key) {
                if (movie_key != null) {
                    mListener = ((OnFragmentInteractionListener) getActivity());
                    mListener.onItemSelected(movie_key);
                }
            }
        });
        videoView.setAdapter(videoAdapter);

        // Initialise the adapter for the reviews the same way.
        reviewsAdapter =  new ReviewsAdapter(null);
        reviewsView.setAdapter(reviewsAdapter);

        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void updateVideoData(List<Video> videos) {
        this.videoAdapter.setVideoList(videos);
        this.videoAdapter.notifyDataSetChanged();
    }

    public void updateReviewsData(List<Reviews> reviews) {
        this.reviewsAdapter.setReviewsList(reviews);
        this.reviewsAdapter.notifyDataSetChanged();
    }

    @Override
    public android.support.v4.content.Loader<List[]> onCreateLoader(int id, Bundle args) {
        return new TrailersLoader(getActivity());
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<List[]> loader, List[] data) {
        updateVideoData(data[0]);
        updateReviewsData(data[1]);
        extras_layout.setVisibility(View.VISIBLE);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        if(mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
            item.setVisible(true);
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<List[]> loader) {
    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");

        Uri movie_uri = Uri.parse(BuildConfig.MOVIE_VIDEOS_BASE_PATH+first_movie_key);
        shareIntent.putExtra(Intent.EXTRA_TEXT, movie_uri.toString());
        return shareIntent;
    }

    public interface OnFragmentInteractionListener {
        void onItemSelected(String movie_key);
    }

    public static class TrailersLoader extends AsyncTaskLoader<List[]> {
        final String LOG_TAG = TrailersLoader.class.getSimpleName();
        private List[] movie_data = new List[2];

        public TrailersLoader(Context context) {
            super(context);
        }

        @Override
        public List[] loadInBackground() {
            // Don't do a thing if there is no id.
            if (movie_id!=null) {
                try {
                    // Try to fetch the trailers.
                    TmdbApi tmdbApi = new TmdbApi(BuildConfig.MOVIE_DB_API_KEY);
                    TmdbMovies movies = tmdbApi.getMovies();

                    // Get the current movie using the id passed in.
                    int id = Integer.parseInt(movie_id);
                    MovieDb movie = movies.getMovie(id, "en", credits, videos, releases, images, similar, reviews);

                    List<Video> videos = movie.getVideos();
                    movie_data[0] = videos;
                    first_movie_key = videos.get(0).getKey();
                    List<Reviews> reviews = movie.getReviews();
                    movie_data[1] = reviews;

                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                    e.printStackTrace();
                }
            }
            return movie_data;
        }
    }
}
