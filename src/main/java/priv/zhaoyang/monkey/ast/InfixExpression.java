package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

public class InfixExpression implements Expression {

    private Token token;
    private Expression left;
    private String operator;
    private Expression right;

    public InfixExpression(Token token){
        this.token = token;
    }
    public InfixExpression putLeftExpression(Expression left){
        this.left = left;
        return this;
    }
    public InfixExpression putOperator(String operator){
        this.operator = operator;
        return this;
    }
    public InfixExpression putRightExpression(Expression right){
        this.right = right;
        return this;
    }

    public Token token(){
        return this.token;
    }
    public Expression left(){
        return this.left;
    }
    public String operator(){
        return this.operator;
    }
    public Expression right(){
        return this.right;
    }

    @Override
    public String tokenLiteral() {
        return this.token.literal;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append("(");

        if (left != null)
            sb.append(left);

        if (operator != null)
            sb.append(" ").append(operator).append(" ");

        if (right != null)
            sb.append(right);

        sb.append(")");

        return sb.toString();
    }
}
