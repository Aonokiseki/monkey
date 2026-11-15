package priv.zhaoyang.monkey.object;

import java.util.Collections;
import java.util.Map;

public class MapObject implements MonkeyObject{

    private final Map<MonkeyObject, MonkeyObject> map;

    public MapObject(Map<MonkeyObject, MonkeyObject> map){
        this.map = map;
    }

    public Map<MonkeyObject, MonkeyObject> map(){
        return Collections.unmodifiableMap(map);
    }

    @Override
    public ObjectType type() {
        return ObjectType.MAP;
    }

    @Override
    public String inspect() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        sb.append("{");
        for (Map.Entry<MonkeyObject, MonkeyObject> e : map.entrySet()){
            sb.append(e.getKey().inspect()).append(": ").append(e.getValue().inspect());
            i++;
            if (i < map.size()){
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
