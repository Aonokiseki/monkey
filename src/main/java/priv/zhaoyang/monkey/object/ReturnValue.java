package priv.zhaoyang.monkey.object;

public class ReturnValue implements MonkeyObject{

    private final MonkeyObject value;

    public ReturnValue(MonkeyObject value){
        this.value = value;
    }

    public MonkeyObject value(){
        return this.value;
    }

    @Override
    public ObjectType type() {
        return this.value.type();
    }

    @Override
    public String inspect() {
        return this.value.inspect();
    }
}
