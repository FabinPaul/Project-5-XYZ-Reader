package com.fabinpaul.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fabinpaul.xyzreader.R;
import com.fabinpaul.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleDetailFragment.class.getSimpleName();
    private static final int DEFAULT_MUTED_COLOR = 0xFF333333;

    public static final String ARG_ITEM_ID = "com.fabinpaul.xyzreader.ui.item_id";

    private Cursor mDetailCursor;
    private long mItemId;

    //This was made global as target as weak reference and will be garbage collected onStop.
    @SuppressWarnings("FieldCanBeLocal")
    private Target mArticleImageTarget;
    private Unbinder mUnBinder;

    @BindView(R.id.detail_photo)
    ImageView mPhotoView;
    @BindView(R.id.meta_bar)
    View mMetaBarView;
    @BindView(R.id.detail_byline)
    TextView mBylineView;
    @BindView(R.id.detail_body)
    TextView mBodyView;
    @BindView(R.id.detail_frag_root)
    CoordinatorLayout mCoordinatorLayoutView;
    @BindView(R.id.share_fab)
    FloatingActionButton mShareFABView;
    @BindView(R.id.detail_toolbar)
    Toolbar mToolbarView;
    @BindView(R.id.detail_app_bar)
    AppBarLayout mAppBarLayoutView;
    @BindView(R.id.detail_collapsing_toolbar)
    CollapsingToolbarLayout mCollapsingToolbarLayoutView;

    @Nullable
    @BindView(R.id.detail_card)
    CardView mCardView;
    @Nullable
    @BindView(R.id.article_title)
    TextView mTitleView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mUnBinder = ButterKnife.bind(this, view);

        bindViews();
        return view;
    }

    private void updateMetaBar(Bitmap bitmap) {
        Palette palette = Palette.generate(bitmap, 12);
        int mutedColor = palette.getDarkMutedColor(DEFAULT_MUTED_COLOR);
        if (mCardView == null) {
            int[] gradientColors = {mutedColor, mutedColor, getActivity().getResources().getColor(android.R.color.transparent)};
            GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, gradientColors);
            gradientDrawable.setGradientCenter(0.4f, 0.4f);
            mMetaBarView.setBackground(gradientDrawable);
        } else {
            mMetaBarView.setBackgroundColor(mutedColor);
            mCollapsingToolbarLayoutView.setContentScrimColor(mutedColor);
            mCollapsingToolbarLayoutView.setStatusBarScrimColor(mutedColor);
        }
    }

    private void bindViews() {
        if (mCoordinatorLayoutView == null)
            return;

        mBylineView.setMovementMethod(new LinkMovementMethod());

        if (mDetailCursor != null) {
            mCoordinatorLayoutView.setAlpha(0);
            mCoordinatorLayoutView.setVisibility(View.VISIBLE);
            mCoordinatorLayoutView.animate().alpha(1);
            final String title = mDetailCursor.getString(ArticleLoader.Query.TITLE);

            if (mCardView == null) {
                mToolbarView.setTitle(title);
            } else {
                mToolbarView.setTitle("");
            }
            mToolbarView.setNavigationIcon(R.drawable.ic_arrow_back);
            mToolbarView.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((AppCompatActivity) getActivity()).onSupportNavigateUp();
                }
            });

            ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbarView);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);


            if (mTitleView != null)
                mTitleView.setText(title);

            mBylineView.setText(Html.fromHtml(
                    mDetailCursor.getString(ArticleLoader.Query.PUBLISHED_DATE) + " by <font color='#ffffff'>"
                            + mDetailCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));

            final String articleBody = mDetailCursor.getString(ArticleLoader.Query.BODY);
            mBodyView.setText(articleBody);
            mBodyView.invalidate();

            mShareFABView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                            .setType("text/plain")
                            .setSubject(title)
                            .setText(articleBody)
                            .getIntent(), getString(R.string.action_share)));
                }
            });

            if (mCardView == null)
                mAppBarLayoutView.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                    @Override
                    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                        mBylineView.setAlpha(0.6f + (verticalOffset / (float) appBarLayout.getTotalScrollRange()));
                    }
                });

            mArticleImageTarget = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    if (bitmap != null && mPhotoView != null) {
                        mPhotoView.setImageBitmap(bitmap);
                        updateMetaBar(bitmap);
                    }
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            };
            Picasso.with(getActivity()).load(mDetailCursor.getString(ArticleLoader.Query.PHOTO_URL)).into(mArticleImageTarget);

            mBodyView.requestLayout();
        } else {
            mCoordinatorLayoutView.setVisibility(View.GONE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mDetailCursor = cursor;
        if (mDetailCursor != null && !mDetailCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mDetailCursor.close();
            mDetailCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mDetailCursor = null;
        bindViews();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnBinder.unbind();
    }

}
