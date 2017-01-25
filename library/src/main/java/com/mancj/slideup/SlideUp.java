package com.mancj.slideup;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.END;
import static android.view.Gravity.START;
import static android.view.Gravity.TOP;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mancj.slideup.SlideUp.State.HIDDEN;
import static com.mancj.slideup.SlideUp.State.SHOWED;

public class SlideUp<T extends View> implements View.OnTouchListener, ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
    private final static String TAG = "SlideUp";

    private final static String KEY_START_GRAVITY = TAG + "_start_gravity";
    private final static String KEY_DEBUG = TAG + "_debug";
    private final static String KEY_TOUCHABLE_AREA = TAG + "_touchable_area";
    private final static String KEY_STATE = TAG + "_state";
    private final static String KEY_AUTO_SLIDE_DURATION = TAG + "_auto_slide_duration";
    private final static String KEY_HIDE_SOFT_INPUT = TAG + "_hide_soft_input";
    
    /**
     * <p>Available start states</p>
     * */
    public enum State implements Parcelable, Serializable {
        
        /**
         * State hidden is equal {@link View#GONE}
         * */
        HIDDEN,
    
        /**
         * State showed is equal {@link View#VISIBLE}
         * */
        SHOWED;

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

    @IntDef(value = {START, END, TOP, BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    private @interface StartVector{}
    
    private State startState;
    private State currentState;
    private T sliderView;
    private float touchableArea;
    private int autoSlideDuration;
    private List<Listener> listeners;

    private ValueAnimator valueAnimator;
    private float slideAnimationTo;

    private float startPositionY;
    private float startPositionX;
    private float viewStartPositionY;
    private float viewStartPositionX;
    private boolean canSlide = true;
    private float density;
    private float maxSlidePosition;
    private float viewHeight;
    private float viewWidth;
    private boolean hideKeyboard;
    private TimeInterpolator interpolator;

    private boolean gesturesEnabled;

    private boolean isRTL;
    
    private int startGravity;
    
    private boolean debug;

    /**
     * <p>Interface to listen to all handled events taking place in the slider</p>
     * */
    public interface Listener {

        /**
         * @param percent percents of complete slide <b color="#EF6C00">(100 = HIDDEN, 0 = SHOWED)</b>
         * */
        void onSlide(float percent);

        /**
         * @param visibility (<b>GONE</b> or <b>VISIBLE</b>)
         * */
        void onVisibilityChanged(int visibility);
    }

    /**
     * <p>Adapter for {@link Listener}. With it you can use all, some single, or none method from Listener</p>
     * */
    public static class ListenerAdapter implements Listener {
        public void onSlide(float percent){}
        public void onVisibilityChanged(int visibility){}
    }
    /**
     * <p>Default constructor for SlideUp</p>
     * */
    public final static class Builder<T extends View>{
        private T sliderView;
        private float density;
        private float touchableArea;
        private boolean isRTL;
        private State startState = HIDDEN;
        private List<Listener> listeners = new ArrayList<>();
        private boolean debug = false;
        private int autoSlideDuration = 300;
        private int startGravity = BOTTOM;
        private boolean gesturesEnabled = true;
        private boolean hideKeyboard = false;
        private TimeInterpolator interpolator = new DecelerateInterpolator();

        /**
         * <p>Construct a SlideUp by passing the view or his child to use for the generation</p>
         * */
        public Builder(@NonNull T sliderView){
            this.sliderView = sliderView;
            density = sliderView.getResources().getDisplayMetrics().density;
            isRTL = sliderView.getResources().getBoolean(R.bool.is_right_to_left);
            touchableArea = 300 * density;
        }

        /**
         * <p>Define a start state on screen</p>
         *
         * @param startState <b>(default - <b color="#EF6C00">{@link State#HIDDEN}</b>)</b>
         * */
        public Builder withStartState(@NonNull State startState){
            this.startState = startState;
            return this;
        }

        /**
         * <p>Define a start gravity, <b>this parameter affects the motion vector slider</b></p>
         *
         * @param gravity <b>(default - <b color="#EF6C00">{@link android.view.Gravity#BOTTOM}</b>)</b>
         * */
        public Builder withStartGravity(@StartVector int gravity){
            startGravity = gravity;
            return this;
        }

        /**
         * <p>Define a {@link Listener} for this SlideUp</p>
         *
         * @param listeners {@link List} of listeners
         * */
        public Builder withListeners(@NonNull List<Listener> listeners){
            this.listeners = listeners;
            return this;
        }

        /**
         * <p>Define a {@link Listener} for this SlideUp</p>
         *
         * @param listeners array of listeners
         * */
        public Builder withListeners(@NonNull Listener... listeners){
            List<Listener> listeners_list = new ArrayList<>();
            Collections.addAll(listeners_list, listeners);
            return withListeners(listeners_list);
        }

        /**
         * <p>Turning on/off debug logging for all handled events</p>
         *
         * @param enabled <b>(default - <b color="#EF6C00">false</b>)</b>
         * */
        public Builder withLoggingEnabled(boolean enabled){
            debug = enabled;
            return this;
        }

        /**
         * <p>Define duration of animation (whenever you use {@link #hide()} or {@link #show()} methods)</p>
         *
         * @param duration <b>(default - <b color="#EF6C00">300</b>)</b>
         * */
        public Builder withAutoSlideDuration(int duration){
            autoSlideDuration = duration;
            return this;
        }

        /**
         * <p>Define touchable area <b>(in dp)</b> for interaction</p>
         *
         * @param area <b>(default - <b color="#EF6C00">300dp</b>)</b>
         * */
        public Builder withTouchableArea(float area){
            touchableArea = area * density;
            return this;
        }

        /**
         * <p>Turning on/off sliding on touch event</p>
         *
         * @param enabled <b>(default - <b color="#EF6C00">true</b>)</b>
         * */
        public Builder withGesturesEnabled(boolean enabled){
            gesturesEnabled = enabled;
            return this;
        }

        /**
         * <p>Define behavior of soft input</p>
         *
         * @param hide <b>(default - <b color="#EF6C00">false</b>)</b>
         * */
        public Builder withHideSoftInputWhenDisplayed(boolean hide){
            hideKeyboard = hide;
            return this;
        }
        
        /**
         * <p>Define interpolator for animation (whenever you use {@link #hide()} or {@link #show()} methods)</p>
         *
         * @param interpolator <b>(default - <b color="#EF6C00">Decelerate interpolator</b>)</b>
         * */
        public Builder withInterpolator(TimeInterpolator interpolator){
            this.interpolator = interpolator;
            return this;
        }
        
        /**
         * <p>
         * <b color="#EF6C00">IMPORTANT:</b>
         * If you want to restore saved parameters, place this method at the end of builder
         * </p>
         * @param savedState parameters will be restored from this bundle, if it contains them
         * */
        public Builder withSavedState(@Nullable Bundle savedState){
            restoreParams(savedState);
            return this;
        }

        /**
         * <p>Build the SlideUp and add behavior to view</p>
         * */
        public SlideUp<T> build(){
            return new SlideUp<>(this);
        }

        /**
         * <p>Trying restore saved state</p>
         * */
        private void restoreParams(@Nullable Bundle savedState){
            if (savedState == null) return;
            if (savedState.getParcelable(KEY_STATE) != null)
                startState = savedState.getParcelable(KEY_STATE);
            startGravity = savedState.getInt(KEY_START_GRAVITY, startGravity);
            debug = savedState.getBoolean(KEY_DEBUG, debug);
            touchableArea = savedState.getFloat(KEY_TOUCHABLE_AREA, touchableArea) * density;
            autoSlideDuration = savedState.getInt(KEY_AUTO_SLIDE_DURATION, autoSlideDuration);
            hideKeyboard = savedState.getBoolean(KEY_HIDE_SOFT_INPUT, hideKeyboard);
        }
    }
    
    /**
     * <p>Trying hide soft input from window</p>
     *
     * @see InputMethodManager#hideSoftInputFromWindow(IBinder, int)
     * */
    public void hideSoftInput(){
        ((InputMethodManager) sliderView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(sliderView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
    
    /**
     * <p>Trying show soft input to window</p>
     *
     * @see InputMethodManager#showSoftInput(View, int)
     * */
    public void showSoftInput(){
        ((InputMethodManager) sliderView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(sliderView, 0);
    }
    
    private SlideUp(Builder<T> builder){
        startGravity = builder.startGravity;
        listeners = builder.listeners;
        sliderView = builder.sliderView;
        startState = builder.startState;
        density = builder.density;
        touchableArea = builder.touchableArea;
        autoSlideDuration = builder.autoSlideDuration;
        debug = builder.debug;
        isRTL = builder.isRTL;
        gesturesEnabled = builder.gesturesEnabled;
        hideKeyboard = builder.hideKeyboard;
        interpolator = builder.interpolator;
        init();
    }

    private void init() {
        sliderView.setOnTouchListener(this);
        createAnimation();
        sliderView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                viewHeight = sliderView.getHeight();
                viewWidth = sliderView.getWidth();
                switch (startGravity){
                    case TOP:    sliderView.setPivotY(viewHeight); break;
                    case BOTTOM: sliderView.setPivotY(0);          break;
                    case START:  sliderView.setPivotX(0);          break;
                    case END:    sliderView.setPivotX(viewWidth);  break;
                }
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

    /**
     * <p>Returns the visibility status for this view.</p>
     *
     * @return true if view have status {@link View#VISIBLE}
     */
    public boolean isVisible(){
        return sliderView.getVisibility() == VISIBLE;
    }

    /**
     * <p>Add Listener which will be used in combination with this SlideUp</p>
     * */
    public void addSlideListener(@NonNull Listener listener){
        listeners.add(listener);
    }

    /**
     * <p>Remove Listener which was used in combination with this SlideUp</p>
     * */
    public void removeSlideListener(@NonNull Listener listener){
        listeners.remove(listener);
    }

    /**
     * <p>Returns typed view which was used as slider</p>
     * */
    public T getSliderView() {
        return sliderView;
    }

    /**
     * <p>Set duration of animation (whenever you use {@link #hide()} or {@link #show()} methods)</p>
     *
     * @param autoSlideDuration <b>(default - <b color="#EF6C00">300</b>)</b>
     * */
    public void setAutoSlideDuration(int autoSlideDuration) {
        this.autoSlideDuration = autoSlideDuration;
    }

    /**
     * <p>Returns duration of animation (whenever you use {@link #hide()} or {@link #show()} methods)</p>
     * */
    public float getAutoSlideDuration(){
        return this.autoSlideDuration;
    }

    /**
     * <p>Set touchable area <b>(in dp)</b> for interaction</p>
     *
     * @param touchableArea <b>(default - <b color="#EF6C00">300dp</b>)</b>
     * */
    public void setTouchableArea(float touchableArea) {
        this.touchableArea = touchableArea * density;
    }

    /**
     * <p>Returns touchable area <b>(in dp)</b> for interaction</p>
     * */
    public float getTouchableArea() {
        return this.touchableArea / density;
    }

    /**
     * <p>Returns running status of animation</p>
     *
     * @return true if animation is running
     * */
    public boolean isAnimationRunning(){
        return valueAnimator != null && valueAnimator.isRunning();
    }

    /**
     * <p>Show view with animation</p>
     * */
    public void show(){
        show(false);
    }

    /**
     * <p>Hide view with animation</p>
     * */
    public void hide(){
        hide(false);
    }

    /**
     * <p>Hide view without animation</p>
     * */
    public void hideImmediately() {
        hide(true);
    }

    /**
     * <p>Show view without animation</p>
     * */
    public void showImmediately() {
        show(true);
    }

    /**
     * <p>Turning on/off debug logging</p>
     *
     * @param enabled <b>(default - <b color="#EF6C00">false</b>)</b>
     * */
    public void setLoggingEnabled(boolean enabled){
        debug = enabled;
    }

    /**
     * <p>Returns current status of debug logging</p>
     * */
    public boolean isLoggingEnabled(){
        return debug;
    }

    /**
     * <p>Turning on/off gestures</p>
     *
     * @param enabled <b>(default - <b color="#EF6C00">true</b>)</b>
     * */
    public void setGesturesEnabled(boolean enabled) {
        this.gesturesEnabled = enabled;
    }

    /**
     * <p>Returns current status of gestures</p>
     * */
    public boolean isGesturesEnabled() {
        return gesturesEnabled;
    }
    
    /**
     * <p>Returns current interpolator</p>
     * */
    public TimeInterpolator getInterpolator() {
        return interpolator;
    }
    
    /**
     * <p>Returns gravity which used in combination with this SlideUp</p>
     * */
    @StartVector
    public int getStartGravity() {
        return startGravity;
    }
    
    /**
     * <p>Sets interpolator for animation (whenever you use {@link #hide()} or {@link #show()} methods)</p>
     *
     * @param interpolator <b>(default - <b color="#EF6C00">Decelerate interpolator</b>)</b>
     * */
    public void setInterpolator(TimeInterpolator interpolator) {
        valueAnimator.setInterpolator(this.interpolator = interpolator);
    }
    
    /**
     * <p>Returns current behavior of soft input</p>
     * */
    public boolean isHideKeyboardWhenDisplayed() {
        return hideKeyboard;
    }
    
    /**
     * <p>Sets behavior of soft input</p>
     *
     * @param hide <b>(default - <b color="#EF6C00">false</b>)</b>
     * */
    public void setHideKeyboardWhenDisplayed(boolean hide) {
        hideKeyboard = hide;
    }

    /**
     * <p>Saving current parameters of SlideUp</p>
     *
     * @return {@link Bundle} with saved parameters of SlideUp
     * */
    public Bundle onSaveInstanceState(@Nullable Bundle savedState){
        if (savedState == null) savedState = Bundle.EMPTY;
        savedState.putInt(KEY_START_GRAVITY, startGravity);
        savedState.putBoolean(KEY_DEBUG, debug);
        savedState.putFloat(KEY_TOUCHABLE_AREA, touchableArea / density);
        savedState.putParcelable(KEY_STATE, currentState);
        savedState.putInt(KEY_AUTO_SLIDE_DURATION, autoSlideDuration);
        savedState.putBoolean(KEY_HIDE_SOFT_INPUT, hideKeyboard);
        return savedState;
    }

    private void hide(boolean immediately) {
        switch (startGravity){
            case TOP:
                if (immediately){
                    if (sliderView.getHeight() > 0){
                        sliderView.setTranslationY(viewHeight);
                        sliderView.setVisibility(GONE);
                        notifyVisibilityChanged(GONE);
                    }else {
                        startState = HIDDEN;
                    }
                }else {
                    this.slideAnimationTo = sliderView.getHeight();
                    valueAnimator.setFloatValues(sliderView.getTranslationY(), slideAnimationTo);
                    valueAnimator.start();
                }
                break;
            case BOTTOM:
                if (immediately){
                    if (sliderView.getHeight() > 0){
                        sliderView.setTranslationY(-viewHeight);
                        sliderView.setVisibility(GONE);
                        notifyVisibilityChanged(GONE);
                    }else {
                        startState = HIDDEN;
                    }
                }else {
                    this.slideAnimationTo = -sliderView.getHeight();
                    valueAnimator.setFloatValues(sliderView.getTranslationY(), slideAnimationTo);
                    valueAnimator.start();
                }
                break;
            case START:
                if (immediately){
                    if (sliderView.getWidth() > 0){
                        sliderView.setTranslationX(viewWidth);
                        sliderView.setVisibility(GONE);
                        notifyVisibilityChanged(GONE);
                    }else {
                        startState = HIDDEN;
                    }
                }else {
                    this.slideAnimationTo = sliderView.getWidth();
                    valueAnimator.setFloatValues(sliderView.getTranslationX(), slideAnimationTo);
                    valueAnimator.start();
                }
                break;
            case END:
                if (immediately){
                    if (sliderView.getWidth() > 0){
                        sliderView.setTranslationX(-viewWidth);
                        sliderView.setVisibility(GONE);
                        notifyVisibilityChanged(GONE);
                    }else {
                        startState = HIDDEN;
                    }
                }else {
                    this.slideAnimationTo = -sliderView.getHeight();
                    valueAnimator.setFloatValues(sliderView.getTranslationX(), slideAnimationTo);
                    valueAnimator.start();
                }
                break;
        }
    }
    
    private void show(boolean immediately){
        switch (startGravity) {
            case TOP:
            case BOTTOM:
                if (immediately){
                    if (sliderView.getHeight() > 0){
                        sliderView.setTranslationY(0);
                        sliderView.setVisibility(VISIBLE);
                        notifyVisibilityChanged(VISIBLE);
                    }else {
                        startState = SHOWED;
                    }
                }else {
                    this.slideAnimationTo = 0;
                    valueAnimator.setFloatValues(viewHeight, slideAnimationTo);
                    valueAnimator.start();
                }
                break;
            case START:
            case END:
                if (immediately){
                    if (sliderView.getWidth() > 0){
                        sliderView.setTranslationX(0);
                        sliderView.setVisibility(VISIBLE);
                        notifyVisibilityChanged(VISIBLE);
                    }else {
                        startState = SHOWED;
                    }
                }else {
                    this.slideAnimationTo = 0;
                    valueAnimator.setFloatValues(viewWidth, slideAnimationTo);
                    valueAnimator.start();
                }
                break;
        }
    }

    private void createAnimation(){
        valueAnimator = ValueAnimator.ofFloat();
        valueAnimator.setDuration(autoSlideDuration);
        valueAnimator.setInterpolator(interpolator);
        valueAnimator.addUpdateListener(this);
        valueAnimator.addListener(this);
    }

    @Override
    public final boolean onTouch(View v, MotionEvent event) {
        if (!gesturesEnabled) return false;
        if (isAnimationRunning()) return false;
        switch (startGravity){
            case TOP:
                return onTouchUpToDown(event);
            case BOTTOM:
                return onTouchDownToUp(event);
            case START:
                return onTouchStartToEnd(event);
            case END:
                return onTouchEndToStart(event);
            default:
                e("onTouchListener", "(onTouch)", "You are using not supportable gravity");
                return false;
        }
    }

    private boolean onTouchEndToStart(MotionEvent event){
        float touchedArea = event.getRawX() - getEnd();
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                viewWidth = sliderView.getWidth();
                startPositionX = event.getRawX();
                viewStartPositionX = sliderView.getTranslationX();
                if (touchableArea < touchedArea){
                    canSlide = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawX() - startPositionX;
                float moveTo = viewStartPositionX + difference;
                float percents = moveTo * 100 / sliderView.getWidth();

                if (moveTo > 0 && canSlide){
                    notifyPercentChanged(percents);
                    sliderView.setTranslationX(moveTo);
                }
                if (event.getRawX() > maxSlidePosition) {
                    maxSlidePosition = event.getRawX();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = sliderView.getTranslationX();
                if (slideAnimationFrom == viewStartPositionX) return false;
                boolean mustShow = maxSlidePosition > event.getRawX();
                boolean scrollableAreaConsumed = sliderView.getTranslationX() > sliderView.getWidth() / 5;

                if (scrollableAreaConsumed && !mustShow){
                    slideAnimationTo = sliderView.getWidth();
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

    private boolean onTouchStartToEnd(MotionEvent event){
        float touchedArea = getEnd() - event.getRawX();
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                maxSlidePosition = viewWidth;
                viewWidth = sliderView.getWidth();
                startPositionX = event.getRawX();
                viewStartPositionX = sliderView.getTranslationX();
                if (touchableArea < touchedArea){
                    canSlide = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawX() - startPositionX;
                float moveTo = viewStartPositionX + difference;
                float percents = moveTo * 100 / -sliderView.getWidth();

                if (moveTo < 0 && canSlide){
                    notifyPercentChanged(percents);
                    sliderView.setTranslationX(moveTo);
                }
                if (event.getRawX() < maxSlidePosition) {
                    maxSlidePosition = event.getRawX();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = -sliderView.getTranslationX();
                if (slideAnimationFrom == viewStartPositionX) return false;
                boolean mustShow = maxSlidePosition < event.getRawX();
                boolean scrollableAreaConsumed = sliderView.getTranslationX() < -sliderView.getHeight() / 5;

                if (scrollableAreaConsumed && !mustShow){
                    slideAnimationTo = sliderView.getWidth();
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

    private boolean onTouchDownToUp(MotionEvent event){
        float touchedArea = event.getRawY() - sliderView.getTop();
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
                if (slideAnimationFrom == viewStartPositionY) return false;
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
                if (slideAnimationFrom == viewStartPositionY) return false;
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
    public final void onAnimationUpdate(ValueAnimator animation) {
        float value = (float) animation.getAnimatedValue();
        switch (startGravity){
            case TOP:    onAnimationUpdateUpToDown(value);   break;
            case BOTTOM: onAnimationUpdateDownToUp(value);   break;
            case START:  onAnimationUpdateStartToEnd(value); break;
            case END:    onAnimationUpdateEndToStart(value); break;
        }
    }

    private void onAnimationUpdateUpToDown(float value){
        sliderView.setTranslationY(-value);
        float visibleDistance = sliderView.getTop() - sliderView.getY();
        float percents = (visibleDistance) * 100 / viewHeight;
        notifyPercentChanged(percents);
    }

    private void onAnimationUpdateDownToUp(float value){
        sliderView.setTranslationY(value);
        float visibleDistance = sliderView.getY() - sliderView.getTop();
        float percents = (visibleDistance) * 100 / viewHeight;
        notifyPercentChanged(percents);
    }

    private void onAnimationUpdateStartToEnd(float value){
        sliderView.setTranslationX(-value);
        float visibleDistance = sliderView.getX() - getStart();
        float percents = (visibleDistance) * 100 / -viewWidth;
        notifyPercentChanged(percents);
    }

    private void onAnimationUpdateEndToStart(float value){
        sliderView.setTranslationX(value);
        float visibleDistance = sliderView.getX() - getStart();
        float percents = (visibleDistance) * 100 / viewWidth;
        notifyPercentChanged(percents);
    }

    private int getStart(){
        if (isRTL){
            return sliderView.getRight();
        }else {
            return sliderView.getLeft();
        }
    }

    private int getEnd(){
        if (isRTL){
            return sliderView.getLeft();
        }else {
            return sliderView.getRight();
        }
    }

    private void notifyPercentChanged(float percent){
        percent = percent > 100 ? 100 : percent;
        percent = percent < 0 ? 0 : percent;
        if (slideAnimationTo == 0 && hideKeyboard)
            hideSoftInput();
        if (listeners != null && !listeners.isEmpty()){
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
        if (listeners != null && !listeners.isEmpty()){
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
            case VISIBLE: currentState = SHOWED; break;
            case GONE:    currentState = HIDDEN; break;
        }
    }

    @Override
    public final void onAnimationStart(Animator animator) {
        if (sliderView.getVisibility() != VISIBLE) {
            sliderView.setVisibility(VISIBLE);
            notifyVisibilityChanged(VISIBLE);
        }
    }

    @Override
    public final void onAnimationEnd(Animator animator) {
        if (slideAnimationTo != 0){
            if (sliderView.getVisibility() != GONE) {
                sliderView.setVisibility(GONE);
                notifyVisibilityChanged(GONE);
            }
        }
    }

    @Override
    public final void onAnimationCancel(Animator animator) {}

    @Override
    public final void onAnimationRepeat(Animator animator) {}
    
    private void e(String listener, String method, String message){
        if (debug)
            Log.e(TAG, String.format("%1$-15s %2$-23s %3$s", listener, method, message));
    }
    
    private void d(String listener, String method, String value){
        if (debug)
            Log.d(TAG, String.format("%1$-15s %2$-23s %3$s", listener, method, value));
    }
}
