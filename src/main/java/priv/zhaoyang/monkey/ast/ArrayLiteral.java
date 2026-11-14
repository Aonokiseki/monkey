package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArrayLiteral implements Expression{

    private Token token;
    private List<Expression> expressionList;

    public ArrayLiteral(Token token){
        this.token = token;
        this.expressionList = new ArrayList<>();
    }

    public ArrayLiteral putExpressionList(List<Expression> expressionList){
        this.expressionList = expressionList;
        return this;
    }

    public Token token(){
        return this.token;
    }

    public List<Expression> expressionList(){
        return Collections.unmodifiableList(expressionList);
    }

    @Override
    public String tokenLiteral() {
        return this.token.literal;
    }

    @Override
    public String toString(){
        return this.expressionList.toString();
    }
}
