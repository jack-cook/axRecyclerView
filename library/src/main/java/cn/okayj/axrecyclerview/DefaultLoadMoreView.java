package cn.okayj.axrecyclerview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DefaultLoadMoreView extends LinearLayout implements ILoadMoreView {
    private View progressBar;
    private TextView hintView;

    public DefaultLoadMoreView(Context context) {
        super(context);
    }

    public DefaultLoadMoreView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DefaultLoadMoreView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onLoadStateChange(LoadMoreState state) {
        switch (state){
            case IDLE:
                progressBar.setVisibility(GONE);
                hintView.setText("点击加载更多");
                hintView.setVisibility(VISIBLE);
                break;
            case FAILED:
                progressBar.setVisibility(GONE);
                hintView.setVisibility(VISIBLE);
                hintView.setText("加载失败，点击重试");
                break;
            case NO_MORE:
                progressBar.setVisibility(GONE);
                hintView.setVisibility(VISIBLE);
                hintView.setText("没有更多了");
                break;
            case LOADING:
                progressBar.setVisibility(VISIBLE);
                hintView.setVisibility(GONE);
                hintView.setText("正在加载...");
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        progressBar = findViewById(R.id.progress);
        hintView = (TextView) findViewById(R.id.hint);
    }
}
