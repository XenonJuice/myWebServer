package erangel.XMLParse;

import erangel.connector.http.HttpConnector;
import erangel.core.DefaultServer;
import erangel.core.DefaultService;
import erangel.net.DefaultServerSocketFactory;
import org.xml.sax.Attributes;

import java.lang.reflect.Method;

public class ServerRuleSet extends RuleSet {

    public void addRuleInstances(MiniDigester d) {

        /* ===== <Server> ===== */
        d.addRule("Server", new ObjectCreateRule(DefaultServer.class));
        d.addRule("Server", new SetPropertiesRule());
        /* ===== <Service> ===== */
        d.addRule("Server/Service", new ObjectCreateRule(DefaultService.class));
        d.addRule("Server/Service", new SetPropertiesRule()); // name
        d.addRule("Server/Service", new SetNextRuleAccessible("addService"));

        /* ===== <Connector> ===== */
        d.addRule("Server/Service/Connector", new ObjectCreateRule(HttpConnector.class));
        d.addRule("Server/Service/Connector", new SetPropertiesRule()); // 设置 port、protocol 等属性
        d.addRule("Server/Service/Connector", new SetNextRuleAccessible("addConnector"));
        d.addRule("Server/Service/Connector/Factory",
                new ObjectCreateRule(DefaultServerSocketFactory.class, "className"));
        d.addRule("Server/Service/Connector/Factory", new SetPropertiesRule());
        d.addRule("Server/Service/Connector/Factory",
                new SetNextRuleAccessible("setFactory"));
    }

    // 设置属性的通用 Rule（类型转换）
    public static class SetPropertiesRule implements Rule {
        @Override
        public void begin(String path, Attributes attrs, MiniDigester d) {
            Object target = d.peek();
            Class<?> clazz = target.getClass();

            for (int i = 0; i < attrs.getLength(); i++) {
                String name = attrs.getQName(i);
                String value = attrs.getValue(i);
                String setter = "set" + capitalize(name);

                boolean found = false;
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equals(setter) && method.getParameterCount() == 1) {
                        try {
                            Class<?> paramType = method.getParameterTypes()[0];
                            Object converted = convert(value, paramType);
                            method.setAccessible(true);
                            method.invoke(target, converted);
                            found = true;
                            break;
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to call method: " + setter + " with value: " + value, e);
                        }
                    }
                }

                if (!found) {
                    System.out.println("no method for " + setter);
                }
            }
        }

        private Object convert(String value, Class<?> type) {
            if (type == String.class) return value;
            if (type == int.class || type == Integer.class) return Integer.parseInt(value);
            if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
            if (type == long.class || type == Long.class) return Long.parseLong(value);
            if (type == double.class || type == Double.class) return Double.parseDouble(value);
            if (type == float.class || type == Float.class) return Float.parseFloat(value);
            if (type == Class.class) {
                try {
                    return Class.forName(value);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Failed to convert to Class: " + value, e);
                }
            }
            throw new RuntimeException("Unsupported parameter type: " + type);
        }

        private String capitalize(String name) {
            if (name == null || name.isEmpty()) return name;
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }
}
