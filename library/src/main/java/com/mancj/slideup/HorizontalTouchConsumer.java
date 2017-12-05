package com.mancj.slideup;

import android.view.MotionEvent;

/**
 * @author pa.gulko zTrap (12.07.2017)
 */
class HorizontalTouchConsumer extends TouchConsumer {
    
    HorizontalTouchConsumer(SlideUpBuilder builder, LoggerNotifier notifier, AnimationProcessor animationProcessor) {
        super(builder, notifier, animationProcessor);
    }
    
    boolean consumeEndToStart(MotionEvent event){
        float touchedArea = event.getX();
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                mViewWidth = mBuilder.mSliderView.getWidth();
                mStartPositionX = event.getRawX();
                mViewStartPositionX = mBuilder.mSliderView.getTranslationX();
                mCanSlide = getStart() + mBuilder.mTouchableArea >= touchedArea;
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawX() - mStartPositionX;
                float moveTo = mViewStartPositionX + difference;
                float percents = moveTo * 100 / mBuilder.mSliderView.getWidth();
                
                if (moveTo > 0 && mCanSlide){
                    mNotifier.notifyPercentChanged(percents);
                    mBuilder.mSliderView.setTranslationX(moveTo);
                }
                if (event.getRawX() > mMaxSlidePosition) {
                    mMaxSlidePosition = event.getRawX();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = mBuilder.mSliderView.getTranslationX();
                if (slideAnimationFrom == mViewStartPositionX){
                    return !Internal.isUpEventInView(mBuilder.mSliderView, event);
                }
                boolean mustShow = mMaxSlidePosition > event.getRawX();
                boolean scrollableAreaConsumed = mBuilder.mSliderView.getTranslationX() > mBuilder.mSliderView.getWidth() / 5;
                
                if (scrollableAreaConsumed && !mustShow){
                    mAnimationProcessor.setValuesAndStart(slideAnimationFrom, mBuilder.mSliderView.getWidth());
                }else {
                    mAnimationProcessor.setValuesAndStart(slideAnimationFrom, 0);
                }
                mCanSlide = true;
                mMaxSlidePosition = 0;
                break;
        }
        return true;
    }
    
    boolean consumeStartToEnd(MotionEvent event){
        float touchedArea = event.getX();
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                mMaxSlidePosition = mViewWidth;
                mViewWidth = mBuilder.mSliderView.getWidth();
                mStartPositionX = event.getRawX();
                mViewStartPositionX = mBuilder.mSliderView.getTranslationX();
                mCanSlide = getEnd() - mBuilder.mTouchableArea >= touchedArea;
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawX() - mStartPositionX;
                float moveTo = mViewStartPositionX + difference;
                float percents = moveTo * 100 / -mBuilder.mSliderView.getWidth();
                
                if (moveTo < 0 && mCanSlide){
                    mNotifier.notifyPercentChanged(percents);
                    mBuilder.mSliderView.setTranslationX(moveTo);
                }
                if (event.getRawX() < mMaxSlidePosition) {
                    mMaxSlidePosition = event.getRawX();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = -mBuilder.mSliderView.getTranslationX();
                if (slideAnimationFrom == mViewStartPositionX){
                    return !Internal.isUpEventInView(mBuilder.mSliderView, event);
                }
                boolean mustShow = mMaxSlidePosition < event.getRawX();
                boolean scrollableAreaConsumed = mBuilder.mSliderView.getTranslationX() < -mBuilder.mSliderView.getHeight() / 5;
                
                if (scrollableAreaConsumed && !mustShow){
                    mAnimationProcessor.setValuesAndStart(slideAnimationFrom, mBuilder.mSliderView.getWidth());
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
