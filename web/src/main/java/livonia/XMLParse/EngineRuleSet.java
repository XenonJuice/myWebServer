package livonia.XMLParse;

import livonia.core.DefaultEngine;

import static livonia.base.Const.commonCharacters.SOLIDUS;
import static livonia.base.Const.confInfo.ENGINE;

public class EngineRuleSet extends RuleSet {
    private final String prefix;

    public EngineRuleSet(String prefix) {
        this.prefix = (prefix != null && !prefix.endsWith(SOLIDUS)) ? prefix + SOLIDUS : prefix;
    }

    @Override
    public void addRuleInstances(MiniDigester digester) {
        // <Engine>自身
        digester.addRule(prefix + ENGINE, new ObjectCreateRule(DefaultEngine.class));
        digester.addRule(prefix + ENGINE, new SetPropertiesRule());
        // 添加默认监听器
        digester.addRule(prefix + ENGINE, new InnerListenerRule("livonia.listener.InnerEngineListener"));
        digester.addRule(prefix + ENGINE, new SetNextRuleAccessible("setVas"));

    }
}
