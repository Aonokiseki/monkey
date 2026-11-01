package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

public class IntegerLiteral implements Expression{

    private Token token;
    private long value;

    public IntegerLiteral(Token token){
        this.token = token;
    }

    public Token token(){
        return token;
    }

    public long value(){
        return this.value;
    }

    public IntegerLiteral putValue(long value){
        this.value = value;
        return this;
    }

    @Override
    public String tokenLiteral() {
        return token.literal;
    }

    /* 擦, 又忘了toString() */
    @Override
    public String toString(){
        return token.literal;
    }
}
