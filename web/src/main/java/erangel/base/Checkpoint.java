package erangel.base;

import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;

public interface Checkpoint {
    void process(HttpRequest request, HttpResponse response, CheckPointContext context) throws Exception;
}
