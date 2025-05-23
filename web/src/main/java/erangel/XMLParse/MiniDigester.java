package erangel.XMLParse;

import erangel.core.DefaultContext;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * <strong>XML解析器，将XML元素映射到 Java 对象</strong><br><br>
 *
 * <strong>Stack Progress</strong><br>
 * <table>
 *   <tr>
 *     <th>currentTag</th>
 *     <th>actions</th>
 *     <th>stack Condition (head → tail)</th>
 *   </tr>
 *   <tr>
 *     <td>&lt;server&gt;</td>
 *     <td>new class Server and push</td>
 *     <td>Server</td>
 *   </tr>
 *   <tr>
 *     <td>&lt;service&gt;</td>
 *     <td>new class Service and push</td>
 *     <td>Service, Server</td>
 *   </tr>
 *   <tr>
 *     <td>&lt;connector&gt;</td>
 *     <td>new class Connector and push</td>
 *     <td>Connector, Service, Server</td>
 *   </tr>
 *   <tr>
 *     <td>&lt;/connector&gt;</td>
 *     <td>pop Connector and peek Service<br>invoke addConnector(conn)</td>
 *     <td>Service, Server</td>
 *   </tr>
 *   <tr>
 *     <td>&lt;/service&gt;</td>
 *     <td>pop Service and peek Server<br>invoke addService(service)</td>
 *     <td>Server</td>
 *   </tr>
 *   <tr>
 *     <td>&lt;/server&gt;</td>
 *     <td>may pop Server</td>
 *     <td>null</td>
 *   </tr>
 * </table><br>
 *
 * <strong>示例XML：</strong><br>
 * <pre>
 * &lt;server class="demo.Server"&gt;
 *   &lt;service class="demo.Service"&gt;
 *     &lt;connector class="demo.Connector" port="8080"/&gt;
 *     &lt;init-param&gt;
 *       &lt;param-name&gt;encoding&lt;/param-name&gt;
 *       &lt;param-value&gt;UTF-8&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *   &lt;/service&gt;
 * &lt;/server&gt;
 * </pre>
 *
 * <strong>MiniDigester 使用示例：</strong><br>
 * <pre>
 * MiniDigester digester = new MiniDigester();
 *
 * // ===== server =====
 * digester.addRule("server", new ObjectCreateRule());
 *
 * // ===== service =====
 * digester.addRule("server/service", new ObjectCreateRule());
 * digester.addRule("server/service", new SetPropertiesRule());
 * digester.addRule("server/service", new SetNextRule("addService"));
 *
 * // ===== connector =====
 * digester.addRule("server/service/connector", new ObjectCreateRule());
 * digester.addRule("server/service/connector", new SetPropertiesRule());
 * digester.addRule("server/service/connector", new SetNextRule("addConnector"));
 *
 * // ===== init-param: param-name + param-value + callMethod =====
 * List<String> paramArgs = new ArrayList<>();
 * digester.addRule("server/service/init-param/param-name", new CallParamRule(paramArgs, 0));
 * digester.addRule("server/service/init-param/param-value", new CallParamRule(paramArgs, 1));
 * digester.addRule("server/service/init-param", new CallMethodRule("setInitParam", paramArgs));
 * </pre>
 */
public class MiniDigester {
    private final Deque<Object> stack = new ArrayDeque<>();
    private final Map<String, List<Rule>> ruleMap = new HashMap<>();
    private boolean namespaceAware = false;

    static String getAttribute(Attributes attrs, String name) {
        String v = attrs.getValue(name);            // qName 带命名空间前缀的元素名或属性名
        if (v == null) v = attrs.getValue("", name); // (uri,localName) – works when namespaceAware=true
        return v;
    }

    static Object convert(Class<?> type, String value) {
        if (value == null) return null;
        return switch (type.getName()) {
            case "int", "java.lang.Integer" -> Integer.valueOf(value);
            case "long", "java.lang.Long" -> Long.valueOf(value);
            case "boolean", "java.lang.Boolean" -> Boolean.valueOf(value);
            case "double", "java.lang.Double" -> Double.valueOf(value);
            default -> value;
        };
    }

    public static void main(String[] args) throws Exception {
//        String xml = """
//                <server class='erangel.xml.testXML$Server'>
//                  <service class='erangel.xml.testXML$Service' name='http'>
//                    <connector class='erangel.xml.testXML$Connector' port='8080'/>
//                  </service>
//                </server>""";
//
//        MiniDigester d = new MiniDigester();
//        d.addRule("server", new ObjectCreateRule());
//        d.addRule("server/service", new ObjectCreateRule());
//        d.addRule("server/service", new SetPropertiesRule());
//        d.addRule("server/service", new SetNextRule("addService"));
//        d.addRule("server/service/connector", new ObjectCreateRule());
//        d.addRule("server/service/connector", new SetPropertiesRule());
//        d.addRule("server/service/connector", new SetNextRule("addConnector"));
//
//        d.parse(new java.io.ByteArrayInputStream(xml.getBytes()));
//        System.out.println(d.pop());
        MiniDigester digester = new MiniDigester();
        digester.setNamespaceAware(true);           // web.xml 通常带默认命名空间
        new WebRuleSet().addRuleInstances(digester);

        try (InputStream in = Files.newInputStream(Path.of("/Users/lilinjian/IdeaProjects/myWebServer/web/src/main/java/erangel/xml/exampleWebXML.xml"))) {
            digester.parse(in);
            DefaultContext ctx = (DefaultContext) digester.pop();    // ctx 已含映射 / 过滤器 / 监听器信息
            System.out.println();
        }
    }

    /**
     * 启用/禁用 XML 命名空间。在解析之前调用。
     */
    public void setNamespaceAware(boolean namespaceAware) {
        this.namespaceAware = namespaceAware;
    }

    /* ---------------- Rule registration ---------------*/
    public void addRule(String pattern, Rule rule) {
        ruleMap.computeIfAbsent(pattern, k -> new ArrayList<>()).add(rule);
    }

    public void addRuleSet(RuleSet ruleSet) {
        ruleSet.addRuleInstances(this);
    }
    public void addCallMethod(String path, String methodName) {
        addCallMethod(path, methodName, 1, new String[]{path}, String.class);
    }


    /**
     * 便捷注册 “收集参数 → 调用目标方法” 的组合规则。
     *
     * @param path        父元素路径（在元素结束时调用目标方法）
     * @param methodName  目标方法名
     * @param paramCount  参数个数
     * @param paramPaths  每个参数所在元素的完整路径数组，顺序需与方法参数顺序一致
     * @param types       每个参数的 Java 类型（用于自动转换），长度必须等于 paramCount
     */
    public void addCallMethod(String path,
                              String methodName,
                              int paramCount,
                              String[] paramPaths,
                              Class<?>... types) {

        if (paramPaths.length != paramCount || types.length != paramCount) {
            throw new IllegalArgumentException("paramCount / paramPaths / types 数量不一致");
        }



        // 用来暂存参数文本
        List<String> paramList = new ArrayList<>(Collections.nCopies(paramCount, null));

        // 为每个参数路径注册 CallParamRule
        for (int i = 0; i < paramCount; i++) {
            addRule(paramPaths[i], new CallParamRule(paramList, i));
        }

        // 在父元素结束标签时调用目标方法
        addRule(path, new CallMethodRule(methodName, paramList, types));
    }

    public void parse(InputStream xml) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(namespaceAware);
        factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", false);
        factory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        SAXParser parser = factory.newSAXParser();
        parser.parse(xml, new Handler());
    }

    /* ---------------- Stack helpers ------------------*/
    public void push(Object obj) {
        stack.push(obj);
    }

    @SuppressWarnings("unchecked")
    public <T> T peek() {
        return (T) stack.peek();
    }

    public Object pop() {
        return stack.pop();
    }

    public void clear() {
        stack.clear();
    }

    private void invokeRules(String path, RulePhase phase, Attributes attrs, String bodyText) {
        List<Rule> list = ruleMap.get(path);
        if (list == null) return;
        for (Rule r : list) {
            switch (phase) {
                case BEGIN -> r.begin(path, attrs, this);
                case BODY -> r.body(path, bodyText, this);
                case END -> r.end(path, this);
            }
        }
    }

    private enum RulePhase {BEGIN, BODY, END}


    /*
     *  SAX Handler
     */
    private class Handler extends DefaultHandler {
        private final StringBuilder body = new StringBuilder();
        private final Deque<String> elementPath = new ArrayDeque<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            flushBody();
            String name = namespaceAware ? localName : qName;
            elementPath.addLast(name);
            invokeRules(currentPath(), RulePhase.BEGIN, attributes, null);
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            body.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            flushBody();
            invokeRules(currentPath(), RulePhase.END, null, null);
            elementPath.removeLast();
        }

        private void flushBody() {
            if (body.isEmpty()) return;
            String txt = body.toString().trim();
            if (!txt.isEmpty())
                invokeRules(currentPath(), RulePhase.BODY, null, txt);
            body.setLength(0);
        }

        private String currentPath() {
            return String.join("/", elementPath);
        }
    }
}
