package priv.zhaoyang.monkey.object;

import java.util.List;

public class ArrayObject implements MonkeyObject{

    private final List<MonkeyObject> values;

    public ArrayObject(List<MonkeyObject> values){
        this.values = values;
    }

    public List<MonkeyObject> values(){
        return this.values;
    }

    @Override
    public ObjectType type() {
        return ObjectType.ARRAY;
    }

    @Override
    public String inspect() {
        /* 递归用元素的 inspect() 拼接，与原书一致：[[1, 2], [3, 4]] 而不是哈希码 */
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(values.get(i).inspect());
        }
        sb.append("]");
        return sb.toString();
    }
}
