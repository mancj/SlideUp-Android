package com.mancj.slideup;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;

public class SlideUp implements View.OnTouchListener, ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
    private View view;
    private float touchableTop;
    private int autoSlideDuration = 300;
    private SlideListener slideListener;

    private ValueAnimator valueAnimator;
    private float slideAnimationTo;

    private float startPositionY;
    private float viewStartPositionY;
    private boolean canSlide = true;
    private float density;
    private float lowerPosition;
    private float viewHeight;

    private boolean hiddenInit;

    public SlideUp(final View view) {
        this.view = view;
        this.density = view.getResources().getDisplayMetrics().density;
        this.touchableTop = 300 * density;
        view.setOnTouchListener(this);
        view.setPivotY(0);
        createAnimation();
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (hiddenInit)
                {
                    viewHeight = view.getHeight();
                    hideImmediately();
                }
                view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }

    public boolean isVisible(){
        return view.getVisibility() == View.VISIBLE;
    }

    public void setSlideListener(SlideListener slideListener) {
        this.slideListener = slideListener;
    }

    public void setAutoSlideDuration(int autoSlideDuration) {
        this.autoSlideDuration = autoSlideDuration;
    }

    public float getAutoSlideDuration(){
        return this.autoSlideDuration;
    }

    public void setTouchableTop(float touchableTop) {
        this.touchableTop = touchableTop * density;
    }

    public float getTouchableTop() {
        return this.touchableTop/density;
    }

    public boolean isAnimationRunning(){
        return valueAnimator != null && valueAnimator.isRunning();
    }

    public void animateIn(){
        this.slideAnimationTo = 0;
        valueAnimator.setFloatValues(viewHeight, slideAnimationTo);
        valueAnimator.start();
    }

    public void animateOut(){
        this.slideAnimationTo = view.getHeight();
        valueAnimator.setFloatValues(view.getTranslationY(), slideAnimationTo);
        valueAnimator.start();
    }

    public void hideImmediately() {
        if (view.getHeight() > 0)
        {
            view.setTranslationY(viewHeight);
            view.setVisibility(View.GONE);
            notifyVisibilityChanged(View.GONE);
        }
        else {
            hiddenInit = true;
        }
    }

    private void createAnimation(){
        valueAnimator = ValueAnimator.ofFloat();
        valueAnimator.setDuration(autoSlideDuration);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(this);
        valueAnimator.addListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float touchedArea = event.getRawY() - view.getTop();
        if (isAnimationRunning())
        {
            return false;
        }
        switch (event.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
                this.viewHeight = view.getHeight();
                startPositionY = event.getRawY();
                viewStartPositionY = view.getTranslationY();
                if (touchableTop < touchedArea)
                {
                    canSlide = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawY() - startPositionY;
                float moveTo = viewStartPositionY + difference;
                float percents = moveTo * 100 / view.getHeight();

                if (moveTo > 0 && canSlide){
                    notifyPercentChanged(percents);
                    view.setTranslationY(moveTo);
                }
                if (event.getRawY() > lowerPosition)
                {
                    lowerPosition = event.getRawY();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = view.getTranslationY();
                boolean mustSlideUp = lowerPosition > event.getRawY();
                boolean scrollableAreaConsumed = view.getTranslationY() > view.getHeight() / 5;

                if (scrollableAreaConsumed && !mustSlideUp)
                {
                    slideAnimationTo = view.getHeight();
                }
                else {
                    slideAnimationTo = 0;
                }
                valueAnimator.setFloatValues(slideAnimationFrom, slideAnimationTo);
                valueAnimator.start();
                canSlide = true;
                lowerPosition = 0;
                break;
        }
        return true;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float val = (float) animation.getAnimatedValue();
        view.setTranslationY(val);
        float percents = (view.getY() - view.getTop()) * 100 / viewHeight;
        notifyPercentChanged(percents);
    }

    private void notifyPercentChanged(float percent){
        if (slideListener != null)
        {
            slideListener.onSlide(percent);
        }
    }

    private void notifyVisibilityChanged(int visibility){
        if (slideListener != null)
        {
            slideListener.onVisibilityChanged(visibility);
        }
    }

    @Override
    public void onAnimationStart(Animator animator) {
        view.setVisibility(View.VISIBLE);
        notifyVisibilityChanged(View.VISIBLE);
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        if (slideAnimationTo > 0)
        {
            view.setVisibility(View.GONE);
            notifyVisibilityChanged(View.GONE);
        }
    }

    @Override
    public void onAnimationCancel(Animator animator) {

    }

    @Override
    public void onAnimationRepeat(Animator animator) {

    }

    public interface SlideListener {
        void onSlide(float percent);
        void onVisibilityChanged(int visibility);
    }

}
