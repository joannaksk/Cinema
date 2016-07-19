package com.example.joanna.cinema;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.joanna.cinema.data.MovieContract;
import com.squareup.picasso.Picasso;

/**
 * Created by joanna on 11/07/16.
 */
public class MovieAdapter extends CursorRecyclerViewAdapter<MovieAdapter.ViewHolder> {
    private Context mContext;

    public MovieAdapter(Context context, Cursor cursor){
        super(context, cursor);
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.grid_item_movie, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
        // Read poster path from cursor
//        holder.titleView.setText(cursor.getString(cursor.getColumnIndex(MovieContract.MovieColumns.COLUMN_TITLE)));
        String base_path = "http://image.tmdb.org/t/p/w500";
        String poster_path = cursor.getString(cursor.getColumnIndex(MovieContract.MovieColumns.COLUMN_POSTER));
        Uri poster_uri = Uri.parse(base_path+poster_path);

        Picasso.with(mContext).cancelRequest(holder.posterView);
        if (poster_uri != null) {
        Picasso
                .with(mContext)
                .load(poster_uri)
                .into(holder.posterView);
        } else {
            holder.posterView.setImageResource(R.drawable.ic_game_of_thrones);
        }

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
}
