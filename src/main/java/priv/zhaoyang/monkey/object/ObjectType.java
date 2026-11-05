package priv.zhaoyang.monkey.object;

public enum ObjectType {

    INTEGER("INTEGER"),
    BOOLEAN("BOOLEAN"),
    NULL("NULL"),
    ;


    private String name;

    private ObjectType(String name){
        this.name = name;
    }
}
