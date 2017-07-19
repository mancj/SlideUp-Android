## v2.2.7 (13.07.2017)
  - Added definition info for [AboutLibraries](https://github.com/mikepenz/AboutLibraries) (© MikePenz)
  
## v2.2.6 (13.07.2017)
  - Fixed problems with clicks onto `view` with `SlideUp`-effect
  - `SlideUp.Builder` was moved into separated class and renamed to `SlideUpBuilder`
  #### SlideUp
  - Method `setTouchebleArea(float touchableArea)` was split to two methods: 
     - `setTouchebleAreaDp(float touchableArea)`
     - `setTouchebleAreaPx(float touchableArea)`
  - Method `getTouchebleArea()` was split to two methods:
     - `getTouchebleAreaDp()`
     - `getTouchebleAreaPx()`
  #### SlideUpBuilder
  - Method `withTouchebleArea(float touchableArea)` was split to two methods: 
     - `withTouchebleAreaDp(float touchableArea)`
     - `withTouchebleAreaPx(float touchableArea)`
