package erangel.webResource;

import erangel.Const;

import javax.naming.*;
import javax.naming.directory.*;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * DirContext的基础实现
 *
 * @author LILINJIAN
 * @version 2025/2/26
 */
public abstract class abstractDirContext implements DirContext {
    //<editor-fold desc = "attr">

    // 用来存储绑定的对象
    protected Map<String, Object> bindings = new HashMap<>();
    // 创建DirContext时，可传入一些环境信息
    protected Hashtable<String, Object> environment = null;
    // 文件根目录
    protected String root = "";

    //</editor-fold>
    //<editor-fold desc = "构造器">
    public abstractDirContext() {
        this.environment = new Hashtable<String, Object>();
    }

    public abstractDirContext(Hashtable<String, Object> environment) {
        this.environment = environment;
    }

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

    public abstract NamingEnumeration<Binding> listBindings(String name) throws NamingException;

    public abstract void destroySubcontext(String name) throws NamingException;

    public abstract DirContext createSubcontext(String name, Attributes attrs) throws NamingException;

    public abstract DirContext getSchema(String name) throws NamingException;

    public abstract DirContext getSchemaClassDefinition(String name) throws NamingException;

    public abstract NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException;

    public abstract NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException;

    public abstract NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException;

    public abstract NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException;

    public abstract Attributes getAttributes(String name, String[] attrIds) throws NamingException;

    public abstract void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException;

    public abstract void modifyAttributes(String name, ModificationItem[] mods) throws NamingException;

    public abstract String getNameInNamespace() throws NamingException;

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

    public void rebind(Name name, Object obj, Attributes attrs)
            throws NamingException {
        rebind(name.toString(), obj, attrs);
    }

    public void unbind(Name name) throws NamingException {
        unbind(name.toString());
    }

    public void rename(Name oldName, Name newName)
            throws NamingException {
        rename(oldName.toString(), newName.toString());
    }

    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return list(name.toString());
    }

    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return listBindings(name.toString());
    }

    public void destroySubcontext(Name name)
            throws NamingException {
        destroySubcontext(name.toString());
    }

    public Context createSubcontext(Name name)
            throws NamingException {
        return createSubcontext(name.toString());
    }

    public Context createSubcontext(String name)
            throws NamingException {
        return createSubcontext(name, null);
    }

    public DirContext createSubcontext(Name name, Attributes attrs)
            throws NamingException {
        return createSubcontext(name.toString(), attrs);
    }

    public DirContext getSchema(Name name)
            throws NamingException {
        return getSchema(name.toString());
    }

    public DirContext getSchemaClassDefinition(Name name)
            throws NamingException {
        return getSchemaClassDefinition(name.toString());
    }

    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn)
            throws NamingException {
        return search(name.toString(), matchingAttributes, attributesToReturn);
    }

    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes)
            throws NamingException {
        return search(name.toString(), matchingAttributes);
    }

    public NamingEnumeration<SearchResult> search
            (Name name, String filter, SearchControls cons)
            throws NamingException {
        return search(name.toString(), filter, cons);
    }

    public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons)
            throws NamingException {
        return search(name.toString(), filterExpr, filterArgs, cons);
    }

    public Attributes getAttributes(Name name)
            throws NamingException {
        return getAttributes(name.toString());
    }

    public Attributes getAttributes(String name)
            throws NamingException {
        return getAttributes(name, null);
    }

    public Attributes getAttributes(Name name, String[] attrIds)
            throws NamingException {
        return getAttributes(name.toString(), attrIds);
    }

    public void modifyAttributes(Name name, int mod_op, Attributes attrs)
            throws NamingException {
        modifyAttributes(name.toString(), mod_op, attrs);
    }

    public void modifyAttributes(Name name, ModificationItem[] mods)
            throws NamingException {
        modifyAttributes(name.toString(), mods);
    }

    // 将单独的名称转换为一个名称组件
    public NameParser getNameParser(Name name) throws NamingException {
        return n -> new CompositeName(n);
    }

    public NameParser getNameParser(String name) throws NamingException {
        return n -> new CompositeName(n);
    }

    // 拼接为完整的名字
    public Name composeName(Name name, Name prefix) throws NamingException {
        prefix = (Name) name.clone();
        return prefix.addAll(name);
    }

    public String composeName(String name, String prefix) throws NamingException {
        return prefix + Const.commonCharacters.SOLIDUS + name;
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return environment.put(propName, propVal);
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        return environment.remove(propName);
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return environment;
    }

    @Override
    public void close() throws NamingException {
        environment.clear();
    }
    //</editor-fold>

    //<editor-fold desc = "不实现">
    //</editor-fold>


}
