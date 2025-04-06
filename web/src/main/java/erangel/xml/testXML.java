package erangel.xml;

import java.util.ArrayList;
import java.util.List;

public class testXML {
    public static class Server {
        private final List<Service> services = new ArrayList<>();

        public void addService(Service s) {
            services.add(s);
        }

        @Override
        public String toString() {
            return "Server" + services;
        }
    }

    public static class Service {
        private final List<Connector> connectors = new ArrayList<>();
        private String name;

        public void setName(String name) {
            this.name = name;
        }

        public void addConnector(Connector c) {
            connectors.add(c);
        }

        @Override
        public String toString() {
            return "{Service " + name + connectors + "}";
        }
    }

    public static class Connector {
        private int port;

        public void setPort(int port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "(Connector:" + port + ")";
        }
    }
}
