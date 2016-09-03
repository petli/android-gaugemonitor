package se.klavrekod.gaugemonitor.server;

import android.graphics.Bitmap;
import android.util.Log;

import org.simpleframework.http.Protocol;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;

import java.io.IOException;

import se.klavrekod.gaugemonitor.GaugeImage;

public class ImageResourceContainer implements Container {
    private final static String TAG = "GM:ImageResourceCtr";

    private final static String IMAGE_PATH = "/image";

    private GaugeImage _image;

    public ImageResourceContainer() {
    }

    public void setImage(GaugeImage image) {
        _image = image;
    }

    @Override
    public void handle(Request req, Response resp) {
        try {
            String path = req.getPath().getPath();

            Log.i(TAG, req.getMethod() + " " + path + " from " + req.getClientAddress());

            resp.setDate(Protocol.DATE, System.currentTimeMillis());

            if (path.equals(IMAGE_PATH)) {
                handle_image(resp);
            }
            else {
                resp.setStatus(Status.NOT_FOUND);
            }

            resp.close();
        }
        catch (Exception e) {
            Log.e(TAG, "Error processing request for " + req.getPath(), e);
            resp.setStatus(Status.INTERNAL_SERVER_ERROR);
            try {
                resp.close();
            } catch (IOException e1) {
                Log.e(TAG, "Error closing error response", e);
            }
        }
    }

    private void handle_image(Response resp) throws IOException {
        if (_image == null) {
            resp.setStatus(Status.SERVICE_UNAVAILABLE);
            return;
        }

        Bitmap bitmap = _image.getGaugeBitmap();
        if (bitmap == null) {
            resp.setStatus(Status.SERVICE_UNAVAILABLE);
            return;
        }

        resp.setContentType("image/png");
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, resp.getOutputStream());
    }
}
