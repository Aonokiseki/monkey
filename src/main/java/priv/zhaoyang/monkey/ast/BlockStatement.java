package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

import java.util.Collections;
import java.util.List;

public class BlockStatement implements Statement{

    private Token token;
    private List<Statement> statements;

    public BlockStatement(Token token){
        this.token = token;
    }
    public Token token(){
        return token;
    }
    public List<Statement> statements(){
        return Collections.unmodifiableList(statements);
    }

    public BlockStatement putStatements(List<Statement> statements){
        this.statements = statements;
        return this;
    }

    @Override
    public String tokenLiteral() {
        return this.token.literal;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        if (statements != null){
            for (Statement statement : statements){
                sb.append(statement).append(";");
            }
        }

        return sb.toString();
    }
}
