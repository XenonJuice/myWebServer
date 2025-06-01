package erangel.XMLParse;

import erangel.core.DefaultHost;

import static erangel.base.Const.commonCharacters.SOLIDUS;

public class HostRuleSet extends RuleSet {
    private final String prefix;

    public HostRuleSet(String prefix) {
        this.prefix = (prefix != null && !prefix.endsWith(SOLIDUS)) ? prefix + SOLIDUS : prefix;
    }

    @Override
    public void addRuleInstances(MiniDigester digester) {
        digester.addRule(prefix + "Host", new ObjectCreateRule(DefaultHost.class));
        digester.addRule(prefix + "Host", new SetPropertiesRule());
        digester.addRule(prefix + "Host", new CopyClassLoaderFromParentRule(digester));
        // 添加默认监听器
        digester.addRule(prefix + "Host", new InnerListenerRule("erangel.listener.InnerHostListener"));


    }
}