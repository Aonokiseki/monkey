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
        /*
        StringBuilder sb = new StringBuilder();

        for(Statement s : statements){
            sb.append(s.tokenLiteral()).append('\n');
        }

        return sb.toString();
        */
    }
}
