package priv.zhaoyang.monkey.object;

public enum BooleanObject implements MonkeyObject, Hashable/* 布尔值真的有必要做散列么, new HashMap<Boolean, String>()??? */{

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

    @Override
    public String toString(){
        return String.valueOf(value);
    }
}
