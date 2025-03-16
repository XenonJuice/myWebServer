package erangel.base;

import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;

public interface CheckPoint {
    void process(HttpRequest request, HttpResponse response, CheckPointContext context) throws Exception;
}
