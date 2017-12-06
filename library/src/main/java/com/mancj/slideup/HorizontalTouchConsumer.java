package com.mancj.slideup;

import android.view.MotionEvent;
import android.view.View;

/**
 * @author pa.gulko zTrap (12.07.2017)
 */
class HorizontalTouchConsumer extends TouchConsumer {
    private boolean mGoingToStart = false;
    private boolean mGoingToEnd = false;
    
    HorizontalTouchConsumer(SlideUpBuilder builder, LoggerNotifier notifier, AnimationProcessor animationProcessor) {
        super(builder, notifier, animationProcessor);
    }
    
    boolean consumeEndToStart(View touchedView, MotionEvent event){
        float touchedArea = event.getX();
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                mViewWidth = mBuilder.mSliderView.getWidth();
                mStartPositionX = event.getRawX();
                mViewStartPositionX = mBuilder.mSliderView.getTranslationX();
                mCanSlide = touchFromAlsoSlide(touchedView, event);
                mCanSlide |= getStart() + mBuilder.mTouchableArea >= touchedArea;
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawX() - mStartPositionX;
                float moveTo = mViewStartPositionX + difference;
                float percents = moveTo * 100 / mBuilder.mSliderView.getWidth();
                calculateDirection(event);
                
                if (moveTo > 0 && mCanSlide){
                    mNotifier.notifyPercentChanged(percents);
                    mBuilder.mSliderView.setTranslationX(moveTo);
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = mBuilder.mSliderView.getTranslationX();
                if (slideAnimationFrom == mViewStartPositionX){
                    return !Internal.isUpEventInView(mBuilder.mSliderView, event);
                }
                boolean scrollableAreaConsumed = mBuilder.mSliderView.getTranslationX() > mBuilder.mSliderView.getWidth() / 5;
                
                if (scrollableAreaConsumed && mGoingToEnd){
                    mAnimationProcessor.setValuesAndStart(slideAnimationFrom, mBuilder.mSliderView.getWidth());
                }else {
                    mAnimationProcessor.setValuesAndStart(slideAnimationFrom, 0);
                }
                mCanSlide = true;
                break;
        }
        mPrevPositionY = event.getRawY();
        mPrevPositionX = event.getRawX();
        return true;
    }
    
    boolean consumeStartToEnd(View touchedView, MotionEvent event){
        float touchedArea = event.getX();
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                mViewWidth = mBuilder.mSliderView.getWidth();
                mStartPositionX = event.getRawX();
                mViewStartPositionX = mBuilder.mSliderView.getTranslationX();
                mCanSlide = touchFromAlsoSlide(touchedView, event);
                mCanSlide |= getEnd() - mBuilder.mTouchableArea >= touchedArea;
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawX() - mStartPositionX;
                float moveTo = mViewStartPositionX + difference;
                float percents = moveTo * 100 / -mBuilder.mSliderView.getWidth();
                calculateDirection(event);
                
                if (moveTo < 0 && mCanSlide){
                    mNotifier.notifyPercentChanged(percents);
                    mBuilder.mSliderView.setTranslationX(moveTo);
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = -mBuilder.mSliderView.getTranslationX();
                if (slideAnimationFrom == mViewStartPositionX){
                    return !Internal.isUpEventInView(mBuilder.mSliderView, event);
                }
                boolean scrollableAreaConsumed = mBuilder.mSliderView.getTranslationX() < -mBuilder.mSliderView.getHeight() / 5;
                
                if (scrollableAreaConsumed && mGoingToStart){
                    mAnimationProcessor.setValuesAndStart(slideAnimationFrom, mBuilder.mSliderView.getWidth());
                }else {
                    mAnimationProcessor.setValuesAndStart(slideAnimationFrom, 0);
                }
                mCanSlide = true;
                break;
        }
        mPrevPositionY = event.getRawY();
        mPrevPositionX = event.getRawX();
        return true;
    }

    private void calculateDirection(MotionEvent event) {
        mGoingToStart = mPrevPositionX - event.getRawX() > 0;
        mGoingToEnd = mPrevPositionX - event.getRawX() < 0;
    }
}
