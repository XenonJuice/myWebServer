package erangel.base;

import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;

public interface Checkpoint {
    String getInfo();

    void process(HttpRequest request, HttpResponse response, CheckpointContext context) throws Exception;
}
