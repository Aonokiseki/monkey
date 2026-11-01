package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

public class Identifier implements Expression{

    private final Token token;
    private final String value;

    public Identifier(Token token, String value){
        this.token = token;
        this.value = value;
    }

    public Token token(){
        return token;
    }
    public String value(){
        return value;
    }

    @Override
    public String tokenLiteral() {
        return this.token.literal;
    }

    @Override
    public String toString(){
        return this.value;
    }
}
