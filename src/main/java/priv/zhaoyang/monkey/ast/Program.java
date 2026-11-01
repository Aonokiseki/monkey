package priv.zhaoyang.monkey.ast;

import java.util.List;

public class Program implements Node{

    private List<Statement> statements;

    public Program(){}

    public Program putStatements(List<Statement> statements){
        this.statements = statements;
        return this;
    }

    public List<Statement> statements(){
        return this.statements;
    }

    @Override
    public String tokenLiteral() {
        return statements.isEmpty() ? "" : statements.get(0).tokenLiteral();

        /* 官方实现只打印第一句, 我们先按自己的方式来 */
        /* 完整打印放到toString()中 */
    }

    /* 原书go语言的实现, 要求实现了String()才能被当作node,
       但java自带Object.toString(), 我决定不把toString()当作接口方法了
     */
    @Override
    public String toString(){
        /* 终于，在补充 ExpressionStatement时，要完整打印AST了 */
        StringBuilder sb = new StringBuilder();

        for(Statement s : statements){
            sb.append(s.toString());
        }

        return sb.toString();
    }
}
