package erangel.xml;

import erangel.core.DefaultContext;
import erangel.core.DefaultEndpoint;
import erangel.filter.FilterDef;
import erangel.filter.FilterMap;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.List;

public class WebRuleSet {

    public void addRuleInstances(MiniDigester d) {
        /* 0. <web-app> → DefaultContext 仅供测试*/
       //  d.addRule("web-app", new ObjectCreateRule(DefaultContext.class));

        /* ---------- <display‑name> ---------- */
        d.addCallMethod("web-app/display-name", "setDisplayName");
        /* ---------- <servlet> ---------- */
        d.addRule("web-app/servlet", new endpointCreateRule());
        d.addCallMethod("web-app/servlet/servlet-name", "setName");
        d.addCallMethod("web-app/servlet/servlet-class", "setServletClass");
        d.addRule("web-app/servlet", new SetNextRuleAccessible("addChild"));
        /* ---------- <servlet‑mapping> ---------- */
        List<String> smArgs = new ArrayList<>();
        d.addRule("web-app/servlet-mapping/url-pattern", new CallParamRule(smArgs, 0));              // 收 url‑pattern
        d.addRule("web-app/servlet-mapping/servlet-name", new CallParamRule(smArgs, 1));              // 收 servlet‑name
        d.addRule("web-app/servlet-mapping", new CallMethodRule("addServletMapping",     // 调用方法
                smArgs,
                String.class,
                String.class));

        /* ---------- <filter> ---------- */
        d.addRule("web-app/filter", new ObjectCreateRule(FilterDef.class));
        d.addCallMethod("web-app/filter/filter-name", "setFilterName");   // 单参数直接简写
        d.addCallMethod("web-app/filter/filter-class", "setFilterClass");
        d.addRule("web-app/filter", new SetNextRuleAccessible("addFilterDef"));

        /* ---------- <filter‑mapping> ---------- */
        d.addRule("web-app/filter-mapping", new ObjectCreateRule(FilterMap.class));
        d.addCallMethod("web-app/filter-mapping/filter-name", "setFilterName");
        d.addCallMethod("web-app/filter-mapping/servlet-name", "setServletName");
        d.addCallMethod("web-app/filter-mapping/url-pattern", "setUrlPattern");
        d.addRule("web-app/filter-mapping", new SetNextRuleAccessible("addFilterMap"));

        /* ---------- <listener> ---------- */
        List<String> listenerArgs = new ArrayList<>();
        d.addRule("web-app/listener/listener-class", new CallParamRule(listenerArgs, 0));
        d.addRule("web-app/listener",
                new CallMethodRule("addApplicationListener",
                        listenerArgs,
                        String.class));
    }

    private static class endpointCreateRule implements Rule {
        @Override
        public void begin(String path, Attributes attrs, MiniDigester d) {
            // 栈底是外部 push 的 DefaultContext
            DefaultContext ctx = d.peek();
            DefaultEndpoint endpoint = (DefaultEndpoint) ctx.createEndpoint();
            d.push(endpoint);
        }
    }

}
