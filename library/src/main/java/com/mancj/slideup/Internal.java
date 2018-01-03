package com.mancj.slideup;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author pa.gulko zTrap (12.07.2017)
 */
class Internal {
    private static Rect sRect = new Rect();
    
    static void checkNonNull(Object obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
    }
    
    static boolean isUpEventInView(View view, MotionEvent event){
        view.getHitRect(sRect);
        return sRect.contains((int) event.getRawX(), (int) event.getRawY());
    }
}
