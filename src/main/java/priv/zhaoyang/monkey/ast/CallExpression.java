package priv.zhaoyang.monkey.ast;

/* expression(expression1,expression2,...) */

import priv.zhaoyang.monkey.token.Token;

import java.util.Collections;
import java.util.List;

public class CallExpression implements Expression{

    private Token token;
    private Expression function;
    private List<Expression> arguments;

    public CallExpression(Token token){
        this.token = token;
    }
    public Token token(){
        return token;
    }
    public Expression function(){
        return function;
    }
    public List<Expression> arguments(){
        return Collections.unmodifiableList(arguments);
    }
    public CallExpression putFunction(Expression function){
        this.function = function;
        return this;
    }
    public CallExpression putArguments(List<Expression> arguments){
        this.arguments = arguments;
        return this;
    }

    @Override
    public String tokenLiteral() {
        return token.literal;
    }
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        if (function != null){
            sb.append(function.toString());
        }

        sb.append("(");

        if (arguments != null){
            for(int i = 0; i < arguments.size(); i++){
                sb.append(arguments.get(i));

                if (i < arguments.size() - 1)
                    sb.append(", ");
            }
        }

        sb.append(")");

        return sb.toString();
    }
}
