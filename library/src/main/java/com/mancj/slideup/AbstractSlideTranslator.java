package com.mancj.slideup;

public abstract class AbstractSlideTranslator {
  protected final SlideUpBuilder mBuilder;
  protected final AnimationProcessor mAnimationProcessor;

  public AbstractSlideTranslator(SlideUpBuilder builder, AnimationProcessor animationProcessor) {
    this.mBuilder = builder;
    this.mAnimationProcessor = animationProcessor;
  }

  public void setTranslationY(float translationY) {
    mBuilder.mSliderView.setTranslationY(translationY);
  }

  public void setTranslationX(float translationX) {
    mBuilder.mSliderView.setTranslationX(translationX);
  }

  public final void showSlideView(boolean immediately) {
    mAnimationProcessor.endAnimation();
    if (immediately) {
      immediatelyShowSlideView();
    } else {
      animateShowSlideView();
    }
  }

  public final void hideSlideView(boolean immediately) {
    mAnimationProcessor.endAnimation();
    if (immediately) {
      immediatelyHideSlideView();
    } else {
      animateHideSlideView();
    }
  }

  protected abstract void immediatelyShowSlideView();

  protected abstract void animateShowSlideView();

  protected abstract void immediatelyHideSlideView();

  protected abstract void animateHideSlideView();

}
