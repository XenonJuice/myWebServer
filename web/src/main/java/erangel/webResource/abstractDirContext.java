package erangel.webResource;

import javax.naming.*;
import javax.naming.directory.*;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * DirContext的基础实现
 * @author LILINJIAN
 * @version 2025/2/26
 */
public abstract class abstractDirContext implements DirContext {

    //<editor-fold desc = "构造器">
    public abstractDirContext() {
        this.environment = new Hashtable<>();
    }
    public abstractDirContext(Hashtable<?, ?> environment) {
        this.environment = environment;
    }
    //</editor-fold>
    //<editor-fold desc = "attr">
    // 用来存储绑定的对象
    protected  Map<String, Object> bindings = new HashMap<>();
    // 创建DirContext时，可传入一些环境信息
    protected  Hashtable<?, ?> environment =null;
    // 文件根目录
    protected String root = "";
    //
    //</editor-fold>
    //<editor-fold desc = "getter&setter">
    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        if (root == null) {
            throw new NullPointerException("root is null");
        }
        this.root = root;
    }
    //</editor-fold>
    //<editor-fold desc = "抽象方法">
    public abstract Object lookup(String name) throws NamingException;
    public abstract void bind(String name, Object obj, Attributes attrs) throws NamingException;
    public abstract void rebind(String name, Object obj, Attributes attrs) throws NamingException;
    public abstract void unbind(String name) throws NamingException;
    public abstract void rename(String oldName, String newName) throws NamingException;
    public abstract NamingEnumeration<NameClassPair> list(String name) throws NamingException;
    //</editor-fold>
    //<editor-fold desc = "方法重载的源方法">
    @Override
    public Object lookup(Name name) throws NamingException {
        return lookup(name.toString());
    }
    @Override
    public void bind(Name name, Object obj)
            throws NamingException {
        bind(name.toString(), obj);
    }
    @Override
    public void bind(String name, Object obj)
            throws NamingException {
        bind(name, obj, null);
    }
    @Override
    public void bind(Name name, Object obj, Attributes attrs)
            throws NamingException {
        bind(name.toString(), obj, attrs);
    }
    public void rebind(Name name, Object obj) throws NamingException {
        rebind(name.toString(), obj);
    }
    public void rebind(String name, Object obj) throws NamingException {
        rebind(name, obj, null);
    }
    public void unbind(Name name) throws NamingException {
        unbind(name.toString());
    }
    public void rename(Name oldName, Name newName)
            throws NamingException {
        rename(oldName.toString(), newName.toString());
    }
    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return null;
    }

    //</editor-fold>
    //<editor-fold desc = "不实现">
    @Override
    public DirContext getSchema(Name name) throws NamingException {
        return null;
    }

    @Override
    public DirContext getSchema(String name) throws NamingException {
        return null;
    }
    //</editor-fold>
    @Override
    public Attributes getAttributes(Name name) throws NamingException {
        return null;
    }

    @Override
    public Attributes getAttributes(String name) throws NamingException {
        return null;
    }

    @Override
    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
        return null;
    }

    @Override
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        return null;
    }

    @Override
    public void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException {

    }

    @Override
    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {

    }

    @Override
    public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException {

    }

    @Override
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {

    }

    @Override
    public void rebind(Name name, Object obj, Attributes attrs) throws NamingException {

    }

    @Override
    public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException {
        return null;
    }

    @Override
    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        return null;
    }



    @Override
    public DirContext getSchemaClassDefinition(Name name) throws NamingException {
        return null;
    }

    @Override
    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return null;
    }



    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return null;
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {

    }

    @Override
    public void destroySubcontext(String name) throws NamingException {

    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        return null;
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        return null;
    }

    @Override
    public Object lookupLink(Name name) throws NamingException {
        return null;
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
        return null;
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        return null;
    }

    @Override
    public NameParser getNameParser(String name) throws NamingException {
        return null;
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        return null;
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        return "";
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return null;
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        return null;
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return null;
    }

    @Override
    public void close() throws NamingException {

    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return "";
    }

}
