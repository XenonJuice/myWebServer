package livonia.XMLParse;

import java.util.List;

/* 假设有这样的xml
 * <init-param>
 *   <param-name>characterEncoding</param-name>
 *   <param-value>UTF-8</param-value>
 * </init-param>
 *
 * 希望调用的方法：setInitParam(String name, String value);
 *
 * 首先创建一个容器 List<String> params = new ArrayList<>();
 *
 * 再 digester.addRule("init-param/param-name", new CallParamRule(params, 0));
 *    digester.addRule("init-param/param-value", new CallParamRule(params, 1));
 *
 * 最后 digester.addRule("init-param", new CallMethodRule("setInitParam", params));
 */
public class CallParamRule implements Rule {
    private final List<String> paramList;
    private final int index;

    public CallParamRule(List<String> paramList, int index) {
        this.paramList = paramList;
        this.index = index;
    }

    @Override
    public void body(String path, String text, MiniDigester d) {
        while (paramList.size() <= index) paramList.add(null);
        paramList.set(index, text);
    }
}
