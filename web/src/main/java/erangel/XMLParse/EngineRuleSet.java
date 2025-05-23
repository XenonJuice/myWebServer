package erangel.XMLParse;

import erangel.core.DefaultEngine;
import erangel.listener.InnerEngineListener;

public class EngineRuleSet extends RuleSet{
    private final String prefix;

    public EngineRuleSet(String prefix) {
        this.prefix = (prefix != null && !prefix.endsWith("/")) ? prefix + "/" : prefix;
    }

    @Override
    public void addRuleInstances(MiniDigester digester) {
        // <Engine>自身
        digester.addRule(prefix + "Engine", new ObjectCreateRule(DefaultEngine.class));
        digester.addRule(prefix + "Engine", new SetPropertiesRule());
        digester.addRule(prefix + "Engine", new SetNextRuleAccessible("setVas"));
        // 添加默认监听器
        digester.addRule(prefix + "Engine", new InnerListenerRule("erangel.listener.InnerEngineListener"));
    }
}
