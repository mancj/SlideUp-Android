package com.mancj.slideup;

import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * @author pa.gulko zTrap (12.07.2017)
 */
public class OnGlobalLayoutSingleListener implements ViewTreeObserver.OnGlobalLayoutListener {
    private final View mView;
    private final Runnable mRunnable;
    
    OnGlobalLayoutSingleListener(View view, Runnable runnable) {
        mView = view;
        mRunnable = runnable;
    }
    
    @Override
    public final void onGlobalLayout() {
        ViewTreeObserver observer = mView.getViewTreeObserver();
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            observer.removeGlobalOnLayoutListener(this);
        } else {
            observer.removeOnGlobalLayoutListener(this);
        }
        mRunnable.run();
    }
}
