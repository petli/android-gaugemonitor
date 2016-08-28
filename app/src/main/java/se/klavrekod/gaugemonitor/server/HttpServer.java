package se.klavrekod.gaugemonitor.server;

import android.net.wifi.WifiManager;
import android.util.Log;

import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerSocketProcessor;
import org.simpleframework.transport.SocketProcessor;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpServer {
    public static final String TAG = "GM:HttpServer";

    public SocketProcessor _server;
    public Connection _connection;

    public HttpServer(Container container) throws IOException {
        _server = new ContainerSocketProcessor(container, 1);
        _connection = new SocketConnection(_server);
    }

    public void start(int port) throws IOException {
        _connection.connect(new InetSocketAddress(port));
    }

    public void stop() {
        try {
            _connection.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing connection", e);
        }
    }
}
