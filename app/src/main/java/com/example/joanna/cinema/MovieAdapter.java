package com.example.joanna.cinema;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

/**
 * Created by joanna on 11/07/16.
 */
public class MovieAdapter extends CursorRecyclerViewAdapter<MovieAdapter.ViewHolder> {
    private Context mContext;
    private Cursor mCursor;
    private final OnItemClickListener listener;

    public MovieAdapter(Context context, Cursor cursor, OnItemClickListener listener){
        super(context, cursor);
        this.mContext = context;
        this.mCursor = cursor;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.grid_item_movie, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final Cursor cursor) {

        String base_path = BuildConfig.MOVIE_POSTER_BASE_PATH;
        String poster_path = cursor.getString(MoviesFragment.COL_MOVIE_POSTER);
        Uri poster_uri = Uri.parse(base_path+poster_path);

        Picasso.with(mContext).cancelRequest(holder.posterView);
        if (poster_uri != null) {
        Picasso
                .with(mContext)
                .load(poster_uri)
                .into(holder.posterView);
        } else {
            //Todo Add placeholder image
            holder.posterView.setImageResource(R.drawable.placeholder);
        }


        final Long movie_id = cursor.getLong(MoviesFragment.COL_MOVIE_ID);

        holder.posterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(movie_id);
            }
        });

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView posterView;
//        public TextView titleView;
        public ViewHolder(View view) {
            super(view);
            posterView = (ImageView) view.findViewById(R.id.grid_item_movie_poster);
//            titleView = (TextView) view.findViewById(R.id.grid_item_movie_title);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Long movie_id);
    }
}
