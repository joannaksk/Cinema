package com.example.joanna.cinema;

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

    private List<Video> videoList;

    public VideoAdapter(List<Video> videoList) {
        this.videoList = videoList;
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        protected TextView trailer_text;
        protected TextView trailer_key;

        public VideoViewHolder(View v) {
            super(v);
            trailer_text = (TextView)  v.findViewById(R.id.list_item_trailer_textview);
            trailer_key = (TextView)  v.findViewById(R.id.list_item_trailer_key_textview);
        }
    }
    @Override
    public void onBindViewHolder(VideoViewHolder videoViewHolder, int i) {
        Video video = videoList.get(i);

        videoViewHolder.trailer_text.setText(video.getName());
        videoViewHolder.trailer_key.setText(video.getKey());
    }
    //select XML layout for each card
    @Override
    public VideoViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.list_item_video, viewGroup, false);

        return new VideoViewHolder(itemView);
    }
}
