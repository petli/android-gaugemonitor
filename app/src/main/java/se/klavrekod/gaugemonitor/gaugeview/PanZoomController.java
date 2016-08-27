package se.klavrekod.gaugemonitor.gaugeview;

import android.graphics.Matrix;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import se.klavrekod.gaugemonitor.GaugeImage;

/** GaugeView controller to pan and zoom the gauge image
 *
 * The scroll/scale code is by Tore Rudberg, from here:
 * http://stackoverflow.com/a/19545542
 */
public class PanZoomController
        extends GestureDetector.SimpleOnGestureListener
        implements IGaugeViewController, ScaleGestureDetector.OnScaleGestureListener {

    private final static String TAG = "GM:PanZoomController";

    private final GaugeView _view;
    private final GaugeImage _image;
    private final GestureDetector _gestureDetector;
    private final ScaleGestureDetector _scaleGestureDetector;

    private float _lastFocusX;
    private float _lastFocusY;

    public PanZoomController(GaugeView view, GaugeImage image)
    {
        _view = view;
        _image = image;

        _gestureDetector = new GestureDetector(view.getContext(), this);
        _gestureDetector.setIsLongpressEnabled(false);

        _scaleGestureDetector = new ScaleGestureDetector(view.getContext(), this);
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
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Matrix drawMatrix = _view.getDrawMatrix();
        if (drawMatrix == null) {
            return false;
        }

        drawMatrix.postTranslate(-distanceX, -distanceY);
        _view.setDrawMatrix(drawMatrix);
        updateImagePosition(drawMatrix);
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {

        if (_view.getDrawMatrix() == null) {
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

        Matrix drawMatrix = _view.getDrawMatrix();
        drawMatrix.postConcat(transformationMatrix);
        _view.setDrawMatrix(drawMatrix);

        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        updateImagePosition(_view.getDrawMatrix());
    }

    /**
     * Calculate the new image position coordinates from a draw matrix.
     * @param drawMatrix
     */
    private void updateImagePosition(Matrix drawMatrix) {
        float[] values = new float[9];
        drawMatrix.getValues(values);

        float scale = values[Matrix.MSCALE_X];
        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];

        Log.d(TAG, "Setting image from trans " + transX + " " + transY + " scale " + scale);

        float viewCenterX = _view.getWidth() * 0.5f;
        float viewCenterY = _view.getHeight() * 0.5f;

        float scaledWidth = _image.getWidth() * scale;
        float scaledHeight = _image.getHeight() * scale;

        float absImageCenterX = viewCenterX - transX;
        float absImageCenterY = viewCenterY - transY;

        // Translate into values relative gauge image
        _image.setCenterX(absImageCenterX / scaledWidth);
        _image.setCenterY(absImageCenterY / scaledHeight);
        _image.setScale(scale * _image.getHeight() / _view.getHeight());

        Log.d(TAG, "New image position: " + _image.getCenterX() + " " + _image.getCenterY() + " scale " + _image.getScale());
    }
}
