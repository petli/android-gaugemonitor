package se.klavrekod.gaugemonitor.gaugeview;

import android.view.MotionEvent;

/**
 * Controller during gauge monitoring (main mode).
 */
public class MonitorController implements IGaugeViewController {
    @Override
    public int imageRefreshDelay() {
        // TODO: make configurables
        return 10000;
    }

    @Override
    public void onStart() { }

    @Override
    public void onStop() { }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}
