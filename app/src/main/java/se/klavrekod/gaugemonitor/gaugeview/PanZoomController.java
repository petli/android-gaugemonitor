package se.klavrekod.gaugemonitor.gaugeview;

import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import se.klavrekod.gaugemonitor.GaugeImage;
import se.klavrekod.gaugemonitor.R;

/** GaugeView controller to pan and zoom the gauge image
 *
 * The scroll/scale code is by Tore Rudberg, from here:
 * http://stackoverflow.com/a/19545542
 */
public class PanZoomController
        extends GestureDetector.SimpleOnGestureListener
        implements IGaugeViewController, ScaleGestureDetector.OnScaleGestureListener, Camera.PreviewCallback {

    private final static String TAG = "GM:PanZoomController";

    private final GaugeView _gaugeView;
    private final GaugeImage _image;
    private Camera _camera;
    private final GestureDetector _gestureDetector;
    private final ScaleGestureDetector _scaleGestureDetector;

    private float _lastFocusX;
    private float _lastFocusY;

    public PanZoomController(GaugeView view, GaugeImage image)
    {
        _gaugeView = view;
        _image = image;

        _gestureDetector = new GestureDetector(view.getContext(), this);
        _gestureDetector.setIsLongpressEnabled(false);

        _scaleGestureDetector = new ScaleGestureDetector(view.getContext(), this);
    }

    @Override
    public void onStart(Camera camera) {
        _camera = camera;

        int bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21);
        _camera.addCallbackBuffer(new byte[(_image.getWidth() * _image.getHeight() * bitsPerPixel) / 8]);
        _camera.setPreviewCallbackWithBuffer(this);
        setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
    }

    @Override
    public void onStop() {
        if (_camera != null) {
            _camera.setPreviewCallbackWithBuffer(null);
            setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }

        updateImagePosition();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        _image.updatePreviewImage(data);
        _gaugeView.invalidate();
        _camera.addCallbackBuffer(data);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Workaround for pre-Jellybean, from http://stackoverflow.com/a/13807698
        float origX = event.getX();
        float origY = event.getY();

        event.setLocation(event.getRawX(), event.getRawY());
        _scaleGestureDetector.onTouchEvent(event);
        event.setLocation(origX, origY);

        _gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu) {
        MenuItem refresh = menu.findItem(R.id.action_refresh);
        refresh.setVisible(false);

        MenuItem panZoom = menu.findItem(R.id.action_pan_zoom);
        panZoom.setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Matrix drawMatrix = _gaugeView.getDrawMatrix();
        if (drawMatrix == null) {
            return false;
        }

        drawMatrix.postTranslate(-distanceX, -distanceY);
        _gaugeView.setDrawMatrix(drawMatrix);
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {

        if (_gaugeView.getDrawMatrix() == null) {
            return false;
        }

        _lastFocusX = detector.getFocusX();
        _lastFocusY = detector.getFocusY();
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {

        Matrix transformationMatrix = new Matrix();
        float focusX = detector.getFocusX();
        float focusY = detector.getFocusY();

        //Zoom focus is where the fingers are centered,
        transformationMatrix.postTranslate(-focusX, -focusY);

        float scale = Math.max(detector.getScaleFactor(), 0.2f);
        transformationMatrix.postScale(scale, scale);

        /* Adding focus shift to allow for scrolling with two pointers down. */
        float focusShiftX = focusX - _lastFocusX;
        float focusShiftY = focusY - _lastFocusY;
        transformationMatrix.postTranslate(focusX + focusShiftX, focusY + focusShiftY);

        _lastFocusX = focusX;
        _lastFocusY = focusY;

        Matrix drawMatrix = _gaugeView.getDrawMatrix();
        drawMatrix.postConcat(transformationMatrix);
        _gaugeView.setDrawMatrix(drawMatrix);

        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }

    /**
     * Calculate the new image position coordinates from a draw matrix.
     */
    private void updateImagePosition() {
        float[] values = new float[9];
        _gaugeView.getDrawMatrix().getValues(values);

        float scale = values[Matrix.MSCALE_X];
        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];

        Log.d(TAG, "Setting image from trans " + transX + " " + transY + " scale " + scale);

        float viewCenterX = _gaugeView.getWidth() * 0.5f;
        float viewCenterY = _gaugeView.getHeight() * 0.5f;

        float scaledWidth = _image.getWidth() * scale;
        float scaledHeight = _image.getHeight() * scale;

        float absImageCenterX = viewCenterX - transX;
        float absImageCenterY = viewCenterY - transY;

        // Translate into values relative gauge image
        _image.setCenterX(absImageCenterX / scaledWidth);
        _image.setCenterY(absImageCenterY / scaledHeight);
        _image.setScale(scale * _image.getHeight() / _gaugeView.getHeight());

        Log.d(TAG, "New image position: " + _image.getCenterX() + " " + _image.getCenterY() + " scale " + _image.getScale());
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
}
