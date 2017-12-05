package com.mancj.slideup;

import static com.mancj.slideup.SlideUp.State.HIDDEN;
import static com.mancj.slideup.SlideUp.State.SHOWED;

public class EndToStartSlideTranslator extends AbstractSlideTranslator {
  public EndToStartSlideTranslator(SlideUpBuilder builder, AnimationProcessor animationProcessor) {
    super(builder, animationProcessor);
  }

  @Override
  protected void immediatelyShowSlideView() {
    if (mBuilder.mSliderView.getWidth() > 0) {
      mBuilder.mSliderView.setTranslationX(0);
    } else {
      mBuilder.mStartState = SHOWED;
    }
  }

  @Override
  protected void animateShowSlideView() {
    mAnimationProcessor.setValuesAndStart(mBuilder.mSliderView.getTranslationX(), 0);
  }

  @Override
  protected void immediatelyHideSlideView() {
    if (mBuilder.mSliderView.getWidth() > 0) {
      mBuilder.mSliderView.setTranslationX(mBuilder.mSliderView.getWidth());
    } else {
      mBuilder.mStartState = HIDDEN;
    }
  }

  @Override
  protected void animateHideSlideView() {
    mAnimationProcessor.setValuesAndStart(mBuilder.mSliderView.getTranslationX(), mBuilder.mSliderView.getWidth());
  }
}
