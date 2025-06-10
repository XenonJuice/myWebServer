package livonia.XMLParse;

import livonia.connector.http.HttpConnector;
import livonia.core.DefaultServer;
import livonia.core.DefaultService;
import livonia.net.DefaultServerSocketFactory;
import org.xml.sax.Attributes;

import java.lang.reflect.Method;

import static livonia.XMLParse.ShutDownServerRuleSet.SetPropertiesRule.getObject;

public class ServerRuleSet extends RuleSet {

    public void addRuleInstances(MiniDigester d) {

        /* ===== <Server> ===== */
        d.addRule("Server", new ObjectCreateRule(DefaultServer.class));
        d.addRule("Server", new SetPropertiesRule());
        // 这里之前放进了 Livonia.java
        d.addRule("Server", new SetNextRuleAccessible("setServer"));

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
            return getObject(value, type);
        }

        private String capitalize(String name) {
            if (name == null || name.isEmpty()) return name;
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }
}
