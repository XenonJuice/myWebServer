package erangel.filter;

import erangel.utils.Decoder;

import java.nio.charset.StandardCharsets;

public class FilterMap {
    //<editor-fold desc = "attr">
    private String filterName = null;
    private String urlPattern = null;
    private String servletName = null;

    //</editor-fold>
    //<editor-fold desc = "getter && setter">
    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = Decoder.decode(urlPattern, StandardCharsets.UTF_8);
    }

    public String getServletName() {
        return servletName;
    }

    public void setServletName(String servletName) {
        this.servletName = servletName;
    }
    //</editor-fold>
}
