package com.luck.pictureselector;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * ================================================
 * 作    者：JayGoo
 * 版    本：
 * 创建日期：2017/6/6
 * 描    述: 设置RecycleView item间距
 * ================================================
 */
public class SpaceItemDecoration extends RecyclerView.ItemDecoration {

    private int space;

    public SpaceItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

        outRect.left = space;
        outRect.bottom = space;

    }

}
