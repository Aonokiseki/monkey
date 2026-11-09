package priv.zhaoyang.monkey.object;

public class IntegerObject implements MonkeyObject {

    private final long value;

    public IntegerObject(long value){
        this.value = value;
    }

    public long value(){
        return this.value;
    }

    @Override
    public ObjectType type() {
        return ObjectType.INTEGER;
    }

    @Override
    public String inspect(){
        return String.valueOf(value);
    }

    @Override
    public String toString(){
        return String.valueOf(value);
    }
}
