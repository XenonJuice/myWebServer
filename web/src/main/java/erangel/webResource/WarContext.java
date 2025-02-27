package erangel.webResource;


import erangel.Const;

import javax.naming.*;
import javax.naming.directory.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import static erangel.Const.commonCharacters.SOLIDUS;

/**
 * war目录上下文的实现
 *
 * @author LILINJIAN
 * @version 2025/2/27
 */
public class WarContext extends AbstractDirContext {

    //<editor-fold desc = "attr">
    // war包
    private ZipFile rootZipFile = null;
    // war目录
    private Entry rootEntry = null;
    //</editor-fold>

    //<editor-fold desc = "构造器">
    public WarContext() {
        super();
    }

    public WarContext(Hashtable<String, Object> environment) {
        super(environment);
    }

    protected WarContext(ZipFile rootZipFile, Entry rootEntry) {
        super();
        this.rootZipFile = rootZipFile;
        this.rootEntry = rootEntry;
    }
    //</editor-fold>

    //<editor-fold desc = "抽象方法实现">
    @Override
    public Object lookup(String name) throws NamingException {
        return lookup(new CompositeName(name));
    }

    @Override
    public Object lookup(Name name)
            throws NamingException {
        if (name.isEmpty())
            return this;
        Entry entry = lookupInTree(name);
        if (entry == null) throw new NamingException();
        ZipEntry zipEntry = entry.getZipEntry();
        if (zipEntry.isDirectory()) {
            return new WarContext(rootZipFile, entry);
        } else {
            return new WARResource(entry.getZipEntry());
        }
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
        return null;
    }

    @Override
    public void bind(String name, Object obj, Attributes attrs) throws NamingException {

    }

    @Override
    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {

    }

    @Override
    public void unbind(String name) throws NamingException {

    }

    @Override
    public void rename(String oldName, String newName) throws NamingException {

    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return null;
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {

    }

    @Override
    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        return null;
    }

    @Override
    public DirContext getSchema(String name) throws NamingException {
        return null;
    }

    @Override
    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
        return null;
    }

    @Override
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        return null;
    }

    @Override
    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {

    }

    @Override
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {

    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return "";
    }

    //</editor-fold>
    //<editor-fold desc = "其他方法">
    public void setRootZipFile(String root) {
        if (root == null ||
                !(root.endsWith(Const.webApp.WAR))) {
            throw new IllegalArgumentException("根不是合法的war文件");
        }
        // 让war文件指向一个对象
        Path rootZipFile = Paths.get(root);
        // 检查文件是否存在、可读，且不能是目录
        if (!Files.exists(rootZipFile) || !Files.isReadable(rootZipFile) ||
                Files.isDirectory(rootZipFile)) {
            throw new IllegalArgumentException("指向的文件不是war文件");
        }
        // 将 Path 转为 File 用于构造 ZipFile
        try {
            this.rootZipFile = new ZipFile(rootZipFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException("将根文件用ZIP FILE声明时出现问题" + e.getMessage());
        }
        // 将根目录设置回超类中的变量
        setRoot(root);
        loadEntries();
    }

    /**
     * 递归实现一个树结构来表示war包
     * 例：
     * - index.jsp
     * - META-INF/
     * └── MANIFEST.MF
     * - WEB-INF/
     * ├── web.xml
     * ├── classes/
     * │ └── com/example/MyServlet.class
     * └── lib/
     * └── some-lib.jar
     * <p>
     * 内存结构：
     * Entry("/", ZipEntry("/"))
     * ├── Entry("index.jsp", ZipEntry("index.jsp"))
     * ├── Entry("META-INF", ZipEntry("META-INF/"))
     * │    └── Entry("MANIFEST.MF", ZipEntry("META-INF/MANIFEST.MF"))
     * └── Entry("WEB-INF", ZipEntry("WEB-INF/"))
     * ├── Entry("web.xml", ZipEntry("WEB-INF/web.xml"))
     * ├── Entry("classes", ZipEntry("WEB-INF/classes/"))
     * │    └── Entry("com", ZipEntry("WEB-INF/classes/com/"))
     * │         └── Entry("example", ZipEntry("WEB-INF/classes/com/example/"))
     * │              └── Entry("MyServlet.class", ZipEntry("WEB-INF/classes/com/example/MyServlet.class"))
     * └── Entry("lib", ZipEntry("WEB-INF/lib/"))
     * └── Entry("some-lib.jar", ZipEntry("WEB-INF/lib/some-lib.jar"))
     *
     * @author LILINJIAN
     */
    protected void loadEntries() {
        try {
            // 根节点，可视作 / 目录
            rootEntry = new Entry(SOLIDUS, new ZipEntry(SOLIDUS));
            // 将 Enumeration<ZipEntry> 转为 List<ZipEntry>，便于遍历
            List<? extends ZipEntry> zipEntries =
                    Collections.list(rootZipFile.entries());
            // 遍历所有 ZipEntry，逐个加入到树结构中
            for (ZipEntry entry : zipEntries) {
                String name;
                name = SOLIDUS + entry.getName();
                if (entry.isDirectory()) {
                    name = name.substring(0, name.length() - 1);
                }
                // 过滤掉空或无效的路径
                if (name.isEmpty()) {
                    continue;
                }
                addToTree(rootEntry, name, entry);
            }
        } catch (Exception e) {
            // FIXME
        }
    }

    //将一个 ZipEntry（以及它的路径信息）递归添加到以 parent 为根的树结构中。
    private void addToTree(Entry parent, String path, ZipEntry zipEntry) {
        // 去掉开头的斜杠，确保处理一致
        while (path.startsWith(SOLIDUS)) {
            path = path.substring(1);
        }
        // 若已空，则说明不需要再创建后续节点
        if (path.isEmpty()) {
            return;
        }
        // 根据 '/' 拆分路径：nodeName 为当前层级，childPath 为后续层级
        int slashPos = path.indexOf(SOLIDUS);
        boolean hasMorePath = (slashPos != -1);
        String nodeName = hasMorePath ? path.substring(0, slashPos) : path;
        String childPath = hasMorePath ? path.substring(slashPos + 1) : "";

        // 查看 parent 下是否已存在同名节点，若无则新创建
        Entry child = parent.getChild(nodeName);
        if (child == null) {
            // 如果后面仍有子路径，或者当前 entry 表示目录，可添加 SOLIDUS 标识（表示目录）；
            // 否则直接使用 zipEntry
            ZipEntry childEntry = (hasMorePath || zipEntry.isDirectory())
                    ? new ZipEntry(path + SOLIDUS)
                    : zipEntry;
            child = new Entry(nodeName, childEntry);
            parent.addChild(child);
        }

        // 若仍有后续路径，则递归往下创建
        if (!childPath.isEmpty()) {
            addToTree(child, childPath, zipEntry);
        }
    }

    protected Entry lookupInTree(Name name) {
        if (name.isEmpty()) return rootEntry;
        Entry currentEntry = rootEntry;
        for (int i = 0; i < name.size(); i++) {
            if (name.get(i).isEmpty()) continue;
            currentEntry = currentEntry.getChild(name.get(i));
            if (currentEntry == null) return null;
        }
        return currentEntry;
    }



    //</editor-fold>


    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>

    //<editor-fold desc = "内部类">
    /**
     * 针对war包内的次级目录设计的结构
     * ┌─────────────────────────┐
     * │        Entry            │
     * │ (封装一个ZipEntry信息)    │
     * ├─────────────────────────┤
     * │ name : String           │ <-- 该条目的名称
     * │ ----------------------- │
     * │ entry : ZipEntry        │ <-- 该条目对应的ZipEntry
     * │ ----------------------- │
     * │ children : List·<Entry> │ <-- 子节点（也都是Entry）
     * └─────────────────────────┘
     *
     * @author LILINJIAN
     * @version 2025/2/28
     */
    protected static class Entry {
        private final String name;
        private final ZipEntry zipEntry;
        private final List<Entry> children;

        public Entry(String name, ZipEntry zipEntry) {
            this.name = name;
            this.zipEntry = zipEntry;
            this.children = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public ZipEntry getZipEntry() {
            return zipEntry;
        }

        public List<Entry> getChildren() {
            return children;
        }

        public void addChild(Entry child) {
            children.add(child);
        }

        public Entry getChild(String childName) {
            for (Entry entry : children) {
                if (entry.getName().equals(childName)) {
                    return entry;
                }
            }
            return null;
        }
    }


    /**
     * 延迟加载
     * <p></p>
     * 等到确实要访问文件内容时(streamContent()方法被调用时)才去调用 `
     * rootZipFile.getInputStream(entry)` 来获取数据
     *
     * @author LILINJIAN
     * @version 2025/2/28
     */
    protected class WARResource extends Resource {
        protected ZipEntry entry;

        public WARResource(ZipEntry entry) {
            this.entry = entry;
        }

        public InputStream streamContent()
                throws IOException {
            try {
                if (binaryContent == null) {
                    inputStream = rootZipFile.getInputStream(entry);
                }
            } catch (ZipException e) {
                throw new IOException(e.getMessage());
            }
            return super.getStreamContent();
        }


    }
    //</editor-fold>
}
