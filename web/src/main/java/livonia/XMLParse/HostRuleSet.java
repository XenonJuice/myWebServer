package livonia.XMLParse;

import livonia.core.DefaultHost;

import static livonia.base.Const.commonCharacters.SOLIDUS;

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
        digester.addRule(prefix + "Host", new SetNextRuleAccessible("addChild"));
        // 添加默认监听器
        digester.addRule(prefix + "Host", new InnerListenerRule("livonia.listener.InnerHostListener"));


    }
}