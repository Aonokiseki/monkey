package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

public class ExpressionStatement implements Statement{

    private Token token;
    private Expression expression;

    public ExpressionStatement(Token token){
        this.token = token;
    }

    public ExpressionStatement putExpression(Expression expression){
        this.expression = expression;
        return this;
    }

    public Token token(){
        return this.token;
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
        /*  原书 ExpressionStatement.String() 只返回 Expression.String()（完全委托，不加 token 前缀、不加 ;）
         *  其实是"该表达式的第一个 token"，当标题打印会重复/误导（a + b; 会打成 a a + b;）
         *  移除, 只返回expression
         * */
        return expression != null ? expression.toString() : "";
    }
}
