package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

public class StringLiteral implements Expression{

    private Token token;
    private String value;

    public StringLiteral(Token token){
        this.token = token;
        this.value = token.literal;
    }

    public StringLiteral putToken(Token token){
        this.token = token;
        this.value = token.literal;
        return this;
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
        return this.token.literal;
    }
}
