package se.klavrekod.gaugemonitor.gaugeview;

import android.hardware.Camera;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

public interface IGaugeViewController {
    /**
     * Called when this controller will start controlling the view.
     */
    void onStart(Camera _camera);

    /**
     * Called when this controller will stop controlling the view.
     */
    void onStop();

    /**
     * Handle any touch events in the view.
     *
     * @param event
     * @return
     */
    boolean onTouchEvent(MotionEvent event);

    /**
     * The controller can use this to disable menu items not relevant to it.
     * @param menu
     */
    void onCreateOptionsMenu(Menu menu);

    boolean onOptionsItemSelected(MenuItem item);
}