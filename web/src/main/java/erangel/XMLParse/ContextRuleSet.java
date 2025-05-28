package erangel.XMLParse;

import erangel.core.DefaultContext;

import static erangel.base.Const.commonCharacters.SOLIDUS;
import static erangel.base.Const.confInfo.CONTEXT;
import static erangel.base.Const.confInfo.LOADER;

public class ContextRuleSet extends RuleSet {
    private final String prefix;

    public ContextRuleSet(String prefix) {
        this.prefix = (prefix != null && !prefix.endsWith(SOLIDUS)) ? prefix + SOLIDUS : prefix;
    }

    @Override
    public void addRuleInstances(MiniDigester digester) {
        // todo
        digester.addRule(prefix + CONTEXT, new ObjectCreateRule(DefaultContext.class));
        digester.addRule(prefix + CONTEXT, new SetPropertiesRule());
        digester.addRule(prefix + CONTEXT, new CopyClassLoaderFromParentRule(digester));
        digester.addRule(prefix + CONTEXT, new CreateWebAppLoaderRule("erangel.loader.WebAppLoader", digester));
        digester.addRule(prefix + CONTEXT + LOADER, new SetPropertiesRule());
        digester.addRule(prefix + CONTEXT + LOADER, new SetNextRuleAccessible("setLoader"));
        // 添加默认监听器
        digester.addRule(prefix + CONTEXT, new InnerListenerRule("erangel.listener.InnerContextListener"));

    }
}
