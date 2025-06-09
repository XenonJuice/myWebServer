package listeners;

import javax.servlet.*;

/**
 * 简单的属性监听器
 */
public class SimpleAttributeListener implements ServletContextAttributeListener {

    public void attributeAdded(ServletContextAttributeEvent event) {
        System.out.println("[Attribute Added] " + event.getName() + " = " + event.getValue());
    }

    public void attributeRemoved(ServletContextAttributeEvent event) {
        System.out.println("[Attribute Removed] " + event.getName());
    }

    public void attributeReplaced(ServletContextAttributeEvent event) {
        System.out.println("[Attribute Replaced] " + event.getName() + ", old = " + event.getValue());
    }
}