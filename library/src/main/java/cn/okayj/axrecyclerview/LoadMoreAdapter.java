package cn.okayj.axrecyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

public abstract class LoadMoreAdapter<VH extends RecyclerView.ViewHolder> extends LoadMoreWrapperAdapter {

    public LoadMoreAdapter() {
        wrap(new Adapter());
    }

    public abstract VH onCreateContentViewHolder(ViewGroup parent, int viewType);

    public abstract void onBindContentViewHolder(VH holder, int position);

    public abstract int getContentItemCount();

    public int getContentItemViewType(int position){
        return 0;
    }


    @Override
    public final RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public final void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
    }

    @Override
    public final int getItemCount() {
        return super.getItemCount();
    }

    @Override
    public final int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    private class Adapter extends RecyclerView.Adapter<VH> {
        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return onCreateContentViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            onBindContentViewHolder(holder, position);
        }

        @Override
        public int getItemCount() {
            return getContentItemCount();
        }

        @Override
        public int getItemViewType(int position) {
            return getContentItemViewType(position);
        }
    }
}
