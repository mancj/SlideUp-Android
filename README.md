# SlideUp-Android
SlideUp is a small library that allows you to add sweet slide effect to any view.

**Anyone with any ideas for SlideUp, create a pull request.**

[![Release](https://jitpack.io/v/mancj/SlideUp-Android.svg)](https://jitpack.io/#mancj/SlideUp-Android)

---

<img src="/art/art1.gif" width="300"> 
<img src="/art/art2.gif" width="300"> 

---
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
    compile 'com.github.mancj:SlideUp-Android:1.0-beta'
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
SlideUp slideUp = new SlideUp(slideView);
```
## Enjoy!
---
# More complicated example

```java
slideView = findViewById(R.id.slideView);
dim = findViewById(R.id.dim);
fab = (FloatingActionButton) findViewById(R.id.fab);

slideUp = new SlideUp(slideView);
slideUp.hideImmediately();

fab.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        slideUp.animateIn();
        fab.hide();
    }
});

slideUp.setSlideListener(new SlideUp.SlideListener() {
    @Override
    public void onSlide(float percent) {
        dim.setAlpha(1 - (percent / 100));
    }

    @Override
    public void onVisibilityChanged(int visibility) {
        if (visibility == View.GONE)
        {
            fab.show();
        }
    }
});
```
-----
The player is designed by [Jauzee](https://github.com/Jauzee)
