package com.mancj.slideup;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    private final static String TAG = "SlideUp";
    
    private final static String KEY_DIRECTION = TAG + "_direction";
    private final static String KEY_DEBUG = TAG + "_debug";
    private final static String KEY_TOUCHABLE_AREA = TAG + "_touchable_area";
    private final static String KEY_STATE = TAG + "_state";
    private final static String KEY_AUTO_SLIDE_DURATION = TAG + "_auto_slide_duration";
    
    private State startState;
    private State currentState;
    private T sliderView;
    private float touchableArea;
    private int autoSlideDuration;
    private List<Listener> listeners;

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
        density = builder.density;
        touchableArea = builder.touchableArea;
        autoSlideDuration = builder.autoSlideDuration;
        debug = builder.debug;
        init();
    }

    private void init() {
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
    
    public Bundle onSaveInstanceState(@Nullable Bundle savedState){
        if (savedState == null) savedState = Bundle.EMPTY;
        savedState.putBoolean(KEY_DIRECTION, downToUp);
        savedState.putBoolean(KEY_DEBUG, debug);
        savedState.putFloat(KEY_TOUCHABLE_AREA, touchableArea / density);
        savedState.putParcelable(KEY_STATE, currentState);
        savedState.putInt(KEY_AUTO_SLIDE_DURATION, autoSlideDuration);
        return savedState;
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
        float touchedArea = sliderView.getBottom() - event.getRawY();
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
                    slideAnimationTo = sliderView.getHeight() + sliderView.getTop();
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
        float total = downToUp ? viewHeight : -viewHeight;
        float percents = (sliderView.getY() - sliderView.getTop()) * 100 / total;
        notifyPercentChanged(percents);
    }

    private void notifyPercentChanged(float percent){
        percent = percent > 100 ? 100 : percent;
        percent = percent < 0 ? 0 : percent;
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
        switch (visibility){
            case VISIBLE: currentState = SHOWED;
            case GONE: currentState = HIDDEN;
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
            Log.e(TAG, String.format("%1$-15s %2$-23s %3$s", listener, method, message));
    }
    
    private void d(String listener, String method, String value){
        if (debug)
            Log.d(TAG, String.format("%1$-15s %2$-23s %3$s", listener, method, value));
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
        private float touchableArea;
        private int autoSlideDuration = 300;
        private float density;
        
        private Builder(){}
        
        public static Builder forView(@NonNull View sliderView){
            Builder builder = new Builder();
            builder.sliderView = sliderView;
            builder.density = sliderView.getResources().getDisplayMetrics().density;
            builder.touchableArea = 300 * builder.density;
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
        
        public Builder withAutoSlideDuration(int duration){
            autoSlideDuration = duration;
            return this;
        }
        
        public Builder withTouchableArea(float area){
            touchableArea = area * density;
            return this;
        }
        
        /**
         * If you want to restore saved params, place this method in end of builder
         * */
        public Builder withSavedState(@Nullable Bundle savedState){
            restoreParams(savedState);
            return this;
        }
        
        
        public SlideUp<T> build(){
            return new SlideUp<>(this);
        }
    
    
        private void restoreParams(@Nullable Bundle savedState){
            if (savedState == null) return;
            if (savedState.getParcelable(KEY_STATE) != null)
                startState = savedState.getParcelable(KEY_STATE);
            vectorDownToUp = savedState.getBoolean(KEY_DIRECTION, vectorDownToUp);
            debug = savedState.getBoolean(KEY_DEBUG, debug);
            touchableArea = savedState.getFloat(KEY_TOUCHABLE_AREA, touchableArea) * density;
            autoSlideDuration = savedState.getInt(KEY_AUTO_SLIDE_DURATION, autoSlideDuration);
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
