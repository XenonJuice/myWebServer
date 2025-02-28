package erangel.webResource;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import static erangel.Const.Header.CONTENT_LENGTH;
import static erangel.Const.webApp.DATE_FORMATS;

@Deprecated
@SuppressWarnings("unused")
public class ResourcesAttr implements Attributes {


    //</editor-fold>
    //<editor-fold desc = "attr">
    // 属性
    protected Attributes attributes = null;
    // 最后修改时间
    protected long lastModified = -1;
    protected Date lastModifiedDate = null;
    // 创建日期
    protected long creation = -1;
    protected Date creationDate = null;
    // 内容长度
    protected long contentLength = -1;
    // name
    protected String name = null;

    //</editor-fold>
    //<editor-fold desc = "构造器">
    public ResourcesAttr() {
    }

    public ResourcesAttr(Attributes attributes) {
        this.attributes = attributes;
    }

    @Override
    public int size() {
        return attributes.size();
    }

    public Attribute get(String attrID) {
        if (attributes == null) {
            if (attrID.equals(ResourceAttributeType.CREATION_DATE.value)) {
                return new BasicAttribute(ResourceAttributeType.CREATION_DATE.value, getCreationDate());
            } else if (attrID.equals(ResourceAttributeType.ALTERNATE_CREATION_DATE.value)) {
                return new BasicAttribute(ResourceAttributeType.ALTERNATE_CREATION_DATE.getValue(),
                        getCreationDate());
            } else if (attrID.equals(ResourceAttributeType.LAST_MODIFIED.value)) {
                return new BasicAttribute(ResourceAttributeType.LAST_MODIFIED.value,
                        getLastModifiedDate());
            } else if (attrID.equals(CONTENT_LENGTH)) {
                return new BasicAttribute(CONTENT_LENGTH,
                        getContentLength());
            }
        } else {
            return attributes.get(attrID);
        }
        return null;
    }

    @Override
    public Attribute put(String attrID, Object val) {
        if (attributes == null) return null;
        else return attributes.put(attrID, val);
    }

    @Override
    public Attribute put(Attribute attr) {
        if (attributes == null) {
            try {
                return put(attr.getID(), attr.get());
            } catch (NamingException e) {
                return null;
            }
        } else {
            return attributes.put(attr);
        }
    }

    @Override
    public Attribute remove(String attrID) {
        if (attributes == null) {
            return null;
        } else {
            return attributes.remove(attrID);
        }
    }

    //<editor-fold desc = "不做实现">
    @Override
    public NamingEnumeration<? extends Attribute> getAll() {
        return null;
    }

    @Override
    public boolean isCaseIgnored() {
        return false;
    }

    @Override
    public Object clone() {
        return this;
    }

    @Override
    public NamingEnumeration<String> getIDs() {
        return null;
    }

    //</editor-fold>
    //<editor-fold desc = "设置，取得属性用的方法">
    public long getLastModified() {
        if (lastModified != -1) return lastModified;
        if (lastModifiedDate != null) return lastModifiedDate.getTime();
        if (attributes != null) {
            Attribute attribute = attributes.get(ResourceAttributeType
                    .LAST_MODIFIED.value);

            if (attribute != null) {
                try {
                    Object value = attribute.get();
                    if (value instanceof Long) {
                        lastModified = (Long) value;
                    } else if (value instanceof Date) {
                        lastModifiedDate = (Date) value;
                        lastModified = lastModifiedDate.getTime();
                    } else {
                        String str = value.toString();
                        Date result = null;
                        for (int i = 0; (result == null) &&
                                (i < DATE_FORMATS.length); i++) {
                            try {
                                result = DATE_FORMATS[i].parse(str);
                            } catch (ParseException _) {
                                ;
                            }
                            if (result != null) {
                                lastModifiedDate = result;
                                lastModified = lastModifiedDate.getTime();
                            }
                        }
                    }
                } catch (NamingException _) {
                    ;
                }
            }
        }
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        setLastModifiedDate(lastModified);
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
        this.lastModifiedDate = null;
        if (attributes != null) attributes.put(
                ResourceAttributeType.LAST_MODIFIED.value,
                new Date(lastModified));
    }

    public Date getLastModifiedDate() {
        if (lastModifiedDate != null)
            return lastModifiedDate;
        if (lastModified != -1L) {
            lastModifiedDate = new Date(lastModified);
            return lastModifiedDate;
        }
        if (attributes != null) {
            Attribute attribute = attributes.get(
                    ResourceAttributeType.LAST_MODIFIED.value);
            if (attribute != null) {
                try {
                    Object value = attribute.get();
                    if (value instanceof Long) {
                        lastModified = (Long) value;
                        lastModifiedDate = new Date(lastModified);
                    } else if (value instanceof Date) {
                        lastModified = ((Date) value).getTime();
                        lastModifiedDate = (Date) value;
                    } else {
                        String lastModifiedDateValue = value.toString();
                        Date result = null;
                        for (int i = 0; (result == null) &&
                                (i < DATE_FORMATS.length); i++) {
                            try {
                                result =
                                        DATE_FORMATS[i].parse(lastModifiedDateValue);
                            } catch (ParseException _) {
                                ;
                            }
                        }
                        if (result != null) {
                            lastModified = result.getTime();
                            lastModifiedDate = result;
                        }
                    }
                } catch (NamingException _) {
                }
            }
        }
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModified = lastModifiedDate.getTime();
        this.lastModifiedDate = lastModifiedDate;
        if (attributes != null)
            attributes.put(
                    ResourceAttributeType.LAST_MODIFIED.value,
                    lastModifiedDate);
    }

    public long getCreation() {
        if (creation != -1L)
            return creation;
        if (creationDate != null)
            return creationDate.getTime();
        if (attributes != null) {
            Attribute attribute = attributes.get(ResourceAttributeType.CREATION_DATE.value);
            if (attribute != null) {
                try {
                    Object value = attribute.get();
                    if (value instanceof Long) {
                        creation = (Long) value;
                    } else if (value instanceof Date) {
                        creation = ((Date) value).getTime();
                        creationDate = (Date) value;
                    } else {
                        String creationDateValue = value.toString();
                        Date result = null;
                        for (int i = 0; (result == null) &&
                                (i < DATE_FORMATS.length); i++) {
                            try {
                                result = DATE_FORMATS[i].parse(creationDateValue);
                            } catch (ParseException _) {
                                ;
                            }
                        }
                        if (result != null) {
                            creation = result.getTime();
                            creationDate = result;
                        }
                    }
                } catch (NamingException _) {

                }
            }
        }
        return creation;
    }

    public void setCreation(long creation) {
        this.creation = creation;
        this.creationDate = null;
        if (attributes != null)
            attributes.put(
                    ResourceAttributeType.CREATION_DATE.value,
                    new Date(creation));
    }

    public Date getCreationDate() {
        if (creationDate != null)
            return creationDate;
        if (creation != -1L) {
            creationDate = new Date(creation);
            return creationDate;
        }
        if (attributes != null) {
            Attribute attribute = attributes.get(ResourceAttributeType.CREATION_DATE.value);
            if (attribute != null) {
                try {
                    Object value = attribute.get();
                    if (value instanceof Long) {
                        creation = (Long) value;
                        creationDate = new Date(creation);
                    } else if (value instanceof Date) {
                        creation = ((Date) value).getTime();
                        creationDate = (Date) value;
                    } else {
                        String creationDateValue = value.toString();
                        Date result = null;
                        for (int i = 0; (result == null) &&
                                (i < DATE_FORMATS.length); i++) {
                            try {
                                result = DATE_FORMATS[i].parse(creationDateValue);
                            } catch (ParseException _) {
                                ;
                            }
                        }
                        if (result != null) {
                            creation = result.getTime();
                            creationDate = result;
                        }
                    }
                } catch (NamingException _) {
                }
            }
        }
        return creationDate;
    }

    public long getContentLength() {
        if (contentLength != -1L)
            return contentLength;
        if (attributes != null) {
            Attribute attribute = attributes.get(CONTENT_LENGTH);
            if (attribute != null) {
                try {
                    Object value = attribute.get();
                    if (value instanceof Long) {
                        contentLength = (Long) value;
                    } else {
                        try {
                            contentLength = Long.parseLong(value.toString());
                        } catch (NumberFormatException _) {
                        }
                    }
                } catch (NamingException _) {
                }
            }
        }
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
        if (attributes != null)
            attributes.put(
                    ResourceAttributeType.CONTENT_LENGTH.value,
                    contentLength);
    }

    /**
     * 资源的基本属性枚举
     */
    public enum ResourceAttributeType {
        CREATION_DATE("creationdate"),
        ALTERNATE_CREATION_DATE("creation-date"),
        LAST_MODIFIED("getlastmodified"),
        NAME("name"),
        ALTERNATE_NAME("displayname"),
        TYPE("resourcetype"),
        ALTERNATE_TYPE("content-type"),
        SOURCE("source"),
        ALTERNATE_SOURCE("content-source"),
        CONTENT_ENCODING("content-encoding"),
        CONTENT_LANGUAGE("content-language"),
        CONTENT_LENGTH("content-length"),
        ALTERNATE_CONTENT_LENGTH("getcontentlength"),
        CONTENT_TYPE("content-type"),
        IS_COLLECTION("iscollection"),
        IS_HIDDEN("ishidden");

        private final String value;

        ResourceAttributeType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
    //<editor-fold desc = "内部类">
    protected class InternalNamingIterator implements NamingEnumeration<Attribute> {
        private Enumeration<Attribute> enumA;

        protected InternalNamingIterator(CopyOnWriteArrayList<Attribute> entries) {
            Vector<Attribute> vector = new Vector<>(entries);
            this.enumA = vector.elements();
        }

        @Override
        public Attribute next() {
            return nextElement();
        }

        @Override
        public boolean hasMore() {
            return enumA.hasMoreElements();
        }

        @Override
        public void close() throws NamingException {
        }

        @Override
        public boolean hasMoreElements() {
            return enumA.hasMoreElements();
        }

        @Override
        public Attribute nextElement() {
            return enumA.nextElement();
        }
    }
    //</editor-fold>
}
