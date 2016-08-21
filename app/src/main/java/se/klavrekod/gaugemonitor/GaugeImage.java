package se.klavrekod.gaugemonitor;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Model: holding the current gauge image, and its pan/zoom setting.
 */
public class GaugeImage {
    private Bitmap _bitmap;
    private float _centerX;
    private float _centerY;
    private float _scale;

    public GaugeImage() {
        _centerX = _centerY = 0.5f;
        _scale = 1;
    }

    public void updateImage(byte[] dataNv21, int w, int h) {
        // Copy the full image in gray scale into the bitmap
        int[] colors = new int[w * h];
        int i = 0;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                colors[i] = Color.rgb(dataNv21[i], dataNv21[i], dataNv21[i]);
                i++;
            }
        }

        _bitmap = Bitmap.createBitmap(colors, w, h, Bitmap.Config.ARGB_8888);
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
}
