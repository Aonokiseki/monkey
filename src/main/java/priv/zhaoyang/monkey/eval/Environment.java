package priv.zhaoyang.monkey.eval;

import priv.zhaoyang.monkey.object.MonkeyObject;

import java.util.HashMap;
import java.util.Map;

public class Environment {

    private final Map<String, MonkeyObject> dict;
    /* 闭包时, 读取函数外部变量的外部变量表 */
    private final Environment outer;

    public Environment(Environment outer){
        this.dict = new HashMap<>();
        this.outer = outer;
    }

    /* 始终put到自己的变量表 */
    public MonkeyObject put(String name, MonkeyObject monkeyObject){
        return this.dict.put(name, monkeyObject);
    }

    /* ==================== 查表（递归）踩坑记录 ====================
     * 1. get 的坑：一开始写成 `if (contains(name)) return dict.getOrDefault(name, default);`
     *    contains 会递归外层（“自己或外层有没有”），但 getOrDefault 只在自己 dict 里取，
     *    于是闭包捕获的外层变量被取成了 default（NULL），函数体里的 a、c 全变 null。
     *    正确：先查自己，查不到再递归外层，到底都没有才给 default。
     * 2. contains 的坑：一开始 `dict.containsKey(name) || outer.contains(name)` 没判 outer==null，
     *    顶层环境（outer=null）查未定义标识符会 NPE。正确：显式判 null 作为递归终止条件。
     * 本质：写“带状态/带边界的递归”先想清楚【终止条件(base case)】，这里就是 outer==null。 */
    public boolean contains(String name){
        if (this.dict.containsKey(name)){
            return true;
        }
        if (this.outer != null){
            return this.outer.contains(name);
        }
        return false;
    }

    public MonkeyObject get(String name, MonkeyObject defaultValue){
        if (this.dict.containsKey(name)){
            return dict.get(name);
        }
        if (this.outer != null){
            return this.outer.get(name, defaultValue);
        }
        return defaultValue;
    }
}
