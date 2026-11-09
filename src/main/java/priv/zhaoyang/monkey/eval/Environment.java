package priv.zhaoyang.monkey.eval;

import priv.zhaoyang.monkey.object.MonkeyObject;

import java.util.HashMap;
import java.util.Map;

public class Environment {

    private final Map<String, MonkeyObject> dict;
    /* 这里应该还有一个用于闭包的外部环境, 以后的 */

    public Environment(){
        this.dict = new HashMap<>();
    }

    public MonkeyObject put(String name, MonkeyObject monkeyObject){
        return this.dict.put(name, monkeyObject);
    }

    public boolean contains(String name){
        return this.dict.containsKey(name);
    }

    public MonkeyObject get(String name, MonkeyObject defaultValue){
        return this.dict.getOrDefault(name, defaultValue);
    }
}
