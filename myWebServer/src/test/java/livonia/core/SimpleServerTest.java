package livonia.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 简单的服务器核心功能测试
 */
public class SimpleServerTest {
    
    @Test
    public void testServerCreation() {
        DefaultServer server = new DefaultServer();
        server.setShutdownPort(8005);
        server.setShutdownCommand("SHUTDOWN");
        
        assertEquals(8005, server.getShutdownPort());
        assertEquals("SHUTDOWN", server.getShutdownCommand());
    }
    
    @Test
    public void testServiceCreation() {
        DefaultService service = new DefaultService();
        service.setName("testService");
        
        assertEquals("testService", service.getName());
    }
    
    @Test
    public void testEngineCreation() {
        DefaultEngine engine = new DefaultEngine();
        engine.setName("testEngine");
        engine.setDefaultHostName("localhost");
        
        assertEquals("testEngine", engine.getName());
        assertEquals("localhost", engine.getDefaultHostName());
    }
    
    @Test
    public void testHostCreation() {
        DefaultHost host = new DefaultHost();
        host.setName("localhost");
        host.setAppBase("webapps");
        
        assertEquals("localhost", host.getName());
        assertEquals("webapps", host.getAppBase());
    }
    
    @Test
    public void testContextCreation() {
        DefaultContext context = new DefaultContext();
        context.setName("testApp");
        context.setPath("/test");
        context.setBasePath("testApp");
        
        assertEquals("/test", context.getName());
        assertEquals("/test", context.getPath());
        assertEquals("testApp", context.getBasePath());
    }
    
    @Test
    public void testContainerHierarchy() {
        // 创建容器层级
        DefaultServer server = new DefaultServer();
        DefaultService service = new DefaultService();
        DefaultEngine engine = new DefaultEngine();
        DefaultHost host = new DefaultHost();
        DefaultContext context = new DefaultContext();
        
        // 设置基本属性
        service.setName("service1");
        engine.setName("engine1");
        host.setName("host1");
        context.setName("context1");
        
        // 构建层级关系
        server.addService(service);
        service.setVas(engine);
        engine.addChild(host);
        host.addChild(context);
        
        // 验证关系
        assertEquals(1, server.findServices().length);
        assertEquals(engine, service.getVas());
        assertEquals(host, engine.findChild("host1"));
        assertEquals(context, host.findChild("context1"));
        assertEquals(host, context.getParent());
    }
}