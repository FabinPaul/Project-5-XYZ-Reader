package com.fabinpaul.xyzreader.ui;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Fabin Paul, Eous Solutions Delivery on 4/26/2017 11:15 AM.
 */

public class ScrollFABBehavior extends CoordinatorLayout.Behavior<FloatingActionButton> {

    public ScrollFABBehavior(Context context, AttributeSet attrs) {
        super();
    }

    @Override
    public boolean onStartNestedScroll(final CoordinatorLayout coordinatorLayout, final FloatingActionButton child,
                                       final View directTargetChild, final View target, final int nestedScrollAxes) {
        return true;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child, View target, int dx, int dy, int[] consumed) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed);
        if (dy > 0 && (child.getVisibility() == View.VISIBLE)) {
            child.hide();
        } else if (dy < 0 && (child.getVisibility() != View.VISIBLE)) {
            child.show();
        }
    }
}
