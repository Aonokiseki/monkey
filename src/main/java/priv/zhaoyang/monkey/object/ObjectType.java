package priv.zhaoyang.monkey.object;

public enum ObjectType {

    /* 目前这些类型会挂上Hashable, 允许散列, 也即允许作为map的key */
    INTEGER("INTEGER"),
    BOOLEAN("BOOLEAN"),
    STRING("STRING"),

    /* 这些都不允许Hashable */
    NULL("NULL"),
    ARRAY("ARRAY"),
    MAP("MAP"),
    FUNCTION("FUNCTION"),
    ;


    private String name;

    private ObjectType(String name){
        this.name = name;
    }
}
