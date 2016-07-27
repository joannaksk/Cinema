package com.example.joanna.cinema;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import info.movito.themoviedbapi.model.Video;

/**
 * Created by joanna on 25/07/16.
 */
public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final Context mContext;
    private List<Video> videoList;
    private final OnItemClickListener listener;

    public VideoAdapter(Context context, List<Video> videoList, OnItemClickListener listener) {
        this.mContext = context;
        this.videoList = videoList;
        this.listener = listener;
    }

    @Override
    public int getItemCount() {
        if (videoList != null) {
            return videoList.size();
        }
        return 0;
    }

    public void setVideoList(List<Video> videos){
        this.videoList = videos;
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        protected TextView trailer_text;

        public VideoViewHolder(View v) {
            super(v);
            trailer_text = (TextView)  v.findViewById(R.id.list_item_trailer_textview);
        }
    }
    @Override
    public void onBindViewHolder(VideoViewHolder videoViewHolder, int i) {
        Video video = videoList.get(i);
        final String movie_key = video.getKey();

        videoViewHolder.trailer_text.setText(video.getName());
        videoViewHolder.trailer_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(movie_key);
            }
        });
    }
    //select XML layout for each card
    @Override
    public VideoViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.list_item_video, viewGroup, false);

        return new VideoViewHolder(itemView);
    }

    public interface OnItemClickListener {
        void onItemClick(String movie_key);
    }
}
