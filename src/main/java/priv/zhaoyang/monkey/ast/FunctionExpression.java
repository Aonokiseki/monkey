package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

import java.util.Collections;
import java.util.List;

public class FunctionExpression implements Expression{
    /* fn(x,y,z){blockStatement} */

    private Token token;
    private List<Identifier> parameters;
    private BlockStatement body;

    public FunctionExpression(Token token){
        this.token = token;
    }

    public Token token(){
        return token;
    }
    public List<Identifier> parameters(){
        return Collections.unmodifiableList(parameters);
    }
    public BlockStatement body(){
        return body;
    }
    public FunctionExpression putParameters(List<Identifier> parameters){
        this.parameters = parameters;
        return this;
    }

    public FunctionExpression putBody(BlockStatement body){
        this.body = body;
        return this;
    }

    @Override
    public String tokenLiteral() {
        return this.token.literal;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append(token.literal).append("(");

        if (parameters != null){
            for(int i = 0; i < parameters.size(); i++){
                sb.append(parameters.get(i).toString());

                if (i < parameters.size() - 1)
                    sb.append(",");
            }
        }

        sb.append(")").append("{");

        if (body != null) {
            sb.append(body.toString());
        }

        sb.append("}");

        return sb.toString();
    }
}
