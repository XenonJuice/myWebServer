package livonia.filter;

import java.util.HashMap;

// 过滤器抽象
public class FilterDef {
    private final HashMap<String, String> params = new HashMap<>();
    private String filterName;
    private String filterClass;

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getFilterClass() {
        return filterClass;
    }

    public void setFilterClass(String filterClass) {
        this.filterClass = filterClass;
    }

    public HashMap<String, String> getParams() {
        return params;
    }

    public void addInitParameter(String name, String value) {
        params.put(name, value);

    }


}


