package priv.zhaoyang.monkey.parser;

public enum Precedence {

    LOWEST(1),
    EQUAL(2),
    LESSGREATER(3),
    SUM(4),
    PRODUCT(5),
    PREFIX(6),
    CALL(7),
    INDEX(8),
    ;

    private int value;

    Precedence(int value){
        this.value = value;
    }

    public int value(){
        return value;
    }

    @Override
    public String toString(){
        return this.name();
    }
}
