# NOT MAINTAINED ANYMORE

# SlideUp-Android
SlideUp is a small library that allows you to add sweet slide effect to any view. Slide your views up, down, left or right with SlideUp!

[![Release](https://jitpack.io/v/mancj/SlideUp-Android.svg)](https://jitpack.io/#mancj/SlideUp-Android)
---

[Example gif 1](https://i.imgur.com/7S5qqSy.gifv)

[Example gif 2](https://i.imgur.com/hKWqyl1.gif)

-----
# Usage
**Get SlideUp library**

Add the JitPack repository to your build file.
Add it in your root build.gradle at the end of repositories:
```groovy
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```
Add the dependency
```groovy
dependencies {
    compile "com.github.mancj:SlideUp-Android:$current_version"
    compile 'ru.ztrap:RxSlideUp2:2.x.x' //optional, for reactive listeners based on RxJava-2
    compile 'ru.ztrap:RxSlideUp:1.x.x' //optional, for reactive listeners based on RxJava
}
```

**To add the SlideUp into your project, follow these three simple steps:**

### Step 1:
create any type of layout

```xml
<LinearLayout
  android:id="@+id/slideView"
  android:layout_width="match_parent"
  android:layout_height="match_parent"/>
```

### Step 2:
Find that view in your activity/fragment
```java
View slideView = findViewById(R.id.slideView);
```

### Step 3:
Create a SlideUp object and pass in your view
```java
slideUp = new SlideUpBuilder(slideView)
                .withStartState(SlideUp.State.HIDDEN)
                .withStartGravity(Gravity.BOTTOM)

                //.withSlideFromOtherView(anotherView)
                //.withGesturesEnabled()
                //.withHideSoftInputWhenDisplayed()
                //.withInterpolator()
                //.withAutoSlideDuration()
                //.withLoggingEnabled()
                //.withTouchableAreaPx()
                //.withTouchableAreaDp()
                //.withListeners()
                //.withSavedState()
                .build();
```
### Enjoy!

# Reactive extensions

 - [RxSlideUp](https://github.com/zTrap/RxSlideUp) - Listening events in reactive style

# Advanced example
[SlideUpViewActivity.java](https://github.com/mancj/SlideUp-Android/blob/master/app/src/main/java/com/example/slideup/SlideUpViewActivity.java)
```java
rootView = findViewById(R.id.rootView);
slideView = findViewById(R.id.slideView);
dim = findViewById(R.id.dim);
fab = (FloatingActionButton) findViewById(R.id.fab);


slideUp = new SlideUpBuilder(slideView)
         .withListeners(new SlideUp.Listener.Events() {
             @Override
             public void onSlide(float percent) {
                 dim.setAlpha(1 - (percent / 100));
                 if (percent < 100 && fab.isShown()) {
                    // slideUp started showing
                    fab.hide();
                 }
             }

             @Override
             public void onVisibilityChanged(int visibility) {
                 if (visibility == View.GONE){
                     fab.show();
                 }
             }
         })
         .withStartGravity(Gravity.TOP)
         .withLoggingEnabled(true)
         .withStartState(SlideUp.State.HIDDEN)
         .withSlideFromOtherView(rootView)
         .build();

fab.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        slideUp.show();
    }
});
```
The player is designed by [Jauzee](https://github.com/Jauzee)
 
# Migration
 
 - See [MIGRATION.md](https://github.com/mancj/SlideUp-Android/blob/master/MIGRATION.md)
 
# Documentation
 
 - See [javadocs](https://jitpack.io/com/github/mancj/SlideUp-Android/2.2.7.1/javadoc/)
 
# Changelog

 - See [CHANGELOG.md](https://github.com/mancj/SlideUp-Android/blob/master/CHANGELOG.md)

# Contract

Please let us know, if you use the library in your applications. 
We want to collect and publish this list.

# License

    MIT License

    Copyright (c) 2018 Mansur

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
