package com.mancj.slideup;

import android.view.MotionEvent;
import android.view.View;

/**
 * @author pa.gulko zTrap (12.07.2017)
 */
class Internal {
    
    static void checkNonNull(Object obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
    }
    
    static boolean isUpEventInView(View view, MotionEvent event){
        int top = view.getTop();
        int bottom = view.getBottom();
        int right = view.getRight();
        int left = view.getLeft();
        if (event.getRawY() > top){
            if (event.getRawY() < bottom){
                if (event.getRawX() > left){
                    if (event.getRawX() < right){
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
