package se.klavrekod.gaugemonitor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Show the gauge with the current pan/zoom with an overlay
 * for the target gauge area.
 */
public class GaugeView extends View {

    private GaugeImage _image;
    private Paint _bitmapPaint;
    private Paint _overlayPaint;

    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        _bitmapPaint = new Paint(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        _overlayPaint = new Paint();
        _overlayPaint.setARGB(128, 255, 0, 0);
        _overlayPaint.setStrokeWidth(3);
        _overlayPaint.setStyle(Paint.Style.STROKE);
    }

    public void setImage(GaugeImage image) {
        _image = image;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Bitmap bitmap = _image.getBitmap();
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0f, 0f, _bitmapPaint);
        }

        canvas.drawCircle(getWidth() / 2, getHeight() / 2, Math.min(getWidth(), getHeight()) * 0.33f, _overlayPaint);
    }
}
