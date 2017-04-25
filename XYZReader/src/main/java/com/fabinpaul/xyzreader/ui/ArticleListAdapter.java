package com.fabinpaul.xyzreader.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;

import com.fabinpaul.xyzreader.R;
import com.fabinpaul.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Picasso;

/**
 * Created by Fabin Paul, Eous Solutions Delivery on 4/12/2017 11:07 AM.
 */

public class ArticleListAdapter extends RecyclerView.Adapter<ArticleListAdapter.ViewHolder> {

    private Cursor mCursor;
    private final Context mContext;
    private OnClickListener mOnClickListener;
    private float mAnimationOffset;

    public void setOnClickListener(@NonNull OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    interface OnClickListener {
        public void onClick(ViewHolder viewHolder, int position);
    }

    public ArticleListAdapter(@NonNull Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
        mAnimationOffset = mContext.getResources().getDimensionPixelSize(R.dimen.offset_y);
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_article, parent, false);
        final ViewHolder vh = new ViewHolder(view);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            animateViewsIn(vh);
        }
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnClickListener != null)
                    mOnClickListener.onClick(vh, vh.getAdapterPosition());
            }
        });
        return vh;
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
        holder.subtitleView.setText(Html.fromHtml(
                mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE)
                        + "<br/>" + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)));
        Picasso.with(mContext).load(mCursor.getString(ArticleLoader.Query.THUMB_URL)).into(holder.thumbnailView);
        holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void animateViewsIn(ViewHolder viewHolder) {
        View view = viewHolder.itemView;
        Interpolator interpolator = AnimationUtils.loadInterpolator(mContext, android.R.interpolator.linear_out_slow_in);
        view.setVisibility(View.VISIBLE);
        view.setTranslationY(mAnimationOffset);
        view.setAlpha(0.85f);
        // then animate back to natural position
        view.animate()
                .translationY(0f)
                .alpha(1f)
                .setInterpolator(interpolator)
                .setDuration(1000L)
                .start();
        // increase the offset distance for the next view
        mAnimationOffset *= 1.5f;
    }

    @Override
    public int getItemCount() {
        return (mCursor != null) ? mCursor.getCount() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightImageView) view.findViewById(R.id.article_thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }

    public void swapCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }
}
