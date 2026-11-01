package priv.zhaoyang.monkey.token;

public enum TokenType {
    ILLEGAL("ILLEGAL"),
    EOF("EOF"),

    /* 标识符 */
    IDENT("IDENT"), //比如变量名
    INT("INT"), //数字

    /* 运算符 */
    ASSIGN("="),
    PLUS("+"),
    MINUS("-"),
    BANG("!"),
    ASTERISK("*"),
    SLASH("/"),

    LT("<"),
    GT(">"),

    EQ("=="),
    NOT_EQ("!="),

    /* 分隔符 */
    COMMA(","),
    SEMICOLON(";"),
    LPAREN("("),
    RPAREN(")"),
    LBRACE("{"),
    RBRACE("}"),

    /* 关键字 */
    FUNCTION("FUNCTION"),
    LET("LET"),
    TRUE("TRUE"),
    FALSE("FALSE"),
    IF("IF"),
    ELSE("ELSE"),
    RETURN("RETURN"),
    ;

    private String literal;

    private TokenType(String literal){
        this.literal = literal;
    }

    public String literal(){
        return this.literal;
    }
}
