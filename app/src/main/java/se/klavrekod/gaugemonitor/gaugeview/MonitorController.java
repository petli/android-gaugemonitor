package se.klavrekod.gaugemonitor.gaugeview;

import android.hardware.Camera;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import se.klavrekod.gaugemonitor.GaugeImage;
import se.klavrekod.gaugemonitor.R;

/**
 * Controller during gauge monitoring (main mode).
 */
@SuppressWarnings("deprecation")
public class MonitorController implements IGaugeViewController {
    private static final String TAG = "GM:MonitorController";

    private final GaugeView _gaugeView;
    private final GaugeImage _image;
    private Camera _camera;
    private Runnable _scheduledPreview;

    public MonitorController(GaugeView gaugeView, GaugeImage image) {
        _gaugeView = gaugeView;
        _image = image;
    }

    @Override
    public void onStart(Camera camera) {
        _camera = camera;

        // Give preview a second to settle
        restartPreviewCallback(1000);
    }

    @Override
    public void onStop() {
        cancelCurrentPreview();
        _camera = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu) {
        MenuItem monitor = menu.findItem(R.id.action_monitor);
        monitor.setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            Log.d(TAG, "Refreshing");
            restartPreviewCallback(0);
            return true;
        }

        return false;
    }

    private void restartPreviewCallback(int delay) {
        cancelCurrentPreview();

        if (_camera != null) {
            _scheduledPreview = new OneShotPreviewCallback();
            _gaugeView.postDelayed(_scheduledPreview, delay);
        }
    }

    private void cancelCurrentPreview() {
        if (_scheduledPreview != null)
        {
            _gaugeView.removeCallbacks(_scheduledPreview);
            _scheduledPreview = null;
        }

        if (_camera != null) {
            _camera.cancelAutoFocus();
            _camera.setOneShotPreviewCallback(null);
            setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }
    }

    private void setFlashMode(String mode) {
        Camera.Parameters parameters = _camera.getParameters();
        parameters.setFlashMode(mode);
        try {
            _camera.setParameters(parameters);
        }
        catch (RuntimeException e) {
            Log.e(TAG, "Failed to set flash mode " + mode, e);
        }
    }

    private class OneShotPreviewCallback implements Runnable, Camera.PreviewCallback, Camera.AutoFocusCallback {
        @Override
        public void run() {
            _scheduledPreview = null;

            setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

            Log.d(TAG, "OneShotPreviewCallback starting autofocus");
            try {
                _camera.autoFocus(this);
            }
            catch (RuntimeException e) {
                Log.e(TAG, "Autofocus failed, rescheduling", e);
                restartPreviewCallback(5000);
            }
        }

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                Log.d(TAG, "Autofocus successful, getting picture");
                _camera.setOneShotPreviewCallback(this);
            }
            else
            {
                Log.e(TAG, "Autofocus not successful, rescheduling");
                restartPreviewCallback(5000);
            }
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Log.d(TAG, "Got one shot preview frame ");
            _image.updatePreviewImage(data);
            _image.updateGaugeFromPreview();
            _gaugeView.invalidate();
            restartPreviewCallback(60000);
        }
    }
}
