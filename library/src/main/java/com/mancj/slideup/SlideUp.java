package com.mancj.slideup;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

public class SlideUp implements View.OnTouchListener, ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
    private final static String LOG_TAG = "SlideUp";

    private View view;
    private float touchableTop;
    private int autoSlideDuration = 300;
    private List<SlideListener> slideListeners = new ArrayList<>();

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
                if (hiddenInit){
                    viewHeight = view.getHeight();
                    hideImmediately();
                }
                ViewTreeObserver observer = view.getViewTreeObserver();
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                    observer.removeGlobalOnLayoutListener(this);
                } else {
                    observer.removeOnGlobalLayoutListener(this);
                }
            }
        });
    }

    public boolean isVisible(){
        return view.getVisibility() == View.VISIBLE;
    }

    @Deprecated
    public void setSlideListener(@Nullable SlideListener slideListener) {
        slideListeners.add(slideListener);
    }

    public void addSlideListener(@NonNull SlideListener slideListener){
        slideListeners.add(slideListener);
    }

    public void removeSlideListener(@NonNull SlideListener slideListener){
        slideListeners.remove(slideListener);
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
        if (view.getHeight() > 0){
            view.setTranslationY(viewHeight);
            view.setVisibility(View.GONE);
            notifyVisibilityChanged(View.GONE);
        }else {
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
        if (isAnimationRunning()){
            return false;
        }
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                this.viewHeight = view.getHeight();
                startPositionY = event.getRawY();
                viewStartPositionY = view.getTranslationY();
                if (touchableTop < touchedArea){
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
                if (event.getRawY() > lowerPosition){
                    lowerPosition = event.getRawY();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = view.getTranslationY();
                boolean mustSlideUp = lowerPosition > event.getRawY();
                boolean scrollableAreaConsumed = view.getTranslationY() > view.getHeight() / 5;

                if (scrollableAreaConsumed && !mustSlideUp){
                    slideAnimationTo = view.getHeight();
                }else {
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
        if (!slideListeners.isEmpty()){
            for (int i = 0; i < slideListeners.size(); i++) {
                SlideListener l = slideListeners.get(i);
                if (l != null){
                    l.onSlide(percent);
                }else {
                    Log.e(LOG_TAG, "SlideListener(" + i + ") is null, skip (onSlide) notify for him...");
                }
            }
        }
    }

    private void notifyVisibilityChanged(int visibility){
        if (!slideListeners.isEmpty()){
            for (int i = 0; i < slideListeners.size(); i++) {
                SlideListener l = slideListeners.get(i);
                if (l != null) {
                    l.onVisibilityChanged(visibility);
                }else {
                    Log.e(LOG_TAG, "SlideListener(" + i + ") is null, skip (onSlide) notify for him...");
                }
            }
        }
    }

    @Override
    public void onAnimationStart(Animator animator) {
        view.setVisibility(View.VISIBLE);
        notifyVisibilityChanged(View.VISIBLE);
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        if (slideAnimationTo > 0){
            view.setVisibility(View.GONE);
            notifyVisibilityChanged(View.GONE);
        }
    }

    @Override
    public void onAnimationCancel(Animator animator) {}

    @Override
    public void onAnimationRepeat(Animator animator) {}

    public interface SlideListener {
        void onSlide(float percent);
        void onVisibilityChanged(int visibility);
    }

    public static class SlideListenerAdapter implements SlideListener{
        public void onSlide(float percent){}
        public void onVisibilityChanged(int visibility){}
    }

}
