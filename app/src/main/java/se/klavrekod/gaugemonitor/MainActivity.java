package se.klavrekod.gaugemonitor;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.IOException;

import se.klavrekod.gaugemonitor.gaugeview.GaugeView;
import se.klavrekod.gaugemonitor.gaugeview.IGaugeViewController;
import se.klavrekod.gaugemonitor.gaugeview.MonitorController;
import se.klavrekod.gaugemonitor.gaugeview.PanZoomController;
import se.klavrekod.gaugemonitor.server.HttpServer;
import se.klavrekod.gaugemonitor.server.ImageResourceContainer;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity implements ICameraPreviewStatusListener {
    private static final String TAG = "GM:MainActivity";

    private Camera _camera;
    private CameraPreview _preview;
    private GaugeView _gaugeView;
    private GaugeImage _image;
    private IGaugeViewController _gaugeViewController;
    private ImageResourceContainer _imageResourceContainer;
    private HttpServer _httpServer;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        _imageResourceContainer = new ImageResourceContainer();
        try {
            String ip = "<unknown>";
            int port = 7980;

            // TODO: figure this out properly, and react when wifi address change
            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            if (wm != null) {
                ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            }

            String url = "http://" + ip + ":" + port;
            Log.i(TAG, "Starting server on " + url);
            Toast.makeText(getApplicationContext(), "Server URL: " + url, Toast.LENGTH_LONG).show();

            _httpServer = new HttpServer(_imageResourceContainer);
            _httpServer.start(port);
        } catch (IOException e) {
            Log.e(TAG, "Error starting HTTP server", e);
            Toast.makeText(getApplicationContext(), "Error starting HTTP server: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy");

        if (_httpServer != null) {
            _httpServer.stop();
            _httpServer = null;
        }

        _imageResourceContainer = null;
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        _camera = getCameraInstance();
        if (_camera == null)
        {
            Toast.makeText(getApplicationContext(), "Cannot get camera, giving up", Toast.LENGTH_LONG).show();
            return;
        }

        Camera.Parameters parameters = _camera.getParameters();
        if (!trySetFocusMode(parameters, Camera.Parameters.FOCUS_MODE_AUTO))
        {
            Log.i(TAG, "Using default focus mode " + parameters.getFocusMode());
        }

        // Format that must be supported by all cameras, and usefully it gets us
        // luminance as a separate component without having to deal with RGB
        parameters.setPreviewFormat(ImageFormat.NV21);

        _camera.setParameters(parameters);

        _preview = new CameraPreview(this, _camera, this);
        FrameLayout previewFrame = (FrameLayout) findViewById(R.id.camera_preview);
        if (previewFrame != null) {
            previewFrame.addView(_preview);
        }

        Camera.Size size = parameters.getPreviewSize();
        _image = new GaugeImage(size.width, size.height);
        _gaugeView = (GaugeView) findViewById(R.id.gauge_view);
        if (_gaugeView != null) {
            _gaugeView.setImage(_image);
        }

        if (_gaugeViewController == null) {
            // Default mode
            _gaugeViewController = new MonitorController(_gaugeView, _image);
            supportInvalidateOptionsMenu();
        }

        if (_gaugeView != null) {
            _gaugeView.setController(_gaugeViewController);
        }

        _imageResourceContainer.setImage(_image);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop");

        if (_gaugeViewController != null) {
            _gaugeViewController.onStop();
        }

        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        if (preview != null) {
            preview.removeView(_preview);
        }
        _preview = null;

        _gaugeView = null;

        releaseCamera();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (_gaugeViewController != null) {
            _gaugeViewController.onCreateOptionsMenu(menu);
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
            changeGaugeViewController(new MonitorController(_gaugeView, _image));
            return true;
        }

        if (_gaugeViewController != null && _gaugeViewController.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPreviewStatusChanged(boolean running) {
        if (_gaugeViewController != null) {
            if (running) {
                _gaugeViewController.onStart(_camera);
            } else {
                _gaugeViewController.onStop();
            }
        }
    }

    private void changeGaugeViewController(IGaugeViewController newController) {
        if (_gaugeViewController != null) {
            _gaugeViewController.onStop();
        }

        _gaugeViewController = newController;
        _gaugeViewController.onStart(_camera);

        _gaugeView.setController(_gaugeViewController);
        supportInvalidateOptionsMenu();
    }

    private void releaseCamera(){
        if (_camera != null){
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
        if (parameters.getSupportedFocusModes().contains(wantedMode))
        {
            Log.i(TAG, "Set focus mode to " + wantedMode);
            parameters.setFocusMode(wantedMode);
            return true;
        }

        return false;
    }
}
