package livonia.mapper;

import livonia.base.Context;
import livonia.base.Host;
import livonia.connector.http.HttpRequest;
import livonia.core.DefaultContext;
import livonia.core.DefaultHost;
import livonia.core.DefaultEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试请求映射功能
 */
public class RequestMappingTest {
    
    private DefaultEngine engine;
    private DefaultHost defaultHost;
    private DefaultHost virtualHost;
    private DefaultContext app1Context;
    private DefaultContext app2Context;
    
    @BeforeEach
    public void setUp() throws Exception {
        // 创建引擎
        engine = new DefaultEngine();
        engine.setName("testEngine");
        engine.setDefaultHostName("localhost");
        engine.setMapper(new EngineMapper());
        
        // 创建默认主机
        defaultHost = new DefaultHost();
        defaultHost.setName("localhost");
        defaultHost.setMapper(new HostMapper());
        
        // 创建虚拟主机
        virtualHost = new DefaultHost();
        virtualHost.setName("virtual.example.com");
        virtualHost.setMapper(new HostMapper());
        
        // 创建应用上下文
        app1Context = new DefaultContext();
        app1Context.setName("app1");
        app1Context.setPath("/app1");
        app1Context.setMapper(new ContextMapper());
        
        app2Context = new DefaultContext();
        app2Context.setName("app2");
        app2Context.setPath("/app2");
        app2Context.setMapper(new ContextMapper());
        
        // 构建层级关系
        engine.addChild(defaultHost);
        engine.addChild(virtualHost);
        defaultHost.addChild(app1Context);
        virtualHost.addChild(app2Context);
        
        // 启动所有组件
        engine.start();
    }
    
    @Test
    public void testDefaultHostMapping() {
        // 创建模拟请求
        HttpRequest request = Mockito.mock(HttpRequest.class);
        Mockito.when(request.getHeader("Host")).thenReturn("localhost:8080");
        Mockito.when(request.getRequestURI()).thenReturn("/app1/test");
        
        // 映射到主机
        Host mappedHost = (Host) engine.map(request, true);
        assertNotNull(mappedHost);
        assertEquals("localhost", mappedHost.getName());
        
        // 映射到上下文
        Mockito.when(request.getContextPath()).thenReturn("/app1");
        Context mappedContext = (Context) defaultHost.map(request, true);
        assertNotNull(mappedContext);
        assertEquals("/app1", mappedContext.getPath());
    }
    
    @Test
    public void testVirtualHostMapping() {
        // 创建模拟请求
        HttpRequest request = Mockito.mock(HttpRequest.class);
        Mockito.when(request.getHeader("Host")).thenReturn("virtual.example.com");
        Mockito.when(request.getRequestURI()).thenReturn("/app2/test");
        
        // 映射到虚拟主机
        Host mappedHost = (Host) engine.map(request, true);
        assertNotNull(mappedHost);
        assertEquals("virtual.example.com", mappedHost.getName());
        
        // 映射到上下文
        Mockito.when(request.getContextPath()).thenReturn("/app2");
        Context mappedContext = (Context) virtualHost.map(request, true);
        assertNotNull(mappedContext);
        assertEquals("/app2", mappedContext.getPath());
    }
    
    @Test
    public void testFallbackToDefaultHost() {
        // 创建模拟请求，使用未知主机名
        HttpRequest request = Mockito.mock(HttpRequest.class);
        Mockito.when(request.getHeader("Host")).thenReturn("unknown.example.com");
        Mockito.when(request.getRequestURI()).thenReturn("/app1/test");
        
        // 应该回退到默认主机
        Host mappedHost = (Host) engine.map(request, true);
        assertNotNull(mappedHost);
        assertEquals("localhost", mappedHost.getName());
    }
    
    @Test
    public void testContextNotFound() {
        // 创建模拟请求，使用不存在的路径
        HttpRequest request = Mockito.mock(HttpRequest.class);
        Mockito.when(request.getHeader("Host")).thenReturn("localhost");
        Mockito.when(request.getRequestURI()).thenReturn("/nonexistent/test");
        Mockito.when(request.getContextPath()).thenReturn("/nonexistent");
        
        // 映射到主机
        Host mappedHost = (Host) engine.map(request, true);
        assertNotNull(mappedHost);
        
        // 映射上下文应该返回 null
        Context mappedContext = (Context) defaultHost.map(request, true);
        assertNull(mappedContext);
    }
}