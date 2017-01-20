package com.mancj.slideup;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class SlideUp<T extends View> implements View.OnTouchListener, ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
    private final static String LOG_TAG = "SlideUp";

    private T sliderView;
    private float touchableTop;
    private int autoSlideDuration = 300;
    private List<Listener> listeners = new ArrayList<>();

    private ValueAnimator valueAnimator;
    private float slideAnimationTo;

    private float startPositionY;
    private float viewStartPositionY;
    private boolean canSlide = true;
    private float density;
    private float lowerPosition;
    private float viewHeight;

    private boolean DownToUp = true;

    private boolean hiddenInit;

    public SlideUp(@NonNull final T sliderView) {
        this.sliderView = sliderView;
        this.density = this.sliderView.getResources().getDisplayMetrics().density;
        this.touchableTop = 300 * density;
        sliderView.setOnTouchListener(this);
        sliderView.setPivotY(0);
        createAnimation();
        sliderView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (hiddenInit){
                    viewHeight = sliderView.getHeight();
                    hideImmediately();
                }
                ViewTreeObserver observer = sliderView.getViewTreeObserver();
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                    observer.removeGlobalOnLayoutListener(this);
                } else {
                    observer.removeOnGlobalLayoutListener(this);
                }
            }
        });
    }

    public void setDownToUp(){
        DownToUp = true;
    }

    public void setUpToDown(){
        DownToUp = false;
    }

    public boolean isVisible(){
        return sliderView.getVisibility() == VISIBLE;
    }

    @Deprecated
    public void setSlideListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public void addSlideListener(@NonNull Listener listener){
        listeners.add(listener);
    }

    public void removeSlideListener(@NonNull Listener listener){
        listeners.remove(listener);
    }

    public T getSliderView() {
        return sliderView;
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
        return this.touchableTop / density;
    }

    public boolean isAnimationRunning(){
        return valueAnimator != null && valueAnimator.isRunning();
    }

    public void show(){
        this.slideAnimationTo = 0;
        valueAnimator.setFloatValues(viewHeight, slideAnimationTo);
        valueAnimator.start();
    }

    public void hide(){
        this.slideAnimationTo = sliderView.getHeight();
        valueAnimator.setFloatValues(sliderView.getTranslationY(), slideAnimationTo);
        valueAnimator.start();
    }

    public void hideImmediately() {
        if (sliderView.getHeight() > 0){
            sliderView.setTranslationY(viewHeight);
            sliderView.setVisibility(GONE);
            notifyVisibilityChanged(GONE);
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
        float touchedArea = event.getRawY() - sliderView.getTop();
        if (isAnimationRunning()){
            return false;
        }
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                this.viewHeight = sliderView.getHeight();
                startPositionY = event.getRawY();
                viewStartPositionY = sliderView.getTranslationY();
                if (touchableTop < touchedArea){
                    canSlide = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawY() - startPositionY;
                float moveTo = viewStartPositionY + difference;
                float percents = moveTo * 100 / sliderView.getHeight();

                if (moveTo > 0 && canSlide){
                    notifyPercentChanged(percents);
                    sliderView.setTranslationY(moveTo);
                }
                if (event.getRawY() > lowerPosition){
                    lowerPosition = event.getRawY();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = sliderView.getTranslationY();
                boolean mustSlideUp = lowerPosition > event.getRawY();
                boolean scrollableAreaConsumed = sliderView.getTranslationY() > sliderView.getHeight() / 5;

                if (scrollableAreaConsumed && !mustSlideUp){
                    slideAnimationTo = sliderView.getHeight();
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
        sliderView.setTranslationY(val);
        float percents = (sliderView.getY() - sliderView.getTop()) * 100 / viewHeight;
        notifyPercentChanged(percents);
    }

    private void notifyPercentChanged(float percent){
        if (!listeners.isEmpty()){
            for (int i = 0; i < listeners.size(); i++) {
                Listener l = listeners.get(i);
                if (l != null){
                    l.onSlide(percent);
                }else {
                    Log.e(LOG_TAG, "Listener(" + i + ") is null, skip (onSlide) notify for him...");
                }
            }
        }
    }

    private void notifyVisibilityChanged(int visibility){
        if (!listeners.isEmpty()){
            for (int i = 0; i < listeners.size(); i++) {
                Listener l = listeners.get(i);
                if (l != null) {
                    l.onVisibilityChanged(visibility);
                }else {
                    Log.e(LOG_TAG, "Listener(" + i + ") is null, skip (onVisibilityChanged) notify for him...");
                }
            }
        }
    }

    @Override
    public void onAnimationStart(Animator animator) {
        sliderView.setVisibility(VISIBLE);
        notifyVisibilityChanged(VISIBLE);
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        if (slideAnimationTo > 0){
            sliderView.setVisibility(GONE);
            notifyVisibilityChanged(GONE);
        }
    }

    @Override
    public void onAnimationCancel(Animator animator) {}

    @Override
    public void onAnimationRepeat(Animator animator) {}

    public interface Listener {
        void onSlide(float percent);
        void onVisibilityChanged(int visibility);
    }

    public static class ListenerAdapter implements Listener {
        public void onSlide(float percent){}
        public void onVisibilityChanged(int visibility){}
    }

}
