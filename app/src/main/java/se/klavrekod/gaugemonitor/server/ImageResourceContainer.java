package se.klavrekod.gaugemonitor.server;

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

            resp.setStatus(Status.NO_CONTENT);
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
}
