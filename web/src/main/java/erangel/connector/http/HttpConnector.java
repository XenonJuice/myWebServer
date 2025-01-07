package erangel.connector.http;

import erangel.net.DefaultServerSocketFactory;
import erangel.net.ServerSocketFactory;

import java.net.ServerSocket;

public class HttpConnector implements Runnable {
    private ServerSocketFactory factory = null;

    @Override
    public void run() {

    }

    public ServerSocketFactory getFactory() {
        if (factory == null) {
            synchronized (this) {
                factory = new DefaultServerSocketFactory();
            }
        }
        return (factory);
    }

    public void setFactory(ServerSocketFactory factory) {
        this.factory = factory;
    }

    private ServerSocket openSocket() {
        return null;
    }

    void threadStart() {
    }

    void threadStop() {
    }

    void initalize() {
    }

    void start() {
    }

    void stop() {
    }
}
