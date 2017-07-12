package com.mancj.slideup;

/**
 * @author pa.gulko zTrap (05.07.2017)
 */
interface LoggerNotifier {
    
    void notifyPercentChanged(float percent);
    
    void notifyVisibilityChanged(int visibility);
}
