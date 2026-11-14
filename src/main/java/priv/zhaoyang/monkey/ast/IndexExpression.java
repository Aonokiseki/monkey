package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

/* 索引表达式有这么几种:
 * [1,2,3][1] 是2
 * let a = [1,2,3]; a[1] 是2
 *  */

public class IndexExpression implements Expression{

    private Token token;
    private Expression target;
    private Expression index;

    public IndexExpression(Token token){
        this.token = token;
    }
    public IndexExpression putTarget(Expression target){
        this.target = target;
        return this;
    }
    public IndexExpression putIndex(Expression index){
        this.index = index;
        return this;
    }
    public Expression target(){
        return this.target;
    }
    public Expression index(){
        return this.index;
    }

    @Override
    public String tokenLiteral() {
        return this.token.literal;
    }
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        if (target != null){
            sb.append(target.toString());
        }
        sb.append("[");
        if (index != null){
            sb.append(index.toString());
        }
        sb.append("]");

        return sb.toString();
    }
}
