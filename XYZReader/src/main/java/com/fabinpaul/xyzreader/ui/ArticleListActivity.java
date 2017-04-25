package com.fabinpaul.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;

import com.fabinpaul.xyzreader.R;
import com.fabinpaul.xyzreader.data.ArticleLoader;
import com.fabinpaul.xyzreader.data.ItemsContract;
import com.fabinpaul.xyzreader.sync.UpdaterService;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.article_list_recycler_view)
    RecyclerView mRecyclerView;

    private Unbinder mUnBinder;
    private ArticleListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mUnBinder = ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.article_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mSwipeRefreshLayout.setOnRefreshListener(this);

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh(false);
        }
    }

    @Override
    public void onRefresh() {
        mAdapter.swapCursor(null);
        refresh(true);
    }

    public void refresh(boolean syncImmediately) {
        Intent intent = new Intent(this, UpdaterService.class);
        intent.putExtra(UpdaterService.EXTRA_SYNC_IMMEDIATELY, syncImmediately);
        startService(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter = new ArticleListAdapter(this, cursor);
        mAdapter.setOnClickListener(new ArticleListAdapter.OnClickListener() {
            @Override
            public void onClick(ArticleListAdapter.ViewHolder viewHolder, int position) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        ItemsContract.Items.buildItemUri(mAdapter.getItemId(position))));
            }
        });
        if (mIsRefreshing)
            mAdapter.swapCursor(null);
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager gridLayoutManager =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(gridLayoutManager);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mRecyclerView != null)
            mRecyclerView.setAdapter(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUnBinder.unbind();
    }
}
