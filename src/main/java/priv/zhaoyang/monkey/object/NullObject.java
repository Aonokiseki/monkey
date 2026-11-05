package priv.zhaoyang.monkey.object;

public enum NullObject implements MonkeyObject {

    NULL;

    public static NullObject nil() {
        return NULL;
    }

    @Override
    public ObjectType type() {
        return ObjectType.NULL;
    }

    @Override
    public String inspect() {
        return "null";
    }
}
