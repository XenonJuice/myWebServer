package erangel.xml;

import erangel.core.DefaultContext;
import erangel.filter.FilterDef;
import erangel.filter.FilterMap;

import java.util.ArrayList;
import java.util.List;

public class WebRuleSet {

    public void addRuleInstances(MiniDigester d) {

        /* 0. <web-app> → DefaultContext */
        d.addRule("web-app", new ObjectCreateRule(DefaultContext.class));

        /* ---------- 1. <servlet‑mapping> ---------- */
        List<String> smArgs = new ArrayList<>();
        d.addRule("web-app/servlet-mapping/url-pattern", new CallParamRule(smArgs, 0));              // 收 url‑pattern
        d.addRule("web-app/servlet-mapping/servlet-name", new CallParamRule(smArgs, 1));              // 收 servlet‑name
        d.addRule("web-app/servlet-mapping", new CallMethodRule("addServletMapping",     // 调用方法
                smArgs,
                String.class,
                String.class));

        /* ---------- 2. <filter> ---------- */
        d.addRule("web-app/filter", new ObjectCreateRule(FilterDef.class));
        d.addCallMethod("web-app/filter/filter-name", "setFilterName");   // 单参数直接简写
        d.addCallMethod("web-app/filter/filter-class", "setFilterClass");
        d.addRule("web-app/filter", new SetNextRule("addFilterDef"));

        /* ---------- 3. <filter‑mapping> ---------- */
        d.addRule("web-app/filter-mapping", new ObjectCreateRule(FilterMap.class));
        d.addCallMethod("web-app/filter-mapping/filter-name", "setFilterName");
        d.addCallMethod("web-app/filter-mapping/servlet-name", "setServletName");
        d.addCallMethod("web-app/filter-mapping/url-pattern", "setUrlPattern");
        d.addRule("web-app/filter-mapping", new SetNextRule("addFilterMap"));

        /* ---------- 4. <listener> ---------- */
        List<String> listenerArgs = new ArrayList<>();
        d.addRule("web-app/listener/listener-class", new CallParamRule(listenerArgs, 0));
        d.addRule("web-app/listener",
                new CallMethodRule("addApplicationListener",
                        listenerArgs,
                        String.class));
    }
}
