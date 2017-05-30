package com.mancj.slideup;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;

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

public class SlideUp implements View.OnTouchListener, ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
    private final static String TAG = SlideUp.class.getSimpleName();

    private final static String KEY_START_GRAVITY = TAG + "_start_gravity";
    private final static String KEY_DEBUG = TAG + "_debug";
    private final static String KEY_TOUCHABLE_AREA = TAG + "_touchable_area";
    private final static String KEY_STATE = TAG + "_state";
    private final static String KEY_AUTO_SLIDE_DURATION = TAG + "_auto_slide_duration";
    private final static String KEY_HIDE_SOFT_INPUT = TAG + "_hide_soft_input";
    private final static String KEY_STATE_SAVED = TAG + "_state_saved";
    
    /**
     * <p>Available start states</p>
     * */
    public enum State{
        
        /**
         * State hidden is equal {@link View#GONE}
         * */
        HIDDEN,
    
        /**
         * State showed is equal {@link View#VISIBLE}
         * */
        SHOWED
    }

    @IntDef(value = {START, END, TOP, BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    private @interface StartVector{}
    
    private State mStartState;
    private State mCurrentState;
    private View mSliderView;
    private float mTouchableArea;
    private int mAutoSlideDuration;
    private List<Listener> mListeners;

    private ValueAnimator mValueAnimator;
    private float mSlideAnimationTo;

    private float mStartPositionY;
    private float mStartPositionX;
    private float mViewStartPositionY;
    private float mViewStartPositionX;
    private boolean mCanSlide = true;
    private float mDensity;
    private float mMaxSlidePosition;
    private float mViewHeight;
    private float mViewWidth;
    private boolean mHideKeyboard;
    private TimeInterpolator mInterpolator;

    private boolean mGesturesEnabled;

    private boolean mIsRTL;
    
    private int mStartGravity;
    
    private boolean mDebug;

    /**
     * <p>Interface to listen to all handled events taking place in the slider</p>
     * */
    public interface Listener {
        
        interface Slide extends Listener{
            
            /**
             * @param percent percents of complete slide <b color="#EF6C00">(100 = HIDDEN, 0 = SHOWED)</b>
             * */
            void onSlide(float percent);
        }

        interface Visibility extends Listener{
    
            /**
             * @param visibility (<b>GONE</b> or <b>VISIBLE</b>)
             * */
            void onVisibilityChanged(int visibility);
        }
        
        interface Events extends Visibility, Slide{}
    }
    
    /**
     * <p>Default constructor for SlideUp</p>
     * */
    public final static class Builder{
        private boolean mStateRestored = false;
        
        private View mSliderView;
        private float mDensity;
        private float mTouchableArea;
        private boolean mIsRTL;
        private State mStartState = HIDDEN;
        private List<Listener> mListeners = new ArrayList<>();
        private boolean mDebug = false;
        private int mAutoSlideDuration = 300;
        private int mStartGravity = BOTTOM;
        private boolean mGesturesEnabled = true;
        private boolean mHideKeyboard = false;
        private TimeInterpolator mInterpolator = new DecelerateInterpolator();

        /**
         * <p>Construct a SlideUp by passing the view or his child to use for the generation</p>
         * */
        public Builder(@NonNull View sliderView){
            this.mSliderView = sliderView;
            mDensity = sliderView.getResources().getDisplayMetrics().density;
            mIsRTL = sliderView.getResources().getBoolean(R.bool.is_right_to_left);
            mTouchableArea = 300 * mDensity;
        }

        /**
         * <p>Define a start state on screen</p>
         *
         * @param startState <b>(default - <b color="#EF6C00">{@link State#HIDDEN}</b>)</b>
         * */
        public Builder withStartState(@NonNull State startState){
            if (!mStateRestored) {
                this.mStartState = startState;
            }
            return this;
        }

        /**
         * <p>Define a start gravity, <b>this parameter affects the motion vector slider</b></p>
         *
         * @param gravity <b>(default - <b color="#EF6C00">{@link android.view.Gravity#BOTTOM}</b>)</b>
         * */
        public Builder withStartGravity(@StartVector int gravity){
            if (!mStateRestored) {
                mStartGravity = gravity;
            }
            return this;
        }

        /**
         * <p>Define a {@link Listener} for this SlideUp</p>
         *
         * @param listeners {@link List} of listeners
         * */
        public Builder withListeners(@NonNull List<Listener> listeners){
            this.mListeners = listeners;
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
            if (!mStateRestored) {
                mDebug = enabled;
            }
            return this;
        }

        /**
         * <p>Define duration of animation (whenever you use {@link #hide()} or {@link #show()} methods)</p>
         *
         * @param duration <b>(default - <b color="#EF6C00">300</b>)</b>
         * */
        public Builder withAutoSlideDuration(int duration){
            if (!mStateRestored) {
                mAutoSlideDuration = duration;
            }
            return this;
        }

        /**
         * <p>Define touchable area <b>(in dp)</b> for interaction</p>
         *
         * @param area <b>(default - <b color="#EF6C00">300dp</b>)</b>
         * */
        public Builder withTouchableArea(float area){
            if (!mStateRestored) {
                mTouchableArea = area * mDensity;
            }
            return this;
        }

        /**
         * <p>Turning on/off sliding on touch event</p>
         *
         * @param enabled <b>(default - <b color="#EF6C00">true</b>)</b>
         * */
        public Builder withGesturesEnabled(boolean enabled){
            mGesturesEnabled = enabled;
            return this;
        }

        /**
         * <p>Define behavior of soft input</p>
         *
         * @param hide <b>(default - <b color="#EF6C00">false</b>)</b>
         * */
        public Builder withHideSoftInputWhenDisplayed(boolean hide){
            if (!mStateRestored) {
                mHideKeyboard = hide;
            }
            return this;
        }
        
        /**
         * <p>Define interpolator for animation (whenever you use {@link #hide()} or {@link #show()} methods)</p>
         *
         * @param interpolator <b>(default - <b color="#EF6C00">Decelerate interpolator</b>)</b>
         * */
        public Builder withInterpolator(TimeInterpolator interpolator){
            this.mInterpolator = interpolator;
            return this;
        }
        
        /**
         * @param savedState parameters will be restored from this bundle, if it contains them
         * */
        public Builder withSavedState(@Nullable Bundle savedState){
            restoreParams(savedState);
            return this;
        }

        /**
         * <p>Build the SlideUp and add behavior to view</p>
         * */
        public SlideUp build(){
            return new SlideUp(this);
        }

        /**
         * <p>Trying restore saved state</p>
         * */
        private void restoreParams(@Nullable Bundle savedState){
            if (savedState == null) return;
            mStateRestored = savedState.getBoolean(KEY_STATE_SAVED, false);
            if (savedState.getSerializable(KEY_STATE) != null) {
                mStartState = (SlideUp.State) savedState.getSerializable(KEY_STATE);
            }
            mStartGravity = savedState.getInt(KEY_START_GRAVITY, mStartGravity);
            mDebug = savedState.getBoolean(KEY_DEBUG, mDebug);
            mTouchableArea = savedState.getFloat(KEY_TOUCHABLE_AREA, mTouchableArea) * mDensity;
            mAutoSlideDuration = savedState.getInt(KEY_AUTO_SLIDE_DURATION, mAutoSlideDuration);
            mHideKeyboard = savedState.getBoolean(KEY_HIDE_SOFT_INPUT, mHideKeyboard);
        }
    }
    
    /**
     * <p>Trying hide soft input from window</p>
     *
     * @see InputMethodManager#hideSoftInputFromWindow(IBinder, int)
     * */
    public void hideSoftInput(){
        ((InputMethodManager) mSliderView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(mSliderView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
    
    /**
     * <p>Trying show soft input to window</p>
     *
     * @see InputMethodManager#showSoftInput(View, int)
     * */
    public void showSoftInput(){
        ((InputMethodManager) mSliderView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(mSliderView, 0);
    }
    
    private SlideUp(Builder builder){
        mStartGravity = builder.mStartGravity;
        mListeners = builder.mListeners;
        mSliderView = builder.mSliderView;
        mStartState = builder.mStartState;
        mDensity = builder.mDensity;
        mTouchableArea = builder.mTouchableArea;
        mAutoSlideDuration = builder.mAutoSlideDuration;
        mDebug = builder.mDebug;
        mIsRTL = builder.mIsRTL;
        mGesturesEnabled = builder.mGesturesEnabled;
        mHideKeyboard = builder.mHideKeyboard;
        mInterpolator = builder.mInterpolator;
        init();
    }

    private void init() {
        mSliderView.setOnTouchListener(this);
        createAnimation();
        mSliderView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mViewHeight = mSliderView.getHeight();
                mViewWidth = mSliderView.getWidth();
                switch (mStartGravity){
                    case TOP:    mSliderView.setPivotY(mViewHeight); break;
                    case BOTTOM: mSliderView.setPivotY(0);          break;
                    case START:  mSliderView.setPivotX(0);          break;
                    case END:    mSliderView.setPivotX(mViewWidth);  break;
                }
                updateToCurrentState();
                ViewTreeObserver observer = mSliderView.getViewTreeObserver();
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
        switch (mStartState){
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
        return mSliderView.getVisibility() == VISIBLE;
    }

    /**
     * <p>Add Listener which will be used in combination with this SlideUp</p>
     * */
    public void addSlideListener(@NonNull Listener listener){
        mListeners.add(listener);
    }

    /**
     * <p>Remove Listener which was used in combination with this SlideUp</p>
     * */
    public void removeSlideListener(@NonNull Listener listener){
        mListeners.remove(listener);
    }

    /**
     * <p>Returns typed view which was used as slider</p>
     * */
    public <T extends View> T getSliderView() {
        return (T) mSliderView;
    }

    /**
     * <p>Set duration of animation (whenever you use {@link #hide()} or {@link #show()} methods)</p>
     *
     * @param autoSlideDuration <b>(default - <b color="#EF6C00">300</b>)</b>
     * */
    public void setAutoSlideDuration(int autoSlideDuration) {
        this.mAutoSlideDuration = autoSlideDuration;
    }

    /**
     * <p>Returns duration of animation (whenever you use {@link #hide()} or {@link #show()} methods)</p>
     * */
    public float getAutoSlideDuration(){
        return this.mAutoSlideDuration;
    }

    /**
     * <p>Set touchable area <b>(in dp)</b> for interaction</p>
     *
     * @param touchableArea <b>(default - <b color="#EF6C00">300dp</b>)</b>
     * */
    public void setTouchableArea(float touchableArea) {
        this.mTouchableArea = touchableArea * mDensity;
    }

    /**
     * <p>Returns touchable area <b>(in dp)</b> for interaction</p>
     * */
    public float getTouchableArea() {
        return this.mTouchableArea / mDensity;
    }

    /**
     * <p>Returns running status of animation</p>
     *
     * @return true if animation is running
     * */
    public boolean isAnimationRunning(){
        return mValueAnimator != null && mValueAnimator.isRunning();
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
        mDebug = enabled;
    }

    /**
     * <p>Returns current status of debug logging</p>
     * */
    public boolean isLoggingEnabled(){
        return mDebug;
    }

    /**
     * <p>Turning on/off gestures</p>
     *
     * @param enabled <b>(default - <b color="#EF6C00">true</b>)</b>
     * */
    public void setGesturesEnabled(boolean enabled) {
        this.mGesturesEnabled = enabled;
    }

    /**
     * <p>Returns current status of gestures</p>
     * */
    public boolean isGesturesEnabled() {
        return mGesturesEnabled;
    }
    
    /**
     * <p>Returns current interpolator</p>
     * */
    public TimeInterpolator getInterpolator() {
        return mInterpolator;
    }
    
    /**
     * <p>Returns gravity which used in combination with this SlideUp</p>
     * */
    @StartVector
    public int getStartGravity() {
        return mStartGravity;
    }
    
    /**
     * <p>Sets interpolator for animation (whenever you use {@link #hide()} or {@link #show()} methods)</p>
     *
     * @param interpolator <b>(default - <b color="#EF6C00">Decelerate interpolator</b>)</b>
     * */
    public void setInterpolator(TimeInterpolator interpolator) {
        mValueAnimator.setInterpolator(this.mInterpolator = interpolator);
    }
    
    /**
     * <p>Returns current behavior of soft input</p>
     * */
    public boolean isHideKeyboardWhenDisplayed() {
        return mHideKeyboard;
    }
    
    /**
     * <p>Sets behavior of soft input</p>
     *
     * @param hide <b>(default - <b color="#EF6C00">false</b>)</b>
     * */
    public void setHideKeyboardWhenDisplayed(boolean hide) {
        mHideKeyboard = hide;
    }

    /**
     * <p>Toggle current state with animation</p>
     * */
    public void toggle(){
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }

    /**
     * <p>Toggle current state without animation</p>
     * */
    public void toggleImmediately(){
        if (isVisible()) {
            hideImmediately();
        } else {
            showImmediately();
        }
    }

    /**
     * <p>Saving current parameters of SlideUp</p>
     * */
    public void onSaveInstanceState(@NonNull Bundle savedState){
        savedState.putBoolean(KEY_STATE_SAVED, true);
        savedState.putInt(KEY_START_GRAVITY, mStartGravity);
        savedState.putBoolean(KEY_DEBUG, mDebug);
        savedState.putFloat(KEY_TOUCHABLE_AREA, mTouchableArea / mDensity);
        savedState.putSerializable(KEY_STATE, mCurrentState);
        savedState.putInt(KEY_AUTO_SLIDE_DURATION, mAutoSlideDuration);
        savedState.putBoolean(KEY_HIDE_SOFT_INPUT, mHideKeyboard);
    }

    private void endAnimation(){
        if (mValueAnimator != null && mValueAnimator.getValues() != null && mValueAnimator.isRunning()) {
            mValueAnimator.end();
        }
    }

    private void hide(boolean immediately) {
        endAnimation();
        switch (mStartGravity){
            case TOP:
                if (immediately){
                    if (mSliderView.getHeight() > 0){
                        mSliderView.setTranslationY(-mViewHeight);
                        notifyVisibilityChanged(GONE);
                    }else {
                        mStartState = HIDDEN;
                    }
                }else {
                    setValuesAndStart(mSliderView.getTranslationY(), mSliderView.getHeight());
                }
                break;
            case BOTTOM:
                if (immediately){
                    if (mSliderView.getHeight() > 0){
                        mSliderView.setTranslationY(mViewHeight);
                        notifyVisibilityChanged(GONE);
                    }else {
                        mStartState = HIDDEN;
                    }
                }else {
                    setValuesAndStart(mSliderView.getTranslationY(), mSliderView.getHeight());
                }
                break;
            case START:
                if (immediately){
                    if (mSliderView.getWidth() > 0){
                        mSliderView.setTranslationX(-mViewWidth);
                        notifyVisibilityChanged(GONE);
                    }else {
                        mStartState = HIDDEN;
                    }
                }else {
                    setValuesAndStart(mSliderView.getTranslationX(), mSliderView.getHeight());
                }
                break;
            case END:
                if (immediately){
                    if (mSliderView.getWidth() > 0){
                        mSliderView.setTranslationX(mViewWidth);
                        notifyVisibilityChanged(GONE);
                    }else {
                        mStartState = HIDDEN;
                    }
                }else {
                    setValuesAndStart(mSliderView.getTranslationX(), mSliderView.getHeight());
                }
                break;
        }
    }
    
    private void show(boolean immediately){
        endAnimation();
        switch (mStartGravity) {
            case TOP:
                if (immediately){
                    if (mSliderView.getHeight() > 0){
                        mSliderView.setTranslationY(0);
                        notifyVisibilityChanged(VISIBLE);
                    }else {
                        mStartState = SHOWED;
                    }
                }else {
                    setValuesAndStart(mSliderView.getTranslationY(), 0);
                }
            case BOTTOM:
                if (immediately){
                    if (mSliderView.getHeight() > 0){
                        mSliderView.setTranslationY(0);
                        notifyVisibilityChanged(VISIBLE);
                    }else {
                        mStartState = SHOWED;
                    }
                }else {
                    setValuesAndStart(mSliderView.getTranslationY(), 0);
                }
                break;
            case START:
                if (immediately){
                    if (mSliderView.getWidth() > 0){
                        mSliderView.setTranslationX(0);
                        notifyVisibilityChanged(VISIBLE);
                    }else {
                        mStartState = SHOWED;
                    }
                }else {
                    setValuesAndStart(mSliderView.getTranslationX(), 0);
                }
            case END:
                if (immediately){
                    if (mSliderView.getWidth() > 0){
                        mSliderView.setTranslationX(0);
                        notifyVisibilityChanged(VISIBLE);
                    }else {
                        mStartState = SHOWED;
                    }
                }else {
                    setValuesAndStart(mSliderView.getTranslationX(), 0);
                }
                break;
        }
    }
    
    private void setValuesAndStart(float from, float to){
        mSlideAnimationTo = to;
        mValueAnimator.setFloatValues(from, to);
        mValueAnimator.start();
    }

    private void createAnimation(){
        mValueAnimator = ValueAnimator.ofFloat();
        mValueAnimator.setDuration(mAutoSlideDuration);
        mValueAnimator.setInterpolator(mInterpolator);
        mValueAnimator.addUpdateListener(this);
        mValueAnimator.addListener(this);
    }

    @Override
    public final boolean onTouch(View v, MotionEvent event) {
        if (!mGesturesEnabled) return false;
        if (isAnimationRunning()) return false;
        switch (mStartGravity){
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
                mViewWidth = mSliderView.getWidth();
                mStartPositionX = event.getRawX();
                mViewStartPositionX = mSliderView.getTranslationX();
                if (mTouchableArea < touchedArea){
                    mCanSlide = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawX() - mStartPositionX;
                float moveTo = mViewStartPositionX + difference;
                float percents = moveTo * 100 / mSliderView.getWidth();

                if (moveTo > 0 && mCanSlide){
                    notifyPercentChanged(percents);
                    mSliderView.setTranslationX(moveTo);
                }
                if (event.getRawX() > mMaxSlidePosition) {
                    mMaxSlidePosition = event.getRawX();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = mSliderView.getTranslationX();
                if (slideAnimationFrom == mViewStartPositionX) return false;
                boolean mustShow = mMaxSlidePosition > event.getRawX();
                boolean scrollableAreaConsumed = mSliderView.getTranslationX() > mSliderView.getWidth() / 5;

                if (scrollableAreaConsumed && !mustShow){
                    mSlideAnimationTo = mSliderView.getWidth();
                }else {
                    mSlideAnimationTo = 0;
                }
                mValueAnimator.setFloatValues(slideAnimationFrom, mSlideAnimationTo);
                mValueAnimator.start();
                mCanSlide = true;
                mMaxSlidePosition = 0;
                break;
        }
        return true;
    }

    private boolean onTouchStartToEnd(MotionEvent event){
        float touchedArea = getEnd() - event.getRawX();
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                mMaxSlidePosition = mViewWidth;
                mViewWidth = mSliderView.getWidth();
                mStartPositionX = event.getRawX();
                mViewStartPositionX = mSliderView.getTranslationX();
                if (mTouchableArea < touchedArea){
                    mCanSlide = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawX() - mStartPositionX;
                float moveTo = mViewStartPositionX + difference;
                float percents = moveTo * 100 / -mSliderView.getWidth();

                if (moveTo < 0 && mCanSlide){
                    notifyPercentChanged(percents);
                    mSliderView.setTranslationX(moveTo);
                }
                if (event.getRawX() < mMaxSlidePosition) {
                    mMaxSlidePosition = event.getRawX();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = -mSliderView.getTranslationX();
                if (slideAnimationFrom == mViewStartPositionX) return false;
                boolean mustShow = mMaxSlidePosition < event.getRawX();
                boolean scrollableAreaConsumed = mSliderView.getTranslationX() < -mSliderView.getHeight() / 5;

                if (scrollableAreaConsumed && !mustShow){
                    mSlideAnimationTo = mSliderView.getWidth();
                }else {
                    mSlideAnimationTo = 0;
                }
                mValueAnimator.setFloatValues(slideAnimationFrom, mSlideAnimationTo);
                mValueAnimator.start();
                mCanSlide = true;
                mMaxSlidePosition = 0;
                break;
        }
        return true;
    }

    private boolean onTouchDownToUp(MotionEvent event){
        float touchedArea = event.getRawY() - mSliderView.getTop();
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                mViewHeight = mSliderView.getHeight();
                mStartPositionY = event.getRawY();
                mViewStartPositionY = mSliderView.getTranslationY();
                if (mTouchableArea < touchedArea){
                    mCanSlide = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawY() - mStartPositionY;
                float moveTo = mViewStartPositionY + difference;
                float percents = moveTo * 100 / mSliderView.getHeight();
                
                if (moveTo > 0 && mCanSlide){
                    notifyPercentChanged(percents);
                    mSliderView.setTranslationY(moveTo);
                }
                if (event.getRawY() > mMaxSlidePosition) {
                    mMaxSlidePosition = event.getRawY();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = mSliderView.getTranslationY();
                if (slideAnimationFrom == mViewStartPositionY) return false;
                boolean mustShow = mMaxSlidePosition > event.getRawY();
                boolean scrollableAreaConsumed = mSliderView.getTranslationY() > mSliderView.getHeight() / 5;

                if (scrollableAreaConsumed && !mustShow){
                    mSlideAnimationTo = mSliderView.getHeight();
                }else {
                    mSlideAnimationTo = 0;
                }
                mValueAnimator.setFloatValues(slideAnimationFrom, mSlideAnimationTo);
                mValueAnimator.start();
                mCanSlide = true;
                mMaxSlidePosition = 0;
                break;
        }
        return true;
    }
    
    private boolean onTouchUpToDown(MotionEvent event){
        float touchedArea = event.getRawY() - mSliderView.getBottom();
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                mViewHeight = mSliderView.getHeight();
                mStartPositionY = event.getRawY();
                mViewStartPositionY = mSliderView.getTranslationY();
                mMaxSlidePosition = mViewHeight;
                if (mTouchableArea < touchedArea){
                    mCanSlide = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float difference = event.getRawY() - mStartPositionY;
                float moveTo = mViewStartPositionY + difference;
                float percents = moveTo * 100 / -mSliderView.getHeight();
                
                if (moveTo < 0 && mCanSlide){
                    notifyPercentChanged(percents);
                    mSliderView.setTranslationY(moveTo);
                }
                if (event.getRawY() < mMaxSlidePosition) {
                    mMaxSlidePosition = event.getRawY();
                }
                break;
            case MotionEvent.ACTION_UP:
                float slideAnimationFrom = -mSliderView.getTranslationY();
                if (slideAnimationFrom == mViewStartPositionY) return false;
                boolean mustShow = mMaxSlidePosition < event.getRawY();
                boolean scrollableAreaConsumed = mSliderView.getTranslationY() < -mSliderView.getHeight() / 5;

                if (scrollableAreaConsumed && !mustShow){
                    mSlideAnimationTo = mSliderView.getHeight() + mSliderView.getTop();
                }else {
                    mSlideAnimationTo = 0;
                }
                mValueAnimator.setFloatValues(slideAnimationFrom, mSlideAnimationTo);
                mValueAnimator.start();
                mCanSlide = true;
                mMaxSlidePosition = 0;
                break;
        }
        return true;
    }

    @Override
    public final void onAnimationUpdate(ValueAnimator animation) {
        float value = (float) animation.getAnimatedValue();
        switch (mStartGravity){
            case TOP:    onAnimationUpdateUpToDown(value);   break;
            case BOTTOM: onAnimationUpdateDownToUp(value);   break;
            case START:  onAnimationUpdateStartToEnd(value); break;
            case END:    onAnimationUpdateEndToStart(value); break;
        }
    }

    private void onAnimationUpdateUpToDown(float value){
        mSliderView.setTranslationY(-value);
        float visibleDistance = mSliderView.getTop() - mSliderView.getY();
        float percents = (visibleDistance) * 100 / mViewHeight;
        notifyPercentChanged(percents);
    }

    private void onAnimationUpdateDownToUp(float value){
        mSliderView.setTranslationY(value);
        float visibleDistance = mSliderView.getY() - mSliderView.getTop();
        float percents = (visibleDistance) * 100 / mViewHeight;
        notifyPercentChanged(percents);
    }

    private void onAnimationUpdateStartToEnd(float value){
        mSliderView.setTranslationX(-value);
        float visibleDistance = mSliderView.getX() - getStart();
        float percents = (visibleDistance) * 100 / -mViewWidth;
        notifyPercentChanged(percents);
    }

    private void onAnimationUpdateEndToStart(float value){
        mSliderView.setTranslationX(value);
        float visibleDistance = mSliderView.getX() - getStart();
        float percents = (visibleDistance) * 100 / mViewWidth;
        notifyPercentChanged(percents);
    }

    private int getStart(){
        if (mIsRTL){
            return mSliderView.getRight();
        }else {
            return mSliderView.getLeft();
        }
    }

    private int getEnd(){
        if (mIsRTL){
            return mSliderView.getLeft();
        }else {
            return mSliderView.getRight();
        }
    }

    private void notifyPercentChanged(float percent){
        percent = percent > 100 ? 100 : percent;
        percent = percent < 0 ? 0 : percent;
        if (mSlideAnimationTo == 0 && mHideKeyboard)
            hideSoftInput();
        if (mListeners != null && !mListeners.isEmpty()){
            for (int i = 0; i < mListeners.size(); i++) {
                Listener l = mListeners.get(i);
                if (l != null){
                    if (l instanceof Listener.Slide) {
                        Listener.Slide slide = (Listener.Slide) l;
                        slide.onSlide(percent);
                        d("Listener(" + i + ")", "(onSlide)", "value = " + percent);
                    }
                }else {
                    e("Listener(" + i + ")", "(onSlide)", "Listener is null, skip notify for him...");
                }
            }
        }
    }

    private void notifyVisibilityChanged(int visibility){
        mSliderView.setVisibility(visibility);
        if (mListeners != null && !mListeners.isEmpty()){
            for (int i = 0; i < mListeners.size(); i++) {
                Listener l = mListeners.get(i);
                if (l != null) {
                    if (l instanceof Listener.Visibility) {
                        Listener.Visibility vis = (Listener.Visibility)l;
                        vis.onVisibilityChanged(visibility);
                        d("Listener(" + i + ")", "(onVisibilityChanged)", "value = " + (visibility == VISIBLE ? "VISIBLE" : visibility == GONE ? "GONE" : visibility));
                    }
                }else {
                    e("Listener(" + i + ")", "(onVisibilityChanged)", "Listener is null, skip notify for him...");
                }
            }
        }
        switch (visibility){
            case VISIBLE: mCurrentState = SHOWED; break;
            case GONE:    mCurrentState = HIDDEN; break;
        }
    }

    @Override
    public final void onAnimationStart(Animator animator) {
        if (mSliderView.getVisibility() != VISIBLE) {
            mSliderView.setVisibility(VISIBLE);
            notifyVisibilityChanged(VISIBLE);
        }
    }

    @Override
    public final void onAnimationEnd(Animator animator) {
        if (mSlideAnimationTo != 0){
            if (mSliderView.getVisibility() != GONE) {
                mSliderView.setVisibility(GONE);
                notifyVisibilityChanged(GONE);
            }
        }
    }

    @Override
    public final void onAnimationCancel(Animator animator) {}

    @Override
    public final void onAnimationRepeat(Animator animator) {}
    
    private void e(String listener, String method, String message){
        if (mDebug) {
            Log.e(TAG, String.format("%1$-15s %2$-23s %3$s", listener, method, message));
        }
    }
    
    private void d(String listener, String method, String value){
        if (mDebug) {
            Log.d(TAG, String.format("%1$-15s %2$-23s %3$s", listener, method, value));
        }
    }
}
