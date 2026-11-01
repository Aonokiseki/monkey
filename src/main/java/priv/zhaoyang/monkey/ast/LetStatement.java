package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

public class LetStatement implements Statement {

    private Token token;
    private Identifier identifier;
    private Expression expression;

    public LetStatement(Token token){
        this.token = token;
    }
    public LetStatement putIdentifier(Identifier identifier){
        this.identifier = identifier;
        return this;
    }
    public LetStatement putExpression(Expression expression){
        this.expression = expression;
        return this;
    }

    public Token token(){
        return this.token;
    }
    public Identifier identifier(){
        return this.identifier;
    }
    public Expression expression(){
        return this.expression;
    }

    @Override
    public String tokenLiteral() {
        return this.token.literal;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append(token.literal).append(" ");

        if (identifier != null) {
            /* 直接写identifier, 交给底层的toString()完成即可 */
            sb.append(identifier).append(" = ");
        }
        if (expression != null){
            sb.append(expression);
        }

        sb.append(";");

        return sb.toString();
    }
}
