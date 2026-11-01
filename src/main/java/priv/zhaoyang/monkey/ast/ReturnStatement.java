package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

public class ReturnStatement implements Statement{

    private Token token;
    private Expression returnExpression;

    public ReturnStatement(Token token){
        this.token = token;
    }

    public ReturnStatement putReturnExpression(Expression returnExpression){
        this.returnExpression = returnExpression;
        return this;
    }

    public Token token(){
        return token;
    }
    public Expression returnExpression(){
        return returnExpression;
    }

    @Override
    public String tokenLiteral() {
        return token.literal;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append(token.literal).append(" ");

        if (returnExpression != null){
            sb.append(returnExpression);
        }

        sb.append(";");

        return sb.toString();
    }
}
