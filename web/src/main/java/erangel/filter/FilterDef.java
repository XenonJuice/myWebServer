package erangel.filter;

import java.util.HashMap;
// 过滤器抽象
public class FilterDef {
        private final HashMap<String, String> params = new HashMap<>();
        private String filterName;
        private String filterClass;

        public String getFilterName() {
            return filterName;
        }

        public String getFilterClass() {
            return filterClass;
        }

        public HashMap<String, String> getParams() {
            return params;
        }

        public void addParam(String name, String value) {
            params.put(name, value);

    }



}


