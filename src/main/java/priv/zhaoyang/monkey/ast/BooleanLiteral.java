package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;
import priv.zhaoyang.monkey.token.TokenType;


public enum BooleanLiteral implements Expression {

    TRUE(new Token(TokenType.TRUE, "true"), true),
    FALSE(new Token(TokenType.FALSE, "false"), false)
    ;

    private Token token;
    private boolean value;

    private BooleanLiteral(Token token, boolean value){
        this.token = token;
        this.value = value;
    }
    public Token token(){
        return token;
    }
    public boolean value(){
        return this.value;
    }

    @Override
    public String tokenLiteral() {
        return this.token.literal;
    }

    @Override
    public String toString(){
        return token.literal;
    }
}
