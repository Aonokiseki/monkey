package priv.zhaoyang.monkey.object;

public class StringObject implements MonkeyObject{

    private final String value;

    public StringObject(String value){
        this.value = value;
    }

    public String value(){
        return this.value;
    }

    @Override
    public ObjectType type() {
        return ObjectType.STRING;
    }

    @Override
    public String inspect() {
        return this.value;
    }

    @Override
    public String toString(){
        return this.value;
    }
}
