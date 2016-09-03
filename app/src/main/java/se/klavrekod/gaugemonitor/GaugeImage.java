package se.klavrekod.gaugemonitor;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

/**
 * Model: holding the current gauge image, and its pan/zoom setting.
 */
public class GaugeImage {
    public static final String TAG = "GM:GaugeImage";

    public static final float CIRCLE_RADIUS = 0.33f;

    private Bitmap _previewBitmap;

    private Bitmap _gaugeBitmap;

    // Center is tracked as ratio of the width/height in the range [0.0-1.0]
    private float _centerX;
    private float _centerY;

    private float _scale;

    // Source image size
    private int _width;
    private int _height;

    public GaugeImage(int width, int height) {
        _width = width;
        _height = height;
        _centerX = _centerY = 0.5f;
        _scale = 1;

        // Pre-allocate to make update quickish during interactive pan/zoom mode
        _previewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    public void updatePreviewImage(byte[] dataNv21) {
        // Copy the full image in grayscale into the bitmap

        // Row-by-row, as a balance between memory allocation and bitmap calls
        int[] colors = new int[_width];

        for (int y = 0; y < _height; y++) {
            for (int x = 0; x < _width; x++) {
                int raw = dataNv21[y * _width + x];
                if (raw < 0) {
                    raw = 256 + raw;
                }
                colors[x] = Color.rgb(raw, raw, raw);
            }

            _previewBitmap.setPixels(colors, 0, _width, 0, y, _width, 1);
        }
    }

    public void updateGaugeFromPreview() {
        float radius = Math.min(_width, _height) * CIRCLE_RADIUS / _scale;
        float x = _width * _centerX - radius;
        float y = _height * _centerY - radius;

        Log.d(TAG, "Updating Gauge from x " + x + " y " + y + " radius " + radius);
        _gaugeBitmap = Bitmap.createBitmap(_previewBitmap, Math.max((int)x, 0), Math.max((int)y, 0), (int)Math.min(radius * 2, _width - x), (int)Math.min(radius * 2, _height - y));
    }

    public Bitmap getPreviewBitmap() {
        return _previewBitmap;
    }

    public Bitmap getGaugeBitmap() {
        return _gaugeBitmap;
    }

    public float getCenterX() {
        return _centerX;
    }

    public void setCenterX(float _centerX) {
        this._centerX = _centerX;
    }

    public float getCenterY() {
        return _centerY;
    }

    public void setCenterY(float _centerY) {
        this._centerY = _centerY;
    }

    public float getScale() {
        return _scale;
    }

    public void setScale(float _scale) {
        this._scale = _scale;
    }

    public int getWidth() {
        return _width;
    }

    public int getHeight() {
        return _height;
    }
}
