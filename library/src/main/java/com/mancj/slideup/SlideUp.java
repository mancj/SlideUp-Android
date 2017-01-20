package com.mancj.slideup;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mancj.slideup.SlideUp.State.HIDDEN;
import static com.mancj.slideup.SlideUp.State.SHOWED;

public class SlideUp<T extends View> implements View.OnTouchListener, ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
    private final static String LOG_TAG = "SlideUp";
    
    private State startState;
    private T sliderView;
    private float touchableArea;
    private int autoSlideDuration = 300;
    private List<Listener> listeners = new ArrayList<>();

    private ValueAnimator valueAnimator;
    private float slideAnimationTo;

    private float startPositionY;
    private float viewStartPositionY;
    private boolean canSlide = true;
    private float density;
    private float maxSlidePosition;
    private float viewHeight;

    private boolean downToUp = true;
    
    private boolean debug = false;
    
    private SlideUp(Builder<T> builder){
        downToUp = builder.vectorDownToUp;
        listeners = builder.listeners;
        sliderView = builder.sliderView;
        startState = builder.startState;
        debug = builder.debug;
        init();
    }

    private void init() {
        density = sliderView.getResources().getDisplayMetrics().density;
        touchableArea = 300 * density;
        sliderView.setOnTouchListener(this);
        createAnimation();
        sliderView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                viewHeight = sliderView.getHeight();
                sliderView.setPivotY(downToUp ? 0 : viewHeight);
                updateToCurrentState();
                ViewTreeObserver observer = sliderView.getViewTreeObserver();
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                    observer.removeGlobalOnLayoutListener(this);
                } else {
                    observer.removeOnGlobalLayoutListener(this);
                }
            }
        });
        updateToCurrentState();
    }
    
    private void updateToCurrentState() {
        switch (startState){
            case HIDDEN:
                hideImmediately();
                break;
            case SHOWED:
                showImmediately();
                break;
        }
    }
    
    public void setDownToUp(){
        downToUp = true;
    }

    public void setUpToDown(){
        downToUp = false;
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

    public void setTouchableArea(float touchableArea) {
        this.touchableArea = touchableArea * density;
    }

    public float getTouchableArea() {
        return this.touchableArea / density;
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
        this.slideAnimationTo = downToUp ? sliderView.getHeight() : -sliderView.getHeight();
        valueAnimator.setFloatValues(sliderView.getTranslationY(), slideAnimationTo);
        valueAnimator.start();
    }
    
    public void hideImmediately() {
        if (sliderView.getHeight() > 0){
            sliderView.setTranslationY(downToUp ? viewHeight : -viewHeight);
            sliderView.setVisibility(GONE);
            notifyVisibilityChanged(GONE);
        }else {
            startState = HIDDEN;
        }
    }
    
    public void showImmediately() {
        if (sliderView.getHeight() > 0){
            sliderView.setTranslationY(0);
            sliderView.setVisibility(VISIBLE);
            notifyVisibilityChanged(VISIBLE);
        }else {
            startState = SHOWED;
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
        if (downToUp)
            return onTouchDownToUp(event);
        else
            return onTouchUpToDown(event);
    }
    
    private boolean onTouchDownToUp(MotionEvent event){
        float touchedArea = event.getRawY() - sliderView.getTop();
        if (isAnimationRunning()){
            return false;
        }
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                viewHeight = sliderView.getHeight();
                startPositionY = event.getRawY();
                viewStartPositionY = sliderView.getTranslationY();
                if (touchableArea < touchedArea){
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
                if (event.getRawY() > maxSlidePosition) {
                    maxSlidePosition = event.getRawY();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = sliderView.getTranslationY();
                boolean mustShow = maxSlidePosition > event.getRawY();
                boolean scrollableAreaConsumed = sliderView.getTranslationY() > sliderView.getHeight() / 5;

                if (scrollableAreaConsumed && !mustShow){
                    slideAnimationTo = sliderView.getHeight();
                }else {
                    slideAnimationTo = 0;
                }
                valueAnimator.setFloatValues(slideAnimationFrom, slideAnimationTo);
                valueAnimator.start();
                canSlide = true;
                maxSlidePosition = 0;
                break;
        }
        return true;
    }
    
    private boolean onTouchUpToDown(MotionEvent event){
        float touchedArea = event.getRawY() - sliderView.getBottom();
        if (isAnimationRunning()){
            return false;
        }
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                viewHeight = sliderView.getHeight();
                startPositionY = event.getRawY();
                viewStartPositionY = sliderView.getTranslationY();
                maxSlidePosition = viewHeight;
                if (touchableArea < touchedArea){
                    canSlide = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawY() - startPositionY;
                float moveTo = viewStartPositionY + difference;
                float percents = moveTo * 100 / -sliderView.getHeight();
                
                if (moveTo < 0 && canSlide){
                    notifyPercentChanged(percents);
                    sliderView.setTranslationY(moveTo);
                }
                if (event.getRawY() < maxSlidePosition) {
                    maxSlidePosition = event.getRawY();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = -sliderView.getTranslationY();
                boolean mustShow = maxSlidePosition < event.getRawY();
                boolean scrollableAreaConsumed = sliderView.getTranslationY() < -sliderView.getHeight() / 5;

                if (scrollableAreaConsumed && !mustShow){
                    slideAnimationTo = sliderView.getHeight();
                }else {
                    slideAnimationTo = 0;
                }
                valueAnimator.setFloatValues(slideAnimationFrom, slideAnimationTo);
                valueAnimator.start();
                canSlide = true;
                maxSlidePosition = 0;
                break;
        }
        return true;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float val = (float) animation.getAnimatedValue();
        sliderView.setTranslationY(downToUp ? val : -val);
        float visibleDistance = downToUp ?
                sliderView.getY() - sliderView.getTop()
                :
                sliderView.getTop() + sliderView.getY();
        float percents = (visibleDistance) * 100 / (downToUp ? viewHeight : -viewHeight);
        notifyPercentChanged(percents);
    }

    private void notifyPercentChanged(float percent){
        if (!listeners.isEmpty()){
            for (int i = 0; i < listeners.size(); i++) {
                Listener l = listeners.get(i);
                if (l != null){
                    l.onSlide(percent);
                    d("Listener(" + i + ")", "(onSlide)", "value = " + percent);
                }else {
                    e("Listener(" + i + ")", "(onSlide)", "Listener is null, skip notify for him...");
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
                    d("Listener(" + i + ")", "(onVisibilityChanged)", "value = " + (visibility == VISIBLE ? "VISIBLE" : visibility == GONE ? "GONE" : visibility));
                }else {
                    e("Listener(" + i + ")", "(onVisibilityChanged)", "Listener is null, skip  notify for him...");
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
        if (slideAnimationTo != 0){
            sliderView.setVisibility(GONE);
            notifyVisibilityChanged(GONE);
        }
    }

    @Override
    public void onAnimationCancel(Animator animator) {}

    @Override
    public void onAnimationRepeat(Animator animator) {}
    
    private void e(String listener, String method, String message){
        if (debug)
            Log.e(LOG_TAG, String.format("%1$-15s %2$-23s %3$s", listener, method, message));
    }
    
    private void d(String listener, String method, String value){
        if (debug)
            Log.d(LOG_TAG, String.format("%1$-15s %2$-23s %3$s", listener, method, value));
    }

    public interface Listener {
        void onSlide(float percent);
        void onVisibilityChanged(int visibility);
    }

    public static class ListenerAdapter implements Listener {
        public void onSlide(float percent){}
        public void onVisibilityChanged(int visibility){}
    }
    
    public static class Builder<T extends View>{
        private T sliderView;
        private State startState = HIDDEN;
        private boolean vectorDownToUp = true;
        private List<Listener> listeners = new ArrayList<>();
        private boolean debug = false;
        
        private Builder(){}
        
        public static Builder forView(@NonNull View sliderView){
            Builder builder = new Builder();
            builder.sliderView = sliderView;
            return builder;
        }
        
        public Builder withStartState(@NonNull State startState){
            this.startState = startState;
            return this;
        }
        
        public Builder withDownToUpVector(boolean downToUp){
            vectorDownToUp = downToUp;
            return this;
        }
        
        public Builder withListeners(@NonNull List<Listener> listeners){
            this.listeners = listeners;
            return this;
        }
        
        public Builder withListeners(@NonNull Listener... listeners){
            List<Listener> listeners_list = new ArrayList<>();
            Collections.addAll(listeners_list, listeners);
            return withListeners(listeners_list);
        }
        
        public Builder withLoggingEnabled(boolean enable){
            debug = enable;
            return this;
        }
        
        
        public SlideUp<T> build(){
            return new SlideUp<>(this);
        }
        
    }
    
    public enum State implements Parcelable {
        HIDDEN, SHOWED;
    
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(ordinal());
        }
    
        @Override
        public int describeContents() {
            return 0;
        }
    
        public static final Creator<State> CREATOR = new Creator<State>() {
            @Override
            public State createFromParcel(Parcel in) {
                return State.values()[in.readInt()];
            }
        
            @Override
            public State[] newArray(int size) {
                return new State[size];
            }
        };
    }
}
