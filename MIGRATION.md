# v2.2.6 → ... 
 - `SlideUp.Builder` was moved into separated class and renamed to `SlideUpBuilder`
 #### SlideUp
 - method `void : setTouchebleArea(float touchableArea)` was split to two methods: 
    - `void : setTouchebleAreaDp(float touchableArea)`
    - `void : setTouchebleAreaPx(float touchableArea)`
 - method `float : getTouchebleArea()` was split to two methods:
    - `float : getTouchebleAreaDp()`
    - `float : getTouchebleAreaPx()`
 #### SlideUpBuilder
 - method `SlideUpBuilder : withTouchebleArea(float touchableArea)` was split to two methods: 
    - `SlideUpBuilder : withTouchebleAreaDp(float touchableArea)`
    - `SlideUpBuilder : withTouchebleAreaPx(float touchableArea)`
