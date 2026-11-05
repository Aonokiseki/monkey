package priv.zhaoyang.monkey.object;

public enum BooleanObject implements MonkeyObject {

    TRUE(true),
    FALSE(false)
    ;

    private final boolean value;

    private BooleanObject(boolean value){
        this.value = value;
    }

    public boolean value(){
        return this.value;
    }

    @Override
    public ObjectType type() {
        return ObjectType.BOOLEAN;
    }

    @Override
    public String inspect() {
        return String.valueOf(value);
    }
}
