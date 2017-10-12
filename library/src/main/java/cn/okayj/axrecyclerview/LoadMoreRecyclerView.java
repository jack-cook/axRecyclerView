package cn.okayj.axrecyclerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

public class LoadMoreRecyclerView extends RecyclerView {
    private LoadMoreWrapperAdapter mLoadMoreWrapperAdapter;

    public LoadMoreRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public LoadMoreRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public LoadMoreRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mLoadMoreWrapperAdapter = new LoadMoreWrapperAdapter();
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LoadMoreRecyclerView);
            boolean loadMore = a.getBoolean(R.styleable.LoadMoreRecyclerView_loadMore, false);
            int loadMoreLayout = a.getResourceId(R.styleable.LoadMoreRecyclerView_loadMoreView, 0);
            mLoadMoreWrapperAdapter.setLoadMoreEnabled(loadMore);
            if (loadMoreLayout > 0) {
                mLoadMoreWrapperAdapter.setLoadMoreView(loadMoreLayout);
            }
            a.recycle();
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {
        mLoadMoreWrapperAdapter.wrap(adapter);
        if (getAdapter() == null) {
            super.setAdapter(mLoadMoreWrapperAdapter);
        }
    }

    public void setLoadMoreListener(LoadMoreListener loadMoreListener) {
        mLoadMoreWrapperAdapter.setLoadMoreListener(loadMoreListener);
    }

    public LoadMoreListener getLoadMoreListener() {
        return mLoadMoreWrapperAdapter.getLoadMoreListener();
    }

    public void endLoading() {
        mLoadMoreWrapperAdapter.endLoading();
    }

    public void noMore() {
        mLoadMoreWrapperAdapter.noMore();
    }

    public void loadFail() {
        mLoadMoreWrapperAdapter.loadFail();
    }

    public void resetLoadMore() {
        mLoadMoreWrapperAdapter.resetLoadMore();
    }

    public boolean isLoadMoreEnabled() {
        return mLoadMoreWrapperAdapter.isLoadMoreEnabled();
    }

    public void setLoadMoreEnabled(boolean mLoadMoreEnabled) {
        mLoadMoreWrapperAdapter.setLoadMoreEnabled(mLoadMoreEnabled);
    }

    public void setLoadMoreEnabled(boolean loadMoreEnabled, boolean withItemAnimation) {
        mLoadMoreWrapperAdapter.setLoadMoreEnabled(loadMoreEnabled, withItemAnimation);
    }

    public void setLoadMoreView(View loadMoreView) {
        mLoadMoreWrapperAdapter.setLoadMoreView(loadMoreView);
    }

    public void setLoadMoreView(@LayoutRes int layout) {
        mLoadMoreWrapperAdapter.setLoadMoreView(layout);
    }
}
