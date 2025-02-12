package erangel;

import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;

import javax.naming.directory.DirContext;

public interface Vas {
    String getName();

    String setName();

    Vas getParent();

    Vas setParent(Vas parent);

    DirContext getResources();

    void setResources(DirContext resources);

    void addChild(Vas child);

    Vas[] findChildren();

    Vas findChild(String name);

    void removeChild(Vas child);

    void invoke(HttpRequest req, HttpResponse res);

    Vas map(HttpRequest req, boolean writeRequest);
    // FIXME 增删容器事件监听器，
}
