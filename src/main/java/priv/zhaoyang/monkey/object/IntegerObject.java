package priv.zhaoyang.monkey.object;

import java.util.Objects;

public class IntegerObject implements MonkeyObject, Hashable {

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

    @Override
    public int hashCode(){
        return Objects.hash(value);
    }

    @Override
    public boolean equals(Object object){
        if (object == this)
            return true;
        if (!(object instanceof IntegerObject))
            return false;
        IntegerObject another = (IntegerObject) object;
        return this.value == another.value;
    }
}
