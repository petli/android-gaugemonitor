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

import java.util.List;

public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback {
    private static final String TAG = "GM:MainActivity";

    private Camera _camera;
    private CameraPreview _preview;
    private GaugeView _gaugeView;
    private GaugeImage _image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        _image = new GaugeImage();
        _gaugeView = (GaugeView) findViewById(R.id.gauge_view);
        _gaugeView.setImage(_image);

        // Create an instance of Camera
        _camera = getCameraInstance();

        if (_camera == null)
        {
            Toast.makeText(getApplicationContext(), "Cannot get camera, giving up", Toast.LENGTH_LONG);
            return;
        }

        Camera.Parameters parameters = _camera.getParameters();
        if (!trySetFocusMode(parameters, Camera.Parameters.FOCUS_MODE_MACRO))
        {
            Log.i(TAG, "Using default focus mode " + parameters.getFocusMode());
        }

        parameters.setPreviewFormat(ImageFormat.NV21);

        _camera.setParameters(parameters);

        // Create our Preview view and set it as the content of our activity.
        _preview = new CameraPreview(this, _camera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(_preview);

        _camera.setPreviewCallback(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void releaseCamera(){
        if (_camera != null){
            _camera.setPreviewCallback(null);
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
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        _image.updateImage(data, previewSize.width, previewSize.height);
        _gaugeView.invalidate();
    }
}
