package zlc.season.practicalrecyclerview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.Observable;
import java.util.Observer;


/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/9/21
 * Time: 13:56
 * FIXME
 */
public class SuperRecyclerView extends FrameLayout {

    private DataSetObserver mObserver;

    private FrameLayout mMain;
    private FrameLayout mLoading;
    private FrameLayout mError;
    private FrameLayout mEmpty;
    private LinearLayout mContent;


    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private View mLoadMoreView;
    private View mNoMoreView;
    private View mLoadMoreFailedView;

    private OnRefreshListener mRefreshListener;
    private OnLoadMoreListener mLoadMoreListener;

    private ConfigureView mConfigureView;

    private boolean onLoading = false;
    private boolean noMore = false;
    private boolean loadMoreFailed = false;


    public SuperRecyclerView(Context context) {
        this(context, null);
    }


    public SuperRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SuperRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
        obtainStyledAttributes(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SuperRecyclerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
        obtainStyledAttributes(context, attrs);
    }

    public void setConfigureView(ConfigureView configureView) {
        mConfigureView = configureView;
    }

    public void setRefreshListener(OnRefreshListener refreshListener) {
        mRefreshListener = refreshListener;
    }

    public void setLoadMoreListener(OnLoadMoreListener loadMoreListener) {
        mLoadMoreListener = loadMoreListener;
    }

    private void init(Context context) {
        mObserver = new DataSetObserver();

        mMain = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.recycler_layout, this, true);
        mLoading = (FrameLayout) mMain.findViewById(R.id.loading);
        mError = (FrameLayout) mMain.findViewById(R.id.error);
        mEmpty = (FrameLayout) mMain.findViewById(R.id.empty);
        mContent = (LinearLayout) mMain.findViewById(R.id.content);

        mSwipeRefreshLayout = (SwipeRefreshLayout) mMain.findViewById(R.id.swipe_refresh);
        mRecyclerView = (RecyclerView) mMain.findViewById(R.id.recycler);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (canNotRefresh()) {
                    closeRefreshing();
                    return;
                }
                mRefreshListener.onRefresh();
            }
        });

        mRecyclerView.addOnScrollListener(new OnScrollListener());
    }

    private void closeRefreshing() {
        if (mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    private boolean canNotLoadMore() {
        return mSwipeRefreshLayout.isRefreshing() || mLoadMoreListener == null || onLoading || loadMoreFailed || noMore;
    }

    private boolean canNotRefresh() {
        return mRefreshListener == null || onLoading;
    }

    private void obtainStyledAttributes(Context context, AttributeSet attrs) {
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.SuperRecyclerView);
        int loadingResId = attributes.getResourceId(R.styleable.SuperRecyclerView_loading_layout, R.layout
                .default_loading_layout);
        int emptyResId = attributes.getResourceId(R.styleable.SuperRecyclerView_empty_layout, R.layout
                .default_empty_layout);
        int errorResId = attributes.getResourceId(R.styleable.SuperRecyclerView_error_layout, R.layout
                .default_error_layout);

        int loadMoreResId = attributes.getResourceId(R.styleable.SuperRecyclerView_load_more_layout, R.layout
                .default_load_more);
        int noMoreResId = attributes.getResourceId(R.styleable.SuperRecyclerView_no_more_layout, R.layout
                .default_no_more_layout);
        int loadMoreErrorResId = attributes.getResourceId(R.styleable.SuperRecyclerView_load_more_error_layout, R
                .layout.default_load_more_error_layout);

        View loadingView = LayoutInflater.from(context).inflate(loadingResId, mLoading, true);
        View emptyView = LayoutInflater.from(context).inflate(emptyResId, mEmpty, true);
        View errorView = LayoutInflater.from(context).inflate(errorResId, mError, true);

        mLoadMoreView = LayoutInflater.from(context).inflate(loadMoreResId, mMain, false);
        mNoMoreView = LayoutInflater.from(context).inflate(noMoreResId, mMain, false);
        mLoadMoreFailedView = LayoutInflater.from(context).inflate(loadMoreErrorResId, mMain, false);

        if (mConfigureView != null) {
            mConfigureView.configureEmptyView(emptyView);
            mConfigureView.configureErrorView(errorView);
            mConfigureView.configureLoadingView(loadingView);
            mConfigureView.configureLoadMoreView(mLoadMoreView);
            mConfigureView.configureNoMoreView(mNoMoreView);
            mConfigureView.configureLoadMoreErrorView(mLoadMoreFailedView);
        }

        attributes.recycle();
    }

    public void setLayoutManager(RecyclerView.LayoutManager layoutManager) {
        mRecyclerView.setLayoutManager(layoutManager);
    }

    public void setAdapterWithLoading(RecyclerView.Adapter adapter) {
        if (adapter instanceof AbstractAdapter) {
            AbstractAdapter abstractAdapter = (AbstractAdapter) adapter;
            subscribeWithAdapter(abstractAdapter);
        }
        showLoadingView();
        mRecyclerView.setAdapter(adapter);
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        mRecyclerView.setAdapter(adapter);
    }

    private void subscribeWithAdapter(AbstractAdapter adapter) {
        adapter.registerObserver(mObserver);
    }

    public void setEmptyView(View emptyView) {
        mEmpty.removeAllViews();
        mEmpty.addView(emptyView);
    }

    public void setErrorView(View errorView) {
        mError.removeAllViews();
        mError.addView(errorView);
    }

    void showLoadingView() {
        mError.setVisibility(GONE);
        mContent.setVisibility(GONE);
        mEmpty.setVisibility(GONE);
        mLoading.setVisibility(VISIBLE);
    }

    void showContentView() {
        onLoading = false;
        loadMoreFailed = false;
        noMore = false;
        mLoading.setVisibility(GONE);
        mError.setVisibility(GONE);
        mEmpty.setVisibility(GONE);
        mContent.setVisibility(VISIBLE);
    }

    void showEmptyView() {
        mLoading.setVisibility(GONE);
        mContent.setVisibility(GONE);
        mError.setVisibility(GONE);
        mEmpty.setVisibility(VISIBLE);
    }

    void showErrorView() {
        mLoading.setVisibility(GONE);
        mContent.setVisibility(GONE);
        mEmpty.setVisibility(GONE);
        mError.setVisibility(VISIBLE);
    }


    void showLoadMoreView() {
        if (!(mRecyclerView.getAdapter() instanceof AbstractAdapter)) return;
        onLoading = true;
        AbstractAdapter adapter = (AbstractAdapter) mRecyclerView.getAdapter();
        adapter.show(mLoadMoreView);
    }

    void showNoMoreView() {
        if (!(mRecyclerView.getAdapter() instanceof AbstractAdapter)) return;
        noMore = true;
        AbstractAdapter adapter = (AbstractAdapter) mRecyclerView.getAdapter();
        adapter.show(mNoMoreView);
    }

    void showLoadMoreErrorView() {
        if (!(mRecyclerView.getAdapter() instanceof AbstractAdapter)) return;
        loadMoreFailed = true;
        AbstractAdapter adapter = (AbstractAdapter) mRecyclerView.getAdapter();
        adapter.show(mLoadMoreFailedView);
    }

    void resumeLoadMore() {
        loadMoreFailed = false;
        showLoadMoreView();
    }

    void autoLoadMore() {
        if (canNotLoadMore()) return;
        showLoadMoreView();
        mLoadMoreListener.onLoadMore();
    }

    public interface OnRefreshListener {
        void onRefresh();
    }


    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    class OnScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (canNotLoadMore()) return;
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            int visibleItemCount = layoutManager.getChildCount();
            int totalItemCount = layoutManager.getItemCount();
            int lastVisibleItemPosition = getLastVisibleItemPosition(layoutManager);
            if (visibleItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 2 && totalItemCount >=
                    visibleItemCount) {
                showLoadMoreView();
                mLoadMoreListener.onLoadMore();
            }
        }

        private int getLastVisibleItemPosition(RecyclerView.LayoutManager layoutManager) {
            int lastVisibleItemPosition;
            if (layoutManager instanceof GridLayoutManager) {
                lastVisibleItemPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                int[] into = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
                ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(into);
                lastVisibleItemPosition = findMax(into);
            } else {
                lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
            }
            return lastVisibleItemPosition;
        }

        private int findMax(int[] lastPositions) {
            int max = lastPositions[0];
            for (int value : lastPositions) {
                if (value > max) {
                    max = value;
                }
            }
            return max;
        }
    }

    public class DataSetObserver implements Observer {

        @Override
        public void update(Observable o, Object arg) {
            closeRefreshing();
            closeLoadingMore();
            EventType type = (EventType) arg;
            type.doSomething(SuperRecyclerView.this);
        }

        private void closeLoadingMore() {
            if (onLoading) {
                onLoading = false;
            }
        }
    }
}