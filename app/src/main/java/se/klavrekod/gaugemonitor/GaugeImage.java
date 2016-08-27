package se.klavrekod.gaugemonitor;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Model: holding the current gauge image, and its pan/zoom setting.
 */
public class GaugeImage {
    public static final float CIRCLE_RADIUS = 0.33f;

    private Bitmap _bitmap;

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

        _bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    public void updateImage(byte[] dataNv21) {
        // Copy the full image in gray scale into the bitmap

        // Row-by-row, as a balance between memory allocation and bitmap calls
        int[] colors = new int[_width];

        for (int y = 0; y < _height; y++) {
            for (int x = 0; x < _width; x++) {
                byte brightness = dataNv21[y * _width + x];
                colors[x] = Color.rgb(brightness, brightness, brightness);
            }

            _bitmap.setPixels(colors, 0, _width, 0, y, _width, 1);
        }
    }

    public Bitmap getBitmap() {
        return _bitmap;
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
