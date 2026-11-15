package priv.zhaoyang.monkey.object;

import java.util.Objects;

public class StringObject implements MonkeyObject, Hashable{

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

    @Override
    public int hashCode(){
        return Objects.hash(value);
    }

    @Override
    public boolean equals(Object object){
        if (this == object)
            return true;
        if (!(object instanceof StringObject))
            return false;
        StringObject another = (StringObject) object;
        return this.value.equals(another.value);
    }
}
