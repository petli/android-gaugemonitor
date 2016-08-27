package se.klavrekod.gaugemonitor.gaugeview;

import android.view.MotionEvent;

public interface IGaugeViewController {
    /**
     * Control when the image should next refresh
     * @return Delay in ms. 0 means continuous, -1 means don't refresh.
     */
    int imageRefreshDelay();

    /**
     * Called when this controller will start controlling the view.
     */
    void onStart();

    /**
     * Called when this controller will stop controlling the view.
     */
    void onStop();

    /**
     * Handle any touch events in the view.
     * @param event
     * @return
     */
    boolean onTouchEvent(MotionEvent event);
}
