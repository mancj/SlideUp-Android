package com.mancj.slideup;

import android.view.MotionEvent;

/**
 * @author pa.gulko zTrap (05.07.2017)
 */
class VerticalTouchConsumer extends TouchConsumer {
    
    VerticalTouchConsumer(SlideUpBuilder builder, LoggerNotifier notifier, AnimationProcessor animationProcessor) {
        super(builder, notifier, animationProcessor);
    }
    
    boolean consumeBottomToTop(MotionEvent event){
        float touchedArea = event.getY();
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                mViewHeight = mBuilder.mSliderView.getHeight();
                mStartPositionY = event.getRawY();
                mViewStartPositionY = mBuilder.mSliderView.getTranslationY();
                mCanSlide = mBuilder.mTouchableArea >= touchedArea;
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawY() - mStartPositionY;
                float moveTo = mViewStartPositionY + difference;
                float percents = moveTo * 100 / mBuilder.mSliderView.getHeight();
                
                if (moveTo > 0 && mCanSlide){
                    mNotifier.notifyPercentChanged(percents);
                    mBuilder.mSliderView.setTranslationY(moveTo);
                }
                if (event.getRawY() > mMaxSlidePosition) {
                    mMaxSlidePosition = event.getRawY();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = mBuilder.mSliderView.getTranslationY();
                if (slideAnimationFrom == mViewStartPositionY){
                    return !Internal.isUpEventInView(mBuilder.mSliderView, event);
                }
                boolean mustShow = mMaxSlidePosition > event.getRawY();
                boolean scrollableAreaConsumed = mBuilder.mSliderView.getTranslationY() > mBuilder.mSliderView.getHeight() / 5;
                
                if (scrollableAreaConsumed && !mustShow){
                    mAnimationProcessor.setValuesAndStart(slideAnimationFrom, mBuilder.mSliderView.getHeight());
                } else {
                    mAnimationProcessor.setValuesAndStart(slideAnimationFrom, 0);
                }
                mCanSlide = true;
                mMaxSlidePosition = 0;
                break;
        }
        return true;
    }
    
    boolean consumeTopToBottom(MotionEvent event){
        float touchedArea = event.getY();
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                mViewHeight = mBuilder.mSliderView.getHeight();
                mStartPositionY = event.getRawY();
                mViewStartPositionY = mBuilder.mSliderView.getTranslationY();
                mMaxSlidePosition = mViewHeight;
                mCanSlide = getBottom() - mBuilder.mTouchableArea <= touchedArea;
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawY() - mStartPositionY;
                float moveTo = mViewStartPositionY + difference;
                float percents = moveTo * 100 / -mBuilder.mSliderView.getHeight();
            
                if (moveTo < 0 && mCanSlide){
                    mNotifier.notifyPercentChanged(percents);
                    mBuilder.mSliderView.setTranslationY(moveTo);
                }
                if (event.getRawY() < mMaxSlidePosition) {
                    mMaxSlidePosition = event.getRawY();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = -mBuilder.mSliderView.getTranslationY();
                if (slideAnimationFrom == mViewStartPositionY){
                    return !Internal.isUpEventInView(mBuilder.mSliderView, event);
                }
                boolean mustShow = mMaxSlidePosition < event.getRawY();
                boolean scrollableAreaConsumed = mBuilder.mSliderView.getTranslationY() < -mBuilder.mSliderView.getHeight() / 5;
            
                if (scrollableAreaConsumed && !mustShow){
                    mAnimationProcessor.setValuesAndStart(slideAnimationFrom, mBuilder.mSliderView.getHeight() + mBuilder.mSliderView.getTop());
                }else {
                    mAnimationProcessor.setValuesAndStart(slideAnimationFrom, 0);
                }
                mCanSlide = true;
                mMaxSlidePosition = 0;
                break;
        }
        return true;
    }
}
