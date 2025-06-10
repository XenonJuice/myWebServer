package livonia.base;

import livonia.connector.http.HttpRequest;
import livonia.connector.http.HttpResponse;

public interface Checkpoint {
    String getInfo();

    void process(HttpRequest request, HttpResponse response, CheckpointContext context) throws Exception;
}
