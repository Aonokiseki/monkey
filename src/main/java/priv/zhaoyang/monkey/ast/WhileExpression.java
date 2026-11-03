package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

public class WhileExpression implements Expression {

    private Token token;
    private Expression condition;
    private BlockStatement body;

    public WhileExpression(Token token){
        this.token = token;
    }
    public Token token(){
        return token;
    }
    public Expression condition(){
        return condition;
    }
    public BlockStatement body(){
        return body;
    }
    public WhileExpression putCondition(Expression condition){
        this.condition = condition;
        return this;
    }
    public WhileExpression putBody(BlockStatement body){
        this.body = body;
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

        sb.append(")").append("{");

        if (body != null) {
            sb.append(body);
        }

        sb.append("}");

        return sb.toString();
    }
}
