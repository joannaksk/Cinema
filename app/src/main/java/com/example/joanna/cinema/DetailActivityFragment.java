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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joanna.cinema.data.MovieContract;
import com.example.joanna.cinema.data.MovieProvider;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Vector;

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
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    static final String DETAIL_URI = "URI";

    private static final int MOVIE_DETAIL_LOADER = 0;
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

    private VideoAdapter videoAdapter;
    private RecyclerView videoView;
    private ReviewsAdapter reviewsAdapter;
    private RecyclerView reviewsView;

    private OnFragmentInteractionListener mListener;
    private Cursor detailsCursor;
    private Button favoritesbutton;

    public DetailActivityFragment() {
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(MOVIE_DETAIL_LOADER, null, this);

        Bundle arguments = getArguments();
        if (arguments != null) {
            dUri = arguments.getParcelable(DetailActivityFragment.DETAIL_URI);
        }

        // Fetch the Extra data.
        String movie_id = dUri.getLastPathSegment();
        fetchExtras(movie_id);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        movieTitleTextView = (TextView) rootView.findViewById(R.id.textview_movie_title);
        posterView = (ImageView) rootView.findViewById(R.id.posterView);
        yearTextView = (TextView) rootView.findViewById(R.id.textView_year);
        durationTextView = (TextView) rootView.findViewById(R.id.textView_duration);
        voteAverageTextView = (TextView) rootView.findViewById(R.id.textView_vote_average);
        overviewTextView = (TextView) rootView.findViewById(R.id.textView_overview);


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
                    ((OnFragmentInteractionListener) getActivity())
                            .onItemSelected(movie_key);
                }
            }
        });
        videoView.setAdapter(videoAdapter);

        // Initialise the adapter for the reviews the same way.
        reviewsAdapter =  new ReviewsAdapter(null);
        reviewsView.setAdapter(reviewsAdapter);

        // Get the Favorites Button and set a Listener.
        favoritesbutton = (Button) rootView.findViewById(R.id.favoritesbutton);
        favoritesbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFavorite();
            }
        });

        return rootView;
    }

    private void addFavorite() {
        AddFavoritesTask addfavoritestask = new AddFavoritesTask(getActivity());
        addfavoritestask.execute();
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

        // Hide favoritesbutton if this is alreadya fave.
        if (data.getInt(COL_MOVIE_FAVORITE) != 0) {
            favoritesbutton.setVisibility(View.GONE);
        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public void fetchExtras(String movie_id){
        FetchTrailersTask trailersTask = new FetchTrailersTask();
        trailersTask.execute(movie_id);
    }

    public interface OnFragmentInteractionListener {
        void onItemSelected(String movie_key);
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

    public  class FetchTrailersTask extends AsyncTask<String, Void, List<Object>[]> {
        final String LOG_TAG = FetchTrailersTask.class.getSimpleName();
        private List[] movie_data = new List[2];

        public FetchTrailersTask() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(List[] movie_data) {
            super.onPostExecute(movie_data);
            // Update the respective Adapters.
            updateVideoData(movie_data[0]);
            updateReviewsData(movie_data[1]);
        }

        @Override
        protected List[] doInBackground(String... params) {

            // Don't do a thing if there is no id.
            if (params!=null & params[0]!=null) {
                try {
                    // Try to fetch the trailers.
                    TmdbApi tmdbApi = new TmdbApi(BuildConfig.MOVIE_DB_API_KEY);
                    TmdbMovies movies = tmdbApi.getMovies();

                    // Get the current movie using the id passed in.
                    int movie_id = Integer.parseInt(params[0]);
                    MovieDb movie = movies.getMovie(movie_id, "en", credits, videos, releases, images, similar, reviews);

                    List<Video> videos = movie.getVideos();
                    movie_data[0] = videos;
                    List<Reviews> reviews = movie.getReviews();
                    movie_data[1] = reviews;

                    return movie_data;

                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                    e.printStackTrace();
                }
            }
            // This will only happen if there was an error getting or parsing the movies.
            return null;
        }
    }

    public class AddFavoritesTask extends AsyncTask<Void, Void, Void> {
        private String LOG_TAG = AddFavoritesTask.class.getSimpleName();
        private Context mContext;

        public AddFavoritesTask(Context context){
            this.mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Insert the new movie information into the database
                Vector<ContentValues> cVVector = new Vector<ContentValues>(1);

                ContentValues favoriteValues = new ContentValues();
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_MOVIE_ID, detailsCursor.getString(COL_MOVIE_ID));
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_TITLE, detailsCursor.getString(COL_MOVIE_TITLE));
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_RELEASE_DATE, detailsCursor.getString(COL_MOVIE_RELEASE_DATE));
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_DURATION, detailsCursor.getString(COL_MOVIE_DURATION));
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_POSTER, detailsCursor.getString(COL_MOVIE_POSTER));
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_POPULARITY, detailsCursor.getString(COL_MOVIE_POPULARITY));
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_VOTE_AVERAGE, detailsCursor.getString(COL_MOVIE_VOTE_AVERAGE));
                favoriteValues.put(MovieContract.FavoriteColumns.COLUMN_OVERVIEW, detailsCursor.getString(COL_MOVIE_OVERVIEW));

                cVVector.add(favoriteValues);
                // Proceed to insert new data.
                int rowsInserted = mContext.getContentResolver().bulkInsert(
                        MovieProvider.Favorites.LIST_CONTENT_URI,
                        cVVector.toArray(new ContentValues[cVVector.size()])
                );
                Log.d(LOG_TAG, "AddFavoritesTask Complete. " + rowsInserted + " Inserted");
                return null;
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            favoritesbutton.setVisibility(View.GONE);
            Toast favoriteAdded = Toast.makeText(mContext, "Favorite Added", Toast.LENGTH_SHORT);
            favoriteAdded.show();
        }
    }
}
