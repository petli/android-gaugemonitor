package se.klavrekod.gaugemonitor.gaugeview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import se.klavrekod.gaugemonitor.GaugeImage;

/**
 * Show the gauge with the current pan/zoom with an overlay
 * for the target gauge area.
 */
public class GaugeView extends View {
    private static final String TAG = "GM:GaugeView";

    private GaugeImage _image;
    private IGaugeViewController _controller;

    private Matrix _drawMatrix;
    private Paint _bitmapPaint;
    private Paint _overlayPaint;

    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        _bitmapPaint = new Paint(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);

        _overlayPaint = new Paint();
        _overlayPaint.setARGB(128, 255, 0, 0);
        _overlayPaint.setStrokeWidth(3);
        _overlayPaint.setStyle(Paint.Style.STROKE);

        _drawMatrix = null;
    }

    public void setImage(GaugeImage image) {
        _image = image;
    }

    public void setController(IGaugeViewController controller) {
        _controller = controller;
        invalidate();
    }

    public Matrix getDrawMatrix() {
        if (_drawMatrix != null) {
            return new Matrix(_drawMatrix);
        }

        return null;
    }

    public void setDrawMatrix(Matrix drawMatrix) {
        if (_drawMatrix != null) {
            _drawMatrix.set(drawMatrix);
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() * 0.5f;
        float centerY = getHeight() * 0.5f;

        if (_image != null) {
            if (_drawMatrix == null) {
                // Initialize the draw matrix from the image settings
                _drawMatrix = new Matrix();
                _drawMatrix.postTranslate(centerX - _image.getWidth() * _image.getCenterX(), centerY - _image.getHeight() * _image.getCenterY());

                float scale = _image.getScale() * getHeight() / _image.getHeight();
                _drawMatrix.postScale(scale, scale, centerX, centerY);

                Log.d(TAG, "Initial matrix: " + _drawMatrix + " scale " + scale);
            }

            Bitmap bitmap = _image.getPreviewBitmap();
            if (bitmap != null) {
//                Log.d(TAG, "Drawing " + bitmap + " with "  + _drawMatrix);
                canvas.drawBitmap(bitmap, _drawMatrix, _bitmapPaint);
            }
        }

        canvas.drawCircle(centerX, centerY, getHeight() * GaugeImage.CIRCLE_RADIUS, _overlayPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (_controller != null) {
            return _controller.onTouchEvent(event);
        }

        return true;
    }
}
