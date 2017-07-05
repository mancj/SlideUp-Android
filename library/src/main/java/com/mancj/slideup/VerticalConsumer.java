package com.mancj.slideup;

import android.animation.ValueAnimator;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author pa.gulko zTrap (05.07.2017)
 */
public class VerticalConsumer {
    private float mSlideAnimationTo;
    private ValueAnimator mValueAnimator;
    private LoggerNotifier mNotifier;
    private boolean mCanSlide = true;
    private float mMaxSlidePosition;
    private float mTouchableArea;
    
    private float mStartPositionY;
    private float mViewStartPositionY;
    
    private float mViewHeight;
    private View mSliderView;
    
    public VerticalConsumer(@NonNull View sliderView, float touchableArea, LoggerNotifier notifier,
                            ValueAnimator valueAnimator){
        mValueAnimator = valueAnimator;
        mTouchableArea = touchableArea;
        mSliderView = sliderView;
        mNotifier = notifier;
    }
    
    public boolean consumeUpToDown(MotionEvent event){
        float touchedArea = event.getRawY() - mSliderView.getBottom();
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                mViewHeight = mSliderView.getHeight();
                mStartPositionY = event.getRawY();
                mViewStartPositionY = mSliderView.getTranslationY();
                mMaxSlidePosition = mViewHeight;
                if (mTouchableArea < touchedArea){
                    mCanSlide = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawY() - mStartPositionY;
                float moveTo = mViewStartPositionY + difference;
                float percents = moveTo * 100 / -mSliderView.getHeight();
            
                if (moveTo < 0 && mCanSlide){
                    mNotifier.notifyPercentChanged(percents);
                    mSliderView.setTranslationY(moveTo);
                }
                if (event.getRawY() < mMaxSlidePosition) {
                    mMaxSlidePosition = event.getRawY();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = -mSliderView.getTranslationY();
                if (slideAnimationFrom == mViewStartPositionY){
                    return false;
                }
                boolean mustShow = mMaxSlidePosition < event.getRawY();
                boolean scrollableAreaConsumed = mSliderView.getTranslationY() < -mSliderView.getHeight() / 5;
            
                if (scrollableAreaConsumed && !mustShow){
                    mSlideAnimationTo = mSliderView.getHeight() + mSliderView.getTop();
                }else {
                    mSlideAnimationTo = 0;
                }
                mValueAnimator.setFloatValues(slideAnimationFrom, mSlideAnimationTo);
                mValueAnimator.start();
                mCanSlide = true;
                mMaxSlidePosition = 0;
                break;
        }
        return true;
    }
}
