package livonia.utils;

import livonia.base.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 将服务器配置写入XML文件
 */
public final class ServerXmlWriter {
    
    /**
     * 将当前服务器配置（包括动态部署的应用）写入server.xml
     */
    public static void writeServerXml(Server server, String xmlPath) throws Exception {
        // 首先读取原始的server.xml，获取静态配置的Context
        Set<String> staticContexts = loadStaticContexts(xmlPath);
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        
        // 创建根元素 <Server>
        Element serverElement = doc.createElement("Server");
        serverElement.setAttribute("shutdownPort", String.valueOf(server.getShutdownPort()));
        serverElement.setAttribute("shutdownCommand", server.getShutdownCommand());
        doc.appendChild(serverElement);
        
        // 遍历所有Service
        Service[] services = server.findServices();
        for (Service service : services) {
            Element serviceElement = doc.createElement("Service");
            serviceElement.setAttribute("name", service.getName());
            serverElement.appendChild(serviceElement);
            
            // 添加Connector
            Connector[] connectors = service.findConnectors();
            for (Connector connector : connectors) {
                Element connectorElement = doc.createElement("Connector");
                connectorElement.setAttribute("port", String.valueOf(connector.getPort()));
                connectorElement.setAttribute("protocol", connector.getProtocol());
                serviceElement.appendChild(connectorElement);
            }
            
            // 添加Engine
            Vas engine = service.getVas();
            if (engine != null) {
                Element engineElement = doc.createElement("Engine");
                engineElement.setAttribute("name", engine.getName());
                engineElement.setAttribute("defaultHostName", ((Engine)engine).getDefaultHostName());
                serviceElement.appendChild(engineElement);
                
                // 遍历所有Host
                Vas[] hosts = engine.findChildren();
                for (Vas host : hosts) {
                    Element hostElement = doc.createElement("Host");
                    hostElement.setAttribute("name", host.getName());
                    hostElement.setAttribute("appBase", ((Host)host).getAppBase());
                    engineElement.appendChild(hostElement);
                    
                    // 获取Host的appBase目录
                    String appBase = ((Host)host).getAppBase();
                    String appBaseAbsPath = appBase;
                    if (!new File(appBase).isAbsolute()) {
                        // 如果appBase是相对路径，转换为绝对路径
                        String coreDir = System.getProperty("core.dir");
                        appBaseAbsPath = new File(coreDir, appBase).getAbsolutePath();
                    }
                    
                    // 遍历所有Context（包括动态部署的）
                    Vas[] contexts = host.findChildren();
                    List<ContextInfo> contextInfos = new ArrayList<>();
                    // 使用Set去重，key为basePath
                    Set<String> processedBasePaths = new HashSet<>();
                    
                    for (Vas context : contexts) {
                        String path = ((Context)context).getPath();
                        String basePath = ((Context)context).getBasePath();
                        
                        // 将绝对路径转换为相对路径
                        if (basePath != null) {
                            // 去除末尾的斜杠
                            if (basePath.endsWith("/") || basePath.endsWith("\\")) {
                                basePath = basePath.substring(0, basePath.length() - 1);
                            }
                            
                            // 如果是绝对路径，尝试转换为相对路径
                            if (new File(basePath).isAbsolute()) {
                                try {
                                    File baseFile = new File(basePath);
                                    File appBaseFile = new File(appBaseAbsPath);
                                    
                                    // 检查basePath是否在appBase目录下
                                    if (baseFile.getCanonicalPath().startsWith(appBaseFile.getCanonicalPath())) {
                                        // 获取相对路径
                                        String relativePath = appBaseFile.toURI().relativize(baseFile.toURI()).getPath();
                                        // 去除末尾的斜杠
                                        if (relativePath.endsWith("/")) {
                                            relativePath = relativePath.substring(0, relativePath.length() - 1);
                                        }
                                        basePath = relativePath;
                                    }
                                } catch (IOException e) {
                                    // 如果转换失败，保持原样
                                }
                            }
                            
                            // 检查是否已经处理过相同的basePath
                            String key = path + "|" + basePath;
                            if (!processedBasePaths.contains(key)) {
                                processedBasePaths.add(key);
                                // 判断是否是动态部署的Context（不在原始server.xml中）
                                boolean isDynamic = !staticContexts.contains(key);
                                contextInfos.add(new ContextInfo(path, basePath, isDynamic));
                            }
                        }
                    }
                    
                    // 添加Context元素
                    for (ContextInfo info : contextInfos) {
                        // 如果是动态部署的应用，添加注释
                        if (info.isDynamic) {
                            Comment comment = doc.createComment(" 动态部署的应用 ");
                            hostElement.appendChild(comment);
                        }
                        
                        Element contextElement = doc.createElement("Context");
                        contextElement.setAttribute("path", info.path);
                        contextElement.setAttribute("basePath", info.basePath);
                        hostElement.appendChild(contextElement);
                    }
                }
            }
        }
        
        // 写入文件
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        
        // 备份原文件到oldServerXML文件夹
        File originalFile = new File(xmlPath);
        if (originalFile.exists()) {
            // 获取server目录路径
            File serverDir = originalFile.getParentFile();
            File oldServerXmlDir = new File(serverDir, "oldServerXML");
            
            // 创建oldServerXML目录（如果不存在）
            if (!oldServerXmlDir.exists()) {
                oldServerXmlDir.mkdirs();
            }
            
            // 使用时间戳创建备份文件名
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String backupFileName = "server_" + timestamp + ".xml";
            File backupFile = new File(oldServerXmlDir, backupFileName);
            
            // 复制原文件到备份目录
            try {
                java.nio.file.Files.copy(originalFile.toPath(), backupFile.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("原配置文件已备份到: " + backupFile.getAbsolutePath());
            } catch (IOException e) {
                System.out.println("备份配置文件失败: " + e.getMessage());
            }
        }
        
        // 写入新文件
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(xmlPath));
        transformer.transform(source, result);
        
        System.out.println("Server配置已保存到: " + xmlPath);
    }
    
    /**
     * 从原始server.xml中加载静态配置的Context
     * 返回一个Set，包含所有静态Context的"path|basePath"组合
     */
    private static Set<String> loadStaticContexts(String xmlPath) {
        Set<String> staticContexts = new HashSet<>();
        File xmlFile = new File(xmlPath);
        
        // 如果文件不存在，返回空集合
        if (!xmlFile.exists()) {
            return staticContexts;
        }
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();
            
            // 获取所有Context节点
            NodeList contextList = doc.getElementsByTagName("Context");
            for (int i = 0; i < contextList.getLength(); i++) {
                Node contextNode = contextList.item(i);
                if (contextNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element contextElement = (Element) contextNode;
                    String path = contextElement.getAttribute("path");
                    String basePath = contextElement.getAttribute("basePath");
                    if (path != null && basePath != null) {
                        staticContexts.add(path + "|" + basePath);
                    }
                }
            }
        } catch (Exception e) {
            // 如果解析失败，返回空集合
            System.out.println("无法解析原始server.xml: " + e.getMessage());
        }
        
        return staticContexts;
    }
    
    private static class ContextInfo {
        String path;
        String basePath;
        boolean isDynamic;
        
        ContextInfo(String path, String basePath, boolean isDynamic) {
            this.path = path;
            this.basePath = basePath;
            this.isDynamic = isDynamic;
        }
    }
}