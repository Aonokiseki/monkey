package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

public class PrefixExpression implements Expression{

    private Token token;
    /* 原书上写着String类型就行, 这里是不是我想复杂了?
    private Expression symbol;
    */
    private String operator;
    private Expression expression;

    public PrefixExpression(Token token){
        this.token = token;
    }

    public PrefixExpression putOperator(String operator){
        this.operator = operator;
        return this;
    }

    public PrefixExpression putExpression(Expression expression){
        this.expression = expression;
        return this;
    }

    public Token token(){
        return token;
    }
    public String operator(){
        return operator;
    }
    public Expression expression(){
        return this.expression;
    }

    @Override
    public String tokenLiteral() {
        return token.literal;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append("(");

        if (operator != null)
            sb.append(operator);

        if (expression != null)
            sb.append(expression);

        sb.append(")");

        return sb.toString();
    }
}
