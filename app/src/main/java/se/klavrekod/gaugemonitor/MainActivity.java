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
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;

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
    private Runnable _scheduledPreview;
    private ImageResourceContainer _imageResourceContainer;
    private HttpServer _httpServer;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Default mode
        _gaugeViewController = new MonitorController();

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

        _gaugeViewController.onStart();
        _gaugeView.setController(_gaugeViewController);
        _scheduledPreview = null;

        _imageResourceContainer.setImage(_image);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop");

        _gaugeViewController.onStop();

        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        if (preview != null) {
            preview.removeView(_preview);
        }
        _preview = null;

        cancelCurrentPreview();

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
            restartPreviewCallback(1);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPreviewStatusChanged(boolean running) {
        if (running) {
            // Give it a second to get going
            restartPreviewCallback(1000);
        }
        else {
            cancelCurrentPreview();
        }
    }

    private void changeGaugeViewController(IGaugeViewController newController) {
        if (_gaugeViewController != null) {
            _gaugeViewController.onStop();
        }

        _gaugeViewController = newController;
        _gaugeViewController.onStart();

        _gaugeView.setController(_gaugeViewController);
        supportInvalidateOptionsMenu();

        restartPreviewCallback(1);
    }

    private void cancelCurrentPreview() {
        if (_scheduledPreview != null)
        {
            _gaugeView.removeCallbacks(_scheduledPreview);
            _scheduledPreview = null;
        }
        _camera.cancelAutoFocus();
        _camera.setPreviewCallbackWithBuffer(null);
        _camera.setOneShotPreviewCallback(null);
    }

    private void restartPreviewCallback(int forceDelay) {
        cancelCurrentPreview();

        int delay = _gaugeViewController.imageRefreshDelay();

        if (delay == 0) {
            new BufferedPreviewCallback();
        }
        else if (delay > 0) {
            _scheduledPreview = new OneShotPreviewCallback();
            _gaugeView.postDelayed(_scheduledPreview, forceDelay > 0 ? forceDelay : delay);
        }
    }

    private void releaseCamera(){
        if (_camera != null){
            cancelCurrentPreview();
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

    private class OneShotPreviewCallback implements Runnable, Camera.PreviewCallback, Camera.AutoFocusCallback {
        @Override
        public void run() {
            _scheduledPreview = null;
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
            _image.updateImage(data);
            _gaugeView.invalidate();
            restartPreviewCallback(0);
        }
    }

    private class BufferedPreviewCallback implements Camera.PreviewCallback {
        public BufferedPreviewCallback() {
            int bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21);
            _camera.addCallbackBuffer(new byte[(_image.getWidth() * _image.getHeight() * bitsPerPixel) / 8]);
            _camera.setPreviewCallbackWithBuffer(this);
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            _image.updateImage(data);
            _gaugeView.invalidate();
            _camera.addCallbackBuffer(data);
        }
    }
}
