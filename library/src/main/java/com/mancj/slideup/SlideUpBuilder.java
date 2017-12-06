package com.mancj.slideup;

import android.animation.TimeInterpolator;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Default constructor for {@link SlideUp}</p>
 */
public final class SlideUpBuilder {
    private boolean mStateRestored = false;
    
    View mSliderView;
    float mDensity;
    float mTouchableArea;
    boolean mIsRTL;
    SlideUp.State mStartState = SlideUp.State.HIDDEN;
    List<SlideUp.Listener> mListeners = new ArrayList<>();
    boolean mDebug = false;
    int mAutoSlideDuration = 300;
    int mStartGravity = Gravity.BOTTOM;
    boolean mGesturesEnabled = true;
    boolean mHideKeyboard = false;
    TimeInterpolator mInterpolator = new DecelerateInterpolator();
    View mAlsoScrollView;

    /**
     * <p>Construct a SlideUp by passing the view or his child to use for the generation</p>
     */
    public SlideUpBuilder(View sliderView) {
        Internal.checkNonNull(sliderView, "View can't be null");
        mSliderView = sliderView;
        mDensity = sliderView.getResources().getDisplayMetrics().density;
        mIsRTL = sliderView.getResources().getBoolean(R.bool.is_right_to_left);
    }
    
    /**
     * <p>Define a start state on screen</p>
     *
     * @param startState <b>(default - <b color="#EF6C00">{@link SlideUp.State#HIDDEN}</b>)</b>
     */
    public SlideUpBuilder withStartState(@NonNull SlideUp.State startState) {
        if (!mStateRestored) {
            mStartState = startState;
        }
        return this;
    }
    
    /**
     * <p>Define a start gravity, <b>this parameter affects the motion vector slider</b></p>
     *
     * @param gravity <b>(default - <b color="#EF6C00">{@link android.view.Gravity#BOTTOM}</b>)</b>
     */
    public SlideUpBuilder withStartGravity(@SlideUp.StartVector int gravity) {
        if (!mStateRestored) {
            mStartGravity = gravity;
        }
        return this;
    }
    
    /**
     * <p>Define a {@link SlideUp.Listener} for this SlideUp</p>
     *
     * @param listeners {@link List} of listeners
     */
    public SlideUpBuilder withListeners(@NonNull List<SlideUp.Listener> listeners) {
        if (listeners != null) {
            mListeners.addAll(listeners);
        }
        return this;
    }
    
    /**
     * <p>Define a {@link SlideUp.Listener} for this SlideUp</p>
     *
     * @param listeners array of listeners
     */
    public SlideUpBuilder withListeners(@NonNull SlideUp.Listener... listeners) {
        List<SlideUp.Listener> listeners_list = new ArrayList<>();
        Collections.addAll(listeners_list, listeners);
        return withListeners(listeners_list);
    }
    
    /**
     * <p>Turning on/off debug logging for all handled events</p>
     *
     * @param enabled <b>(default - <b color="#EF6C00">false</b>)</b>
     */
    public SlideUpBuilder withLoggingEnabled(boolean enabled) {
        if (!mStateRestored) {
            mDebug = enabled;
        }
        return this;
    }
    
    /**
     * <p>Define duration of animation (whenever you use {@link SlideUp#hide()} or {@link SlideUp#show()} methods)</p>
     *
     * @param duration <b>(default - <b color="#EF6C00">300</b>)</b>
     */
    public SlideUpBuilder withAutoSlideDuration(int duration) {
        if (!mStateRestored) {
            mAutoSlideDuration = duration;
        }
        return this;
    }
    
    /**
     * <p>Define touchable area <b>(in px)</b> for interaction</p>
     *
     * @param area <b>(default - <b color="#EF6C00">300dp</b>)</b>
     */
    public SlideUpBuilder withTouchableAreaPx(float area) {
        if (!mStateRestored) {
            mTouchableArea = area;
        }
        return this;
    }
    
    /**
     * <p>Define touchable area <b>(in dp)</b> for interaction</p>
     *
     * @param area <b>(default - <b color="#EF6C00">300dp</b>)</b>
     */
    public SlideUpBuilder withTouchableAreaDp(float area) {
        if (!mStateRestored) {
            mTouchableArea = area * mDensity;
        }
        return this;
    }
    
    /**
     * <p>Turning on/off sliding on touch event</p>
     *
     * @param enabled <b>(default - <b color="#EF6C00">true</b>)</b>
     */
    public SlideUpBuilder withGesturesEnabled(boolean enabled) {
        mGesturesEnabled = enabled;
        return this;
    }
    
    /**
     * <p>Define behavior of soft input</p>
     *
     * @param hide <b>(default - <b color="#EF6C00">false</b>)</b>
     */
    public SlideUpBuilder withHideSoftInputWhenDisplayed(boolean hide) {
        if (!mStateRestored) {
            mHideKeyboard = hide;
        }
        return this;
    }
    
    /**
     * <p>Define interpolator for animation (whenever you use {@link SlideUp#hide()} or {@link SlideUp#show()} methods)</p>
     *
     * @param interpolator <b>(default - <b color="#EF6C00">Decelerate interpolator</b>)</b>
     */
    public SlideUpBuilder withInterpolator(TimeInterpolator interpolator) {
        mInterpolator = interpolator;
        return this;
    }
    
    /**
     * @param savedState parameters will be restored from this bundle, if it contains them
     */
    public SlideUpBuilder withSavedState(@Nullable Bundle savedState) {
        restoreParams(savedState);
        return this;
    }


    /**
     * <p>Provide a {@link View} that will also trigger slide events on the {@link SlideUp}.</p>
     *
     * @param alsoScrollView the other view that will trigger the slide events
     */
    public SlideUpBuilder withSlideFromOtherView(@Nullable View alsoScrollView) {
        mAlsoScrollView = alsoScrollView;
        return this;
    }
    
    /**
     * <p>Build the SlideUp and add behavior to view</p>
     */
    public SlideUp build() {
        return new SlideUp(this);
    }
    
    /**
     * <p>Trying restore saved state</p>
     */
    private void restoreParams(@Nullable Bundle savedState) {
        if (savedState == null) return;
        mStateRestored = savedState.getBoolean(SlideUp.KEY_STATE_SAVED, false);
        if (savedState.getSerializable(SlideUp.KEY_STATE) != null) {
            mStartState = (SlideUp.State) savedState.getSerializable(SlideUp.KEY_STATE);
        }
        mStartGravity = savedState.getInt(SlideUp.KEY_START_GRAVITY, mStartGravity);
        mDebug = savedState.getBoolean(SlideUp.KEY_DEBUG, mDebug);
        mTouchableArea = savedState.getFloat(SlideUp.KEY_TOUCHABLE_AREA, mTouchableArea) * mDensity;
        mAutoSlideDuration = savedState.getInt(SlideUp.KEY_AUTO_SLIDE_DURATION, mAutoSlideDuration);
        mHideKeyboard = savedState.getBoolean(SlideUp.KEY_HIDE_SOFT_INPUT, mHideKeyboard);
    }
}