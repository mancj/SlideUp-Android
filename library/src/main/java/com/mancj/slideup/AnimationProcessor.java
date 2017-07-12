package com.mancj.slideup;

import android.animation.Animator;
import android.animation.ValueAnimator;

/**
 * @author pa.gulko zTrap (12.07.2017)
 */
class AnimationProcessor {
    private SlideUpBuilder mBuilder;
    private ValueAnimator mValueAnimator;
    private float mSlideAnimationTo;
    
    AnimationProcessor(SlideUpBuilder builder, ValueAnimator.AnimatorUpdateListener updateListener, Animator.AnimatorListener listener){
        mBuilder = builder;
        createAnimation(updateListener, listener);
    }
    
    void endAnimation() {
        if (mValueAnimator != null && mValueAnimator.getValues() != null && mValueAnimator.isRunning()) {
            mValueAnimator.end();
        }
    }
    
    void paramsChanged(){
        mValueAnimator.setDuration(mBuilder.mAutoSlideDuration);
        mValueAnimator.setInterpolator(mBuilder.mInterpolator);
    }
    
    float getSlideAnimationTo() {
        return mSlideAnimationTo;
    }
    
    boolean isAnimationRunning(){
        return mValueAnimator != null && mValueAnimator.isRunning();
    }
    
    void setValuesAndStart(float from, float to){
        mSlideAnimationTo = to;
        mValueAnimator.setFloatValues(from, to);
        mValueAnimator.start();
    }
    
    private void createAnimation(ValueAnimator.AnimatorUpdateListener updateListener, Animator.AnimatorListener listener){
        mValueAnimator = ValueAnimator.ofFloat();
        mValueAnimator.setDuration(mBuilder.mAutoSlideDuration);
        mValueAnimator.setInterpolator(mBuilder.mInterpolator);
        mValueAnimator.addUpdateListener(updateListener);
        mValueAnimator.addListener(listener);
    }
}
