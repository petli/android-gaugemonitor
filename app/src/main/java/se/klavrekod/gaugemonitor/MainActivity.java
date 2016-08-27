package se.klavrekod.gaugemonitor;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import se.klavrekod.gaugemonitor.gaugeview.GaugeView;
import se.klavrekod.gaugemonitor.gaugeview.IGaugeViewController;
import se.klavrekod.gaugemonitor.gaugeview.MonitorController;
import se.klavrekod.gaugemonitor.gaugeview.PanZoomController;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback {
    private static final String TAG = "GM:MainActivity";

    private Camera _camera;
    private CameraPreview _preview;
    private GaugeView _gaugeView;
    private GaugeImage _image;
    private IGaugeViewController _gaugeViewController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Default mode
        _gaugeViewController = new MonitorController();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart");

        _camera = getCameraInstance();
        if (_camera == null)
        {
            Toast.makeText(getApplicationContext(), "Cannot get camera, giving up", Toast.LENGTH_LONG).show();
            return;
        }

        Camera.Parameters parameters = _camera.getParameters();
        if (!trySetFocusMode(parameters, Camera.Parameters.FOCUS_MODE_MACRO))
        {
            Log.i(TAG, "Using default focus mode " + parameters.getFocusMode());
        }

        parameters.setPreviewFormat(ImageFormat.NV21);

        _camera.setParameters(parameters);

        Camera.Size size = parameters.getPreviewSize();
        int bitsPerPixel = ImageFormat.getBitsPerPixel(parameters.getPreviewFormat());

        Log.d(TAG, "Adding buffer for preview size: " + size.width + "x" + size.height + " bits/pixel: " + bitsPerPixel);
        _camera.addCallbackBuffer(new byte[(size.width * size.height * bitsPerPixel) / 8]);

        _preview = new CameraPreview(this, _camera);
        FrameLayout previewFrame = (FrameLayout) findViewById(R.id.camera_preview);
        if (previewFrame != null) {
            previewFrame.addView(_preview);
        }

        _image = new GaugeImage(size.width, size.height);
        _gaugeView = (GaugeView) findViewById(R.id.gauge_view);
        if (_gaugeView != null) {
            _gaugeView.setImage(_image);
        }

        _gaugeViewController.onStart();
        _gaugeView.setController(_gaugeViewController);

        _camera.setPreviewCallbackWithBuffer(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop");

        _gaugeViewController.onStop();

        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.removeView(_preview);
        _preview = null;

        _gaugeView = null;

        releaseCamera();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Refresh is only visible in monitor mode
        MenuItem refresh = menu.findItem(R.id.action_refresh);
        refresh.setVisible(false);

        if (_gaugeViewController instanceof MonitorController) {
            refresh.setVisible(true);

            MenuItem monitor = menu.findItem(R.id.action_monitor);
            monitor.setVisible(false);
        }
        else if (_gaugeViewController instanceof PanZoomController) {
            MenuItem panZoom = menu.findItem(R.id.action_pan_zoom);
            panZoom.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_pan_zoom) {
            Log.d(TAG, "Switching to pan/zoom controller");
            changeGaugeViewController(new PanZoomController(_gaugeView, _image));
            return true;
        }

        if (id == R.id.action_monitor) {
            Log.d(TAG, "Switching to monitor controller");
            changeGaugeViewController(new MonitorController());
            return true;
        }

        if (id == R.id.action_refresh) {
            Log.d(TAG, "Refreshing");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void changeGaugeViewController(IGaugeViewController newController) {
        if (_gaugeViewController != null) {
            _gaugeViewController.onStop();
        }

        _gaugeViewController = newController;
        _gaugeViewController.onStart();

        _gaugeView.setController(_gaugeViewController);
        supportInvalidateOptionsMenu();
    }

    private void releaseCamera(){
        if (_camera != null){
            _camera.setPreviewCallbackWithBuffer(null);
            _camera.release();
            _camera = null;
        }
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.e(TAG, "Error getting camera", e);
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    public static boolean trySetFocusMode(Camera.Parameters parameters, String wantedMode) {
        for (String mode : parameters.getSupportedFocusModes()) {
            if (mode.equals(wantedMode))
            {
                Log.i(TAG, "Set focus mode to " + wantedMode);
                parameters.setFocusMode(wantedMode);
                return true;
            }
        }

        return false;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        _image.updateImage(data);
        _gaugeView.invalidate();
        _camera.addCallbackBuffer(data);
    }
}
