package livonia.lifecycle;

import livonia.core.DefaultContext;
import livonia.core.DefaultHost;
import livonia.core.DefaultEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试容器生命周期管理
 */
public class LifecycleTest {
    
    private DefaultEngine engine;
    private DefaultHost host;
    private DefaultContext context;
    
    @BeforeEach
    public void setUp() {
        engine = new DefaultEngine();
        engine.setName("testEngine");
        engine.setDefaultHostName("localhost");
        
        host = new DefaultHost();
        host.setName("localhost");
        host.setAppBase("webapps");
        
        context = new DefaultContext();
        context.setName("testContext");
        context.setPath("/test");
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        try {
            if (context != null && isStarted(context)) {
                context.stop();
            }
        } catch (Exception e) {
            // ignore
        }
        try {
            if (host != null && isStarted(host)) {
                host.stop();
            }
        } catch (Exception e) {
            // ignore
        }
        try {
            if (engine != null && isStarted(engine)) {
                engine.stop();
            }
        } catch (Exception e) {
            // ignore
        }
    }
    
    @Test
    public void testEngineLifecycle() throws Exception {
        // 测试启动前状态
        assertFalse(isStarted(engine));
        
        // 启动引擎
        engine.start();
        assertTrue(isStarted(engine));
        
        // 停止引擎
        engine.stop();
        assertFalse(isStarted(engine));
    }
    
    @Test
    public void testHostLifecycle() throws Exception {
        // 将 host 添加到 engine
        engine.addChild(host);
        
        // 启动 engine 应该自动启动 host
        engine.start();
        assertTrue(isStarted(engine));
        assertTrue(isStarted(host));
        
        // 停止 engine 应该自动停止 host
        engine.stop();
        assertFalse(isStarted(engine));
        assertFalse(isStarted(host));
    }
    
    @Test
    public void testContextLifecycle() throws Exception {
        // 构建层级关系
        engine.addChild(host);
        host.addChild(context);
        
        // 启动顶层容器
        engine.start();
        
        // 验证所有容器都已启动
        assertTrue(isStarted(engine));
        assertTrue(isStarted(host));
        assertTrue(isStarted(context));
        
        // 停止顶层容器
        engine.stop();
        
        // 验证所有容器都已停止
        assertFalse(isStarted(engine));
        assertFalse(isStarted(host));
        assertFalse(isStarted(context));
    }
    
    @Test
    public void testDuplicateStart() throws Exception {
        engine.start();
        
        // 重复启动应该抛出异常
        assertThrows(LifecycleException.class, () -> engine.start());
    }
    
    @Test
    public void testStopBeforeStart() {
        // 未启动就停止应该抛出异常
        assertThrows(LifecycleException.class, () -> engine.stop());
    }
    
    private boolean isStarted(Object component) {
        try {
            // 使用反射获取 started 字段
            var field = component.getClass().getSuperclass().getDeclaredField("started");
            field.setAccessible(true);
            return (boolean) field.get(component);
        } catch (Exception e) {
            return false;
        }
    }
}