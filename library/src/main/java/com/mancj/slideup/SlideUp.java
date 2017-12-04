package com.mancj.slideup;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.END;
import static android.view.Gravity.START;
import static android.view.Gravity.TOP;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mancj.slideup.SlideUp.State.HIDDEN;
import static com.mancj.slideup.SlideUp.State.SHOWED;

public class SlideUp implements View.OnTouchListener, ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener, LoggerNotifier {
    private final static String TAG = SlideUp.class.getSimpleName();
    
    final static String KEY_START_GRAVITY = TAG + "_start_gravity";
    final static String KEY_DEBUG = TAG + "_debug";
    final static String KEY_TOUCHABLE_AREA = TAG + "_touchable_area";
    final static String KEY_STATE = TAG + "_state";
    final static String KEY_AUTO_SLIDE_DURATION = TAG + "_auto_slide_duration";
    final static String KEY_HIDE_SOFT_INPUT = TAG + "_hide_soft_input";
    final static String KEY_STATE_SAVED = TAG + "_state_saved";
    
    /**
     * <p>Available start states</p>
     */
    public enum State {
        
        /**
         * State hidden is equal {@link View#GONE}
         */
        HIDDEN,
        
        /**
         * State showed is equal {@link View#VISIBLE}
         */
        SHOWED
    }
    
    @IntDef(value = {START, END, TOP, BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    @interface StartVector {
    }
    
    private State mCurrentState;
    
    private float mViewHeight;
    private float mViewWidth;
    
    private SlideUpBuilder mBuilder;
    
    private VerticalTouchConsumer mVerticalTouchConsumer;
    private HorizontalTouchConsumer mHorizontalTouchConsumer;
    
    private AnimationProcessor mAnimationProcessor;
    
    /**
     * <p>Interface to listen to all handled events taking place in the slider</p>
     */
    public interface Listener {
        
        interface Slide extends Listener {
            
            /**
             * @param percent percents of complete slide <b color="#EF6C00">(100 = HIDDEN, 0 = SHOWED)</b>
             */
            void onSlide(float percent);
        }
        
        interface Visibility extends Listener {
            
            /**
             * @param visibility (<b>GONE</b> or <b>VISIBLE</b>)
             */
            void onVisibilityChanged(int visibility);
        }
        
        interface Events extends Visibility, Slide {
        }
    }
    
    SlideUp(SlideUpBuilder builder) {
        mBuilder = builder;
        init();
    }
    
    private void init() {
        mBuilder.mSliderView.setOnTouchListener(this);
        if(mBuilder.mAlsoScrollView != null) {
            mBuilder.mAlsoScrollView.setOnTouchListener(this);
        }
        createAnimation();
        mBuilder.mSliderView.getViewTreeObserver().addOnGlobalLayoutListener(
                new OnGlobalLayoutSingleListener(mBuilder.mSliderView, new Runnable() {
                    @Override
                    public void run() {
                        
                        mViewHeight = mBuilder.mSliderView.getHeight();
                        mViewWidth = mBuilder.mSliderView.getWidth();
                        switch (mBuilder.mStartGravity) {
                            case TOP:
                                mBuilder.mSliderView.setPivotY(mViewHeight);
                                setTouchableAreaVertical();
                                break;
                            case BOTTOM:
                                mBuilder.mSliderView.setPivotY(0);
                                setTouchableAreaVertical();
                                break;
                            case START:
                                mBuilder.mSliderView.setPivotX(0);
                                setTouchableAreaHorizontal();
                                break;
                            case END:
                                mBuilder.mSliderView.setPivotX(mViewWidth);
                                setTouchableAreaHorizontal();
                                break;
                        }
                        createConsumers();
                        updateToCurrentState();
                    }
                }));
        updateToCurrentState();
    }
    
    private void setTouchableAreaHorizontal(){
        if (mBuilder.mTouchableArea == 0) {
            mBuilder.mTouchableArea = (float) Math.ceil(mViewWidth / 10);
        }
    }
    
    private void setTouchableAreaVertical(){
        if (mBuilder.mTouchableArea == 0) {
            mBuilder.mTouchableArea = (float) Math.ceil(mViewHeight / 10);
        }
    }
    
    private void createAnimation() {
        mAnimationProcessor = new AnimationProcessor(mBuilder, this, this);
    }
    
    private void createConsumers() {
        createAnimation();
        mVerticalTouchConsumer = new VerticalTouchConsumer(mBuilder, this, mAnimationProcessor);
        mHorizontalTouchConsumer = new HorizontalTouchConsumer(mBuilder, this, mAnimationProcessor);
    }
    
    private void updateToCurrentState() {
        switch (mBuilder.mStartState) {
            case HIDDEN:
                hideImmediately();
                break;
            case SHOWED:
                showImmediately();
                break;
        }
    }
    
    //region public interface
    /**
     * <p>Trying hide soft input from window</p>
     *
     * @see InputMethodManager#hideSoftInputFromWindow(IBinder, int)
     */
    public void hideSoftInput() {
        ((InputMethodManager) mBuilder.mSliderView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(mBuilder.mSliderView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
    
    /**
     * <p>Trying show soft input to window</p>
     *
     * @see InputMethodManager#showSoftInput(View, int)
     */
    public void showSoftInput() {
        ((InputMethodManager) mBuilder.mSliderView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(mBuilder.mSliderView, 0);
    }
    
    /**
     * <p>Returns the visibility status for this view.</p>
     *
     * @return true if view have status {@link View#VISIBLE}
     */
    public boolean isVisible() {
        return mBuilder.mSliderView.getVisibility() == VISIBLE;
    }
    
    /**
     * <p>Add Listener which will be used in combination with this SlideUp</p>
     */
    public void addSlideListener(@NonNull Listener listener) {
        mBuilder.mListeners.add(listener);
    }
    
    /**
     * <p>Remove Listener which was used in combination with this SlideUp</p>
     */
    public void removeSlideListener(@NonNull Listener listener) {
        mBuilder.mListeners.remove(listener);
    }
    
    /**
     * <p>Returns typed view which was used as slider</p>
     */
    public <T extends View> T getSliderView() {
        return (T) mBuilder.mSliderView;
    }
    
    /**
     * <p>Set duration of animation (whenever you use {@link #hide()} or {@link #show()} methods)</p>
     *
     * @param autoSlideDuration <b>(default - <b color="#EF6C00">300</b>)</b>
     */
    public void setAutoSlideDuration(int autoSlideDuration) {
        mBuilder.withAutoSlideDuration(autoSlideDuration);
        mAnimationProcessor.paramsChanged();
    }
    
    /**
     * <p>Returns duration of animation (whenever you use {@link #hide()} or {@link #show()} methods)</p>
     */
    public float getAutoSlideDuration() {
        return mBuilder.mAutoSlideDuration;
    }
    
    /**
     * <p>Set touchable area <b>(in dp)</b> for interaction</p>
     *
     * @param touchableArea <b>(default - <b color="#EF6C00">300dp</b>)</b>
     */
    public void setTouchableAreaDp(float touchableArea) {
        mBuilder.withTouchableAreaDp(touchableArea);
    }
    
    /**
     * <p>Set touchable area <b>(in px)</b> for interaction</p>
     *
     * @param touchableArea <b>(default - <b color="#EF6C00">300dp</b>)</b>
     */
    public void setTouchableAreaPx(float touchableArea) {
        mBuilder.withTouchableAreaPx(touchableArea);
    }
    
    /**
     * <p>Returns touchable area <b>(in dp)</b> for interaction</p>
     */
    public float getTouchableAreaDp() {
        return mBuilder.mTouchableArea / mBuilder.mDensity;
    }
    
    /**
     * <p>Returns touchable area <b>(in px)</b> for interaction</p>
     */
    public float getTouchableAreaPx() {
        return mBuilder.mTouchableArea;
    }
    
    /**
     * <p>Returns running status of animation</p>
     *
     * @return true if animation is running
     */
    public boolean isAnimationRunning() {
        return mAnimationProcessor.isAnimationRunning();
    }
    
    /**
     * <p>Show view with animation</p>
     */
    public void show() {
        show(false);
    }
    
    /**
     * <p>Hide view with animation</p>
     */
    public void hide() {
        hide(false);
    }
    
    /**
     * <p>Hide view without animation</p>
     */
    public void hideImmediately() {
        hide(true);
    }
    
    /**
     * <p>Show view without animation</p>
     */
    public void showImmediately() {
        show(true);
    }
    
    /**
     * <p>Turning on/off debug logging</p>
     *
     * @param enabled <b>(default - <b color="#EF6C00">false</b>)</b>
     */
    public void setLoggingEnabled(boolean enabled) {
        mBuilder.withLoggingEnabled(enabled);
    }
    
    /**
     * <p>Returns current status of debug logging</p>
     */
    public boolean isLoggingEnabled() {
        return mBuilder.mDebug;
    }
    
    /**
     * <p>Turning on/off gestures</p>
     *
     * @param enabled <b>(default - <b color="#EF6C00">true</b>)</b>
     */
    public void setGesturesEnabled(boolean enabled) {
        mBuilder.withGesturesEnabled(enabled);
    }
    
    /**
     * <p>Returns current status of gestures</p>
     */
    public boolean isGesturesEnabled() {
        return mBuilder.mGesturesEnabled;
    }
    
    /**
     * <p>Returns current interpolator</p>
     */
    public TimeInterpolator getInterpolator() {
        return mBuilder.mInterpolator;
    }
    
    /**
     * <p>Returns gravity which used in combination with this SlideUp</p>
     */
    @StartVector
    public int getStartGravity() {
        return mBuilder.mStartGravity;
    }
    
    /**
     * <p>Sets interpolator for animation (whenever you use {@link #hide()} or {@link #show()} methods)</p>
     *
     * @param interpolator <b>(default - <b color="#EF6C00">Decelerate interpolator</b>)</b>
     */
    public void setInterpolator(TimeInterpolator interpolator) {
        mBuilder.withInterpolator(interpolator);
        mAnimationProcessor.paramsChanged();
    }
    
    /**
     * <p>Returns current behavior of soft input</p>
     */
    public boolean isHideKeyboardWhenDisplayed() {
        return mBuilder.mHideKeyboard;
    }
    
    /**
     * <p>Sets behavior of soft input</p>
     *
     * @param hide <b>(default - <b color="#EF6C00">false</b>)</b>
     */
    public void setHideKeyboardWhenDisplayed(boolean hide) {
        mBuilder.withHideSoftInputWhenDisplayed(hide);
    }
    
    /**
     * <p>Toggle current state with animation</p>
     */
    public void toggle() {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }
    
    /**
     * <p>Toggle current state without animation</p>
     */
    public void toggleImmediately() {
        if (isVisible()) {
            hideImmediately();
        } else {
            showImmediately();
        }
    }
    
    /**
     * <p>Saving current parameters of SlideUp</p>
     */
    public void onSaveInstanceState(@NonNull Bundle savedState) {
        savedState.putBoolean(KEY_STATE_SAVED, true);
        savedState.putInt(KEY_START_GRAVITY, mBuilder.mStartGravity);
        savedState.putBoolean(KEY_DEBUG, mBuilder.mDebug);
        savedState.putFloat(KEY_TOUCHABLE_AREA, mBuilder.mTouchableArea / mBuilder.mDensity);
        savedState.putSerializable(KEY_STATE, mCurrentState);
        savedState.putInt(KEY_AUTO_SLIDE_DURATION, mBuilder.mAutoSlideDuration);
        savedState.putBoolean(KEY_HIDE_SOFT_INPUT, mBuilder.mHideKeyboard);
    }
    //endregion
    
    private void hide(boolean immediately) {
        mAnimationProcessor.endAnimation();
        switch (mBuilder.mStartGravity) {
            case TOP:
                if (immediately) {
                    if (mBuilder.mSliderView.getHeight() > 0) {
                        mBuilder.mSliderView.setTranslationY(-mViewHeight);
                        notifyPercentChanged(100);
                    } else {
                        mBuilder.mStartState = HIDDEN;
                    }
                } else {
                    mAnimationProcessor.setValuesAndStart(mBuilder.mSliderView.getTranslationY(), mBuilder.mSliderView.getHeight());
                }
                break;
            case BOTTOM:
                if (immediately) {
                    if (mBuilder.mSliderView.getHeight() > 0) {
                        mBuilder.mSliderView.setTranslationY(mViewHeight);
                        notifyPercentChanged(100);
                    } else {
                        mBuilder.mStartState = HIDDEN;
                    }
                } else {
                    mAnimationProcessor.setValuesAndStart(mBuilder.mSliderView.getTranslationY(), mBuilder.mSliderView.getHeight());
                }
                break;
            case START:
                if (immediately) {
                    if (mBuilder.mSliderView.getWidth() > 0) {
                        mBuilder.mSliderView.setTranslationX(-mViewWidth);
                        notifyPercentChanged(100);
                    } else {
                        mBuilder.mStartState = HIDDEN;
                    }
                } else {
                    mAnimationProcessor.setValuesAndStart(mBuilder.mSliderView.getTranslationX(), mBuilder.mSliderView.getHeight());
                }
                break;
            case END:
                if (immediately) {
                    if (mBuilder.mSliderView.getWidth() > 0) {
                        mBuilder.mSliderView.setTranslationX(mViewWidth);
                        notifyPercentChanged(100);
                    } else {
                        mBuilder.mStartState = HIDDEN;
                    }
                } else {
                    mAnimationProcessor.setValuesAndStart(mBuilder.mSliderView.getTranslationX(), mBuilder.mSliderView.getHeight());
                }
                break;
        }
    }
    
    private void show(boolean immediately) {
        mAnimationProcessor.endAnimation();
        switch (mBuilder.mStartGravity) {
            case TOP:
                if (immediately) {
                    if (mBuilder.mSliderView.getHeight() > 0) {
                        mBuilder.mSliderView.setTranslationY(0);
                        notifyPercentChanged(0);
                    } else {
                        mBuilder.mStartState = SHOWED;
                    }
                } else {
                    mAnimationProcessor.setValuesAndStart(mBuilder.mSliderView.getTranslationY(), 0);
                }
            case BOTTOM:
                if (immediately) {
                    if (mBuilder.mSliderView.getHeight() > 0) {
                        mBuilder.mSliderView.setTranslationY(0);
                        notifyPercentChanged(0);
                    } else {
                        mBuilder.mStartState = SHOWED;
                    }
                } else {
                    mAnimationProcessor.setValuesAndStart(mBuilder.mSliderView.getTranslationY(), 0);
                }
                break;
            case START:
                if (immediately) {
                    if (mBuilder.mSliderView.getWidth() > 0) {
                        mBuilder.mSliderView.setTranslationX(0);
                        notifyPercentChanged(0);
                    } else {
                        mBuilder.mStartState = SHOWED;
                    }
                } else {
                    mAnimationProcessor.setValuesAndStart(mBuilder.mSliderView.getTranslationX(), 0);
                }
            case END:
                if (immediately) {
                    if (mBuilder.mSliderView.getWidth() > 0) {
                        mBuilder.mSliderView.setTranslationX(0);
                        notifyPercentChanged(0);
                    } else {
                        mBuilder.mStartState = SHOWED;
                    }
                } else {
                    mAnimationProcessor.setValuesAndStart(mBuilder.mSliderView.getTranslationX(), 0);
                }
                break;
        }
    }
    
    @Override
    public final boolean onTouch(View v, MotionEvent event) {
        if (mAnimationProcessor.isAnimationRunning()) return false;
        if (!mBuilder.mGesturesEnabled){
            mBuilder.mSliderView.performClick();
            return true;
        }
        boolean consumed;
        switch (mBuilder.mStartGravity) {
            case TOP:
                consumed = mVerticalTouchConsumer.consumeTopToBottom(v, event);
                break;
            case BOTTOM:
                consumed = mVerticalTouchConsumer.consumeBottomToTop(v, event);
                break;
            case START:
                consumed = mHorizontalTouchConsumer.consumeStartToEnd(v, event);
                break;
            case END:
                consumed = mHorizontalTouchConsumer.consumeEndToStart(v, event);
                break;
            default:
                throw new IllegalArgumentException("You are using not supported gravity");
        }
        if (!consumed){
            mBuilder.mSliderView.performClick();
        }
        return true;
    }
    
    @Override
    public final void onAnimationUpdate(ValueAnimator animation) {
        float value = (float) animation.getAnimatedValue();
        switch (mBuilder.mStartGravity) {
            case TOP:
                onAnimationUpdateTopToBottom(value);
                break;
            case BOTTOM:
                onAnimationUpdateBottomToTop(value);
                break;
            case START:
                onAnimationUpdateStartToEnd(value);
                break;
            case END:
                onAnimationUpdateEndToStart(value);
                break;
        }
    }
    
    private void onAnimationUpdateTopToBottom(float value) {
        mBuilder.mSliderView.setTranslationY(-value);
        float visibleDistance = mBuilder.mSliderView.getTop() - mBuilder.mSliderView.getY();
        float percents = (visibleDistance) * 100 / mViewHeight;
        notifyPercentChanged(percents);
    }
    
    private void onAnimationUpdateBottomToTop(float value) {
        mBuilder.mSliderView.setTranslationY(value);
        float visibleDistance = mBuilder.mSliderView.getY() - mBuilder.mSliderView.getTop();
        float percents = (visibleDistance) * 100 / mViewHeight;
        notifyPercentChanged(percents);
    }
    
    private void onAnimationUpdateStartToEnd(float value) {
        mBuilder.mSliderView.setTranslationX(-value);
        float visibleDistance = mBuilder.mSliderView.getX() - getStart();
        float percents = (visibleDistance) * 100 / -mViewWidth;
        notifyPercentChanged(percents);
    }
    
    private void onAnimationUpdateEndToStart(float value) {
        mBuilder.mSliderView.setTranslationX(value);
        float visibleDistance = mBuilder.mSliderView.getX() - getStart();
        float percents = (visibleDistance) * 100 / mViewWidth;
        notifyPercentChanged(percents);
    }
    
    private int getStart() {
        if (mBuilder.mIsRTL) {
            return mBuilder.mSliderView.getRight();
        } else {
            return mBuilder.mSliderView.getLeft();
        }
    }
    
    @Override
    public void notifyPercentChanged(float percent) {
        percent = percent > 100 ? 100 : percent;
        percent = percent < 0 ? 0 : percent;
        if (percent == 100) {
            mBuilder.mSliderView.setVisibility(GONE);
            notifyVisibilityChanged(GONE);
        } else {
            mBuilder.mSliderView.setVisibility(VISIBLE);
            if (percent == 0) {
                notifyVisibilityChanged(VISIBLE);
            }
        }
        if (mAnimationProcessor.getSlideAnimationTo() == 0 && mBuilder.mHideKeyboard)
            hideSoftInput();
        if (!mBuilder.mListeners.isEmpty()) {
            for (int i = 0; i < mBuilder.mListeners.size(); i++) {
                Listener l = mBuilder.mListeners.get(i);
                if (l != null) {
                    if (l instanceof Listener.Slide) {
                        Listener.Slide slide = (Listener.Slide) l;
                        slide.onSlide(percent);
                        logValue(i, "onSlide", percent);
                    }
                } else {
                    logError(i, "onSlide");
                }
            }
        }
    }
    
    @Override
    public void notifyVisibilityChanged(int visibility) {
        if (!mBuilder.mListeners.isEmpty()) {
            for (int i = 0; i < mBuilder.mListeners.size(); i++) {
                Listener l = mBuilder.mListeners.get(i);
                if (l != null) {
                    if (l instanceof Listener.Visibility) {
                        Listener.Visibility vis = (Listener.Visibility) l;
                        vis.onVisibilityChanged(visibility);
                        logValue(i, "onVisibilityChanged", visibility == VISIBLE ? "VISIBLE" : visibility == GONE ? "GONE" : visibility);
                    }
                } else {
                    logError(i, "onVisibilityChanged");
                }
            }
        }
        switch (visibility) {
            case VISIBLE:
                mCurrentState = SHOWED;
                break;
            case GONE:
                mCurrentState = HIDDEN;
                break;
        }
    }
    
    @Override
    public final void onAnimationStart(Animator animator) {
    }
    
    @Override
    public final void onAnimationEnd(Animator animator) {
    }
    
    @Override
    public final void onAnimationCancel(Animator animator) {
    }
    
    @Override
    public final void onAnimationRepeat(Animator animator) {
    }
    
    private void logValue(int listener, String method, Object message) {
        if (mBuilder.mDebug) {
            Log.e(TAG, String.format("Listener(%1s) (%2$-23s) value = %3$s", listener, method, message));
        }
    }
    
    private void logError(int listener, String method) {
        if (mBuilder.mDebug) {
            Log.d(TAG, String.format("Listener(%1s) (%2$-23s) Listener is null, skip notification...", listener, method));
        }
    }
}
