package com.example.joanna.cinema;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import info.movito.themoviedbapi.model.Reviews;

/**
 * Created by joanna on 25/07/16.
 */
public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewsViewHolder> {

    private List<Reviews> reviewsList;

    public ReviewsAdapter(List<Reviews> reviewsList) {
        this.reviewsList = reviewsList;
    }

    @Override
    public int getItemCount() {
        if (reviewsList != null) {
            return reviewsList.size();
        }
        return 0;
    }

    public void setReviewsList(List<Reviews> reviews) {
        this.reviewsList = reviews;
    }

    public static class ReviewsViewHolder extends RecyclerView.ViewHolder {
        protected TextView review_author;
        protected TextView review_content;

        public ReviewsViewHolder(View v) {
            super(v);
            review_author = (TextView)  v.findViewById(R.id.list_item_review_author_textView);
            review_content = (TextView)  v.findViewById(R.id.list_item_review_content_textView);
        }
    }
    @Override
    public void onBindViewHolder(ReviewsViewHolder videoViewHolder, int i) {
        Reviews review = reviewsList.get(i);

        videoViewHolder.review_author.setText(review.getAuthor());
        videoViewHolder.review_content.setText(review.getContent());
    }
    //select XML layout for each card
    @Override
    public ReviewsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.list_item_review, viewGroup, false);

        return new ReviewsViewHolder(itemView);
    }
}
