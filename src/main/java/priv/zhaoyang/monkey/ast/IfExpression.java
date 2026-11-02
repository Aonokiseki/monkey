package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

/*
 * if (expression) {
 *    expression;
 *    expression;
 *    ...  -> 想起来了, 应该组装成BlockStatement
 * } else {
 *    expression;
 *    expression;
 *    ...
 * }
 * */
public class IfExpression implements Expression {

    private Token token;
    private Expression condition;
    private BlockStatement consequence;
    private BlockStatement alternative;

    public IfExpression(Token token){
        this.token = token;
    }
    public Token token(){
        return token;
    }
    public Expression condition(){
        return condition;
    }
    public BlockStatement consequence(){
        return this.consequence;
    }
    public BlockStatement alternative(){
        return this.alternative;
    }

    public IfExpression putCondition(Expression condition){
        this.condition = condition;
        return this;
    }
    public IfExpression putConsequence(BlockStatement consequence){
        this.consequence = consequence;
        return this;
    }
    public IfExpression putAlternative(BlockStatement alternative){
        this.alternative = alternative;
        return this;
    }

    @Override
    public String tokenLiteral() {
        return token.literal;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append(token.literal).append("(");

        if (condition != null){
            sb.append(condition);
        }

        sb.append("){");

        if (consequence != null) {
            sb.append(consequence);
        }

        sb.append("}");

        if (alternative != null) {
            sb.append(" else {").append(alternative).append("}");
        }

        return sb.toString();
    }
}
