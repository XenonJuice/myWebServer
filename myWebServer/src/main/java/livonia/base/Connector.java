package livonia.base;

/**
 * To make the abstract structure look more aesthetically pleasing,
 * this interface has been declared.
 * Currently, there is only one HTTP connector.<code>:-)</code>
 */
public interface Connector {

    int getPort();

    String getProtocol();

    void setProtocol(String protocol);


}
