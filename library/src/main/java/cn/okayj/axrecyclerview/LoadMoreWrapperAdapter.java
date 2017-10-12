package cn.okayj.axrecyclerview;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * todo overscroll 触发加载 , load more animation, extend adapter
 * 暂不支持一个Adapter给多个RecyclerView用,因为 loadMoreView 不能有多个 parent，并且loadMoreView的状态不能被多个RecyclerView共享
 */
public class LoadMoreWrapperAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "LoadMoreWrapperAdapter";

    public static final int VIEW_TYPE_LOAD_MORE_FOOTER = (1 << 14) * (-1);
    private static final int LOAD_MORE_CONTAINER = R.layout.load_more_container;


    private RecyclerView.Adapter mDummyAdapter = new DummyAdapter();
    private RecyclerView.Adapter mAdapter = mDummyAdapter;

    private final AdapterLoadMoreStateTracker mAdapterLoadMoreStateTracker = new AdapterLoadMoreStateTracker();
    private final PendingLoadMoreView mDefaultPendingLoadMoreView = new PendingLoadMoreView(R.layout.load_more_view_default);
    private PendingLoadMoreView mPendingLoadMoreView = mDefaultPendingLoadMoreView;
    private LoadMoreDirector mLoadMoreDirector;
    private LoadMoreListener mLoadMoreListener;
    private boolean mLoadMoreEnabled = false;

    private int mAttachedRecyclerViewCount = 0;//保护不被多个RecyclerView同时使用

    private RecyclerView.AdapterDataObserver mAdapterObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            LoadMoreWrapperAdapter.this.notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            LoadMoreWrapperAdapter.this.notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            LoadMoreWrapperAdapter.this.notifyItemRangeChanged(positionStart, itemCount, payload);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            LoadMoreWrapperAdapter.this.notifyItemRangeInserted(positionStart, itemCount);
            afterContentInsert(itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            LoadMoreWrapperAdapter.this.notifyItemRangeRemoved(positionStart, itemCount);
            afterContentRemove(itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            if (itemCount == 1) {
                LoadMoreWrapperAdapter.this.notifyItemMoved(fromPosition, toPosition);
            } else {
                throw new UnsupportedOperationException("目前暂不支持多个item同时move");
            }
        }
    };

    public LoadMoreWrapperAdapter() {

    }

    public void wrap(RecyclerView.Adapter adapter) {
        if (mAdapter == adapter) {
            notifyDataSetChanged();
            return;
        }

        if (mAdapter != mDummyAdapter) {
            mAdapter.unregisterAdapterDataObserver(mAdapterObserver);
        }

        if (adapter == null) {
            mAdapter = mDummyAdapter;
        } else {
            mAdapter = adapter;
            mAdapter.registerAdapterDataObserver(mAdapterObserver);
        }

        notifyDataSetChanged();
    }

    public void setLoadMoreView(View loadMoreView) {
        PendingLoadMoreView pendingLoadMoreView;
        if (loadMoreView == null) {
            pendingLoadMoreView = mDefaultPendingLoadMoreView;
        } else {
            pendingLoadMoreView = new PendingLoadMoreView(loadMoreView);
        }

        setLoadMoreView(pendingLoadMoreView);
    }

    public void setLoadMoreView(@LayoutRes int layout) {
        PendingLoadMoreView pendingLoadMoreView = new PendingLoadMoreView(layout);
        setLoadMoreView(pendingLoadMoreView);
    }

    private void setLoadMoreView(PendingLoadMoreView pendingView) {
        if (mPendingLoadMoreView.equals(pendingView)) {
            return;
        } else {
            mPendingLoadMoreView = pendingView;
        }

        if (isLoadMoreEnabled() && mAdapter.getItemCount() > 0) {
            notifyItemChanged(mAdapter.getItemCount());
        }
    }

    public boolean isLoadMoreEnabled() {
        return mLoadMoreEnabled;
    }

    public void setLoadMoreEnabled(boolean loadMoreEnabled) {
        setLoadMoreEnabled(loadMoreEnabled,false);
    }

    public void setLoadMoreEnabled(boolean loadMoreEnabled, boolean withItemAnimation){
        if (mLoadMoreEnabled != loadMoreEnabled) {
            this.mLoadMoreEnabled = loadMoreEnabled;
            if(withItemAnimation) {
                if (loadMoreEnabled) {
                    notifyItemInserted(mAdapter.getItemCount());
                } else {
                    notifyItemRemoved(mAdapter.getItemCount());
                }
            }else {
                notifyDataSetChanged();
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType >= 0) {
            return mAdapter.onCreateViewHolder(parent, viewType);
        } else {
            ViewGroup loadMoreContainer = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(LOAD_MORE_CONTAINER, parent, false);
            LoadMoreViewHolder loadMoreViewHolder = new LoadMoreViewHolder(loadMoreContainer);
            return loadMoreViewHolder;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position < mAdapter.getItemCount()) {
            mAdapter.onBindViewHolder(holder, position);
        } else {
            if (holder instanceof LoadMoreViewHolder) {
                LoadMoreViewHolder loadMoreViewHolder = (LoadMoreViewHolder) holder;
                loadMoreViewHolder.setPendingLoadMoreView(mPendingLoadMoreView);
            }
        }
    }

    @Override
    public int getItemCount() {
        int count = mAdapter.getItemCount();
        if (count > 0 && isLoadMoreEnabled()) {
            return count + 1;
        } else {
            return count;
        }
    }

    @Override
    public int getItemViewType(int position) {
        int viewType;

        if (position < mAdapter.getItemCount()) {
            viewType = mAdapter.getItemViewType(position);
            if (viewType < 0) {
                throw new IllegalStateException("adapterWrapper 的 viewtype 需要大于等于0");
            }
        } else {
            viewType = VIEW_TYPE_LOAD_MORE_FOOTER;
        }

        return viewType;
    }

    /**
     * 如果{@link #isLoadMoreEnabled()} == true,需要判断是否加载 load more view
     *
     * @param insertSize
     */
    private void afterContentInsert(int insertSize) {
        if (isLoadMoreEnabled()) {
            if (insertSize > 0 && insertSize == mAdapter.getItemCount()) {
                notifyItemInserted(mAdapter.getItemCount());
            } else {
                return;
            }
        }
    }

    /**
     * 如果{@link #isLoadMoreEnabled()} == true,需要判断是否移除 load more view
     *
     * @param removeSize
     */
    private void afterContentRemove(int removeSize) {
        if (isLoadMoreEnabled()) {
            if (removeSize > 0 && mAdapter.getItemCount() == 0) {
                notifyItemRemoved(0);
            } else {
                return;
            }
        }
    }

    public void endLoading() {
        if (mLoadMoreDirector != null) {
            mLoadMoreDirector.endLoading();
        }
    }

    public void noMore() {
        if (mLoadMoreDirector != null) {
            mLoadMoreDirector.noMore();
        }
    }

    public void loadFail() {
        if (mLoadMoreDirector != null) {
            mLoadMoreDirector.loadFail();
        }
    }

    public void resetLoadMore() {
        if (mLoadMoreDirector != null) {
            mLoadMoreDirector.reset();
        }
    }

    public LoadMoreListener getLoadMoreListener() {
        return mLoadMoreListener;
    }

    public void setLoadMoreListener(LoadMoreListener mLoadMoreListener) {
        this.mLoadMoreListener = mLoadMoreListener;
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        if (holder instanceof LoadMoreViewHolder) {
            mLoadMoreDirector.onShow();
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        if (holder instanceof LoadMoreViewHolder) {
            mLoadMoreDirector.onHide();
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mAttachedRecyclerViewCount++;
        if (mAttachedRecyclerViewCount > 1) {
            throw new IllegalStateException(TAG + "不能同时被多个RecyclerView使用");
        }
        recyclerView.addOnScrollListener(mRecyclerViewScrollListener);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mAttachedRecyclerViewCount--;
        recyclerView.removeOnScrollListener(mRecyclerViewScrollListener);
    }

    private final class PendingLoadMoreView {
        private int loadMoreLayout;
        private View loadMoreView;

        public PendingLoadMoreView(@LayoutRes int loadMoreLayout) {
            if (loadMoreLayout <= 0) {
                throw new IllegalArgumentException("loadMoreLayout 需要大于0 （layout）");
            }
            this.loadMoreLayout = loadMoreLayout;
        }

        public PendingLoadMoreView(@NonNull View loadMoreView) {
            this.loadMoreView = loadMoreView;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof PendingLoadMoreView) {
                PendingLoadMoreView outer = (PendingLoadMoreView) obj;

                if (loadMoreView != null) {
                    return loadMoreView == outer.loadMoreView;
                } else {
                    return loadMoreLayout == outer.loadMoreLayout;
                }
            } else {
                return false;
            }
        }

        public View createView(ViewGroup parent) {
            if (loadMoreView != null) {
                return loadMoreView;
            } else {
                return LayoutInflater.from(parent.getContext()).inflate(loadMoreLayout, parent, false);
            }
        }
    }

    private static class DummyAdapter extends RecyclerView.Adapter {
        public DummyAdapter() {
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 0;
        }
    }

    private static class InnerViewHolder extends RecyclerView.ViewHolder {
        public InnerViewHolder(View itemView) {
            super(itemView);
            setIsRecyclable(false);
        }
    }

    private class LoadMoreViewHolder extends InnerViewHolder {
        private ViewGroup loadMoreContainer;
        private PendingLoadMoreView pendingLoadMoreView;

        public LoadMoreViewHolder(ViewGroup itemView) {
            super(itemView);
            loadMoreContainer = itemView;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mLoadMoreDirector.trigger(true);
                }
            });
            setIsRecyclable(true);
        }

        public void setPendingLoadMoreView(PendingLoadMoreView pendingView) {
            if (pendingView.equals(pendingLoadMoreView)) {

            } else {
                pendingLoadMoreView = pendingView;
                View v = pendingView.createView(loadMoreContainer);
                mLoadMoreDirector = new LoadMoreDirector(v);
                loadMoreContainer.removeAllViews();
                loadMoreContainer.addView(v);
            }
        }
    }

    private class LoadMoreDirector {
        private View loadMoreView;

        private boolean mLoadMoreViewVisible = false;

        private LoadMoreDirector(View loadMoreView) {
            this.loadMoreView = loadMoreView;
            onInternalLoadStateChange();
        }

        public void reset() {
            if (mAdapterLoadMoreStateTracker.getLoadMoreState() != LoadMoreState.IDLE) {
                mAdapterLoadMoreStateTracker.setLoadMoreState(LoadMoreState.IDLE);
                onInternalLoadStateChange();
            }
        }

        public void trigger(boolean force) {
            if (mAdapterLoadMoreStateTracker.getLoadMoreState() == LoadMoreState.LOADING) {
                return;
            }

            if (mLoadMoreListener == null) {
                return;
            }

            if (force) {
                if (mAdapterLoadMoreStateTracker.getLoadMoreState() != LoadMoreState.NO_MORE) {
                    mAdapterLoadMoreStateTracker.setLoadMoreState(LoadMoreState.LOADING);
                    mLoadMoreListener.onLoadMore();
                    onInternalLoadStateChange();
                }
            } else {
                if (mAdapterLoadMoreStateTracker.getLoadMoreState() == LoadMoreState.IDLE) {
                    mAdapterLoadMoreStateTracker.setLoadMoreState(LoadMoreState.LOADING);
                    onInternalLoadStateChange();
                    mLoadMoreListener.onLoadMore();
                }
            }
        }

        private void onShow() {
            mLoadMoreViewVisible = true;
        }

        private void onHide() {
            mLoadMoreViewVisible = false;

            if (mAdapterLoadMoreStateTracker.getLoadMoreState() != LoadMoreState.LOADING && mAdapterLoadMoreStateTracker.getLoadMoreState() != LoadMoreState.NO_MORE) {
                if (mAdapterLoadMoreStateTracker.getLoadMoreState() != LoadMoreState.IDLE) {
                    mAdapterLoadMoreStateTracker.setLoadMoreState(LoadMoreState.IDLE);
                    onInternalLoadStateChange();
                }
            }
        }

        private void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if(newState == RecyclerView.SCROLL_STATE_DRAGGING){
                mAdapterLoadMoreStateTracker.setAutoTriggerEnabled(true);
            }
        }

        private void onRecyclerViewScrolled(RecyclerView recyclerView, int dx, int dy) {
            if(mAdapterLoadMoreStateTracker.isLoadMoreViewVisibleOnLastScrolled() == false && mLoadMoreViewVisible == true){
                mAdapterLoadMoreStateTracker.setLoadMoreViewVisibleOnLastScrolled(mLoadMoreViewVisible);
                if(mAdapterLoadMoreStateTracker.isAutoTriggerEnabled()) {
                    mAdapterLoadMoreStateTracker.setAutoTriggerEnabled(false);
                    trigger(false);
                }
            } else {
                mAdapterLoadMoreStateTracker.setLoadMoreViewVisibleOnLastScrolled(mLoadMoreViewVisible);
            }
        }

        public void endLoading() {
            if (mAdapterLoadMoreStateTracker.getLoadMoreState() == LoadMoreState.LOADING) {
                mAdapterLoadMoreStateTracker.setLoadMoreState(LoadMoreState.IDLE);
                onInternalLoadStateChange();
            }

        }

        public void loadFail() {
            if (mAdapterLoadMoreStateTracker.getLoadMoreState() != LoadMoreState.FAILED) {
                mAdapterLoadMoreStateTracker.setLoadMoreState(LoadMoreState.FAILED);
                onInternalLoadStateChange();
            }

        }

        public void noMore() {
            if (mAdapterLoadMoreStateTracker.getLoadMoreState() != LoadMoreState.NO_MORE) {
                mAdapterLoadMoreStateTracker.setLoadMoreState(LoadMoreState.NO_MORE);
                onInternalLoadStateChange();
            }
        }

        private void onInternalLoadStateChange() {
            if (loadMoreView instanceof ILoadMoreView) {
                ((ILoadMoreView) loadMoreView).onLoadStateChange(mAdapterLoadMoreStateTracker.getLoadMoreState());
            }
        }
    }

    private class AdapterLoadMoreStateTracker {
        private LoadMoreState loadMoreState = LoadMoreState.IDLE;

        private boolean mLoadMoreViewVisibleOnLastScrolled = false;//上一次 load more view 随 recycler view 滚动时的可见性

        private boolean autoTriggerEnabled = true;

        public boolean isLoadMoreViewVisibleOnLastScrolled() {
            return mLoadMoreViewVisibleOnLastScrolled;
        }

        public void setLoadMoreViewVisibleOnLastScrolled(boolean mLoadMoreViewVisibleOnLastScrolled) {
            this.mLoadMoreViewVisibleOnLastScrolled = mLoadMoreViewVisibleOnLastScrolled;
        }

        public LoadMoreState getLoadMoreState() {
            return loadMoreState;
        }

        public void setLoadMoreState(LoadMoreState loadMoreState) {
            this.loadMoreState = loadMoreState;
        }

        public boolean isAutoTriggerEnabled() {
            return autoTriggerEnabled;
        }

        public void setAutoTriggerEnabled(boolean autoTriggerEnabled) {
            this.autoTriggerEnabled = autoTriggerEnabled;
        }
    }

    private RecyclerView.OnScrollListener mRecyclerViewScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if(mLoadMoreDirector != null){
                mLoadMoreDirector.onScrollStateChanged(recyclerView, newState);
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (mLoadMoreDirector != null) {
                mLoadMoreDirector.onRecyclerViewScrolled(recyclerView, dx, dy);
            }
        }
    };

}
