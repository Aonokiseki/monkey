package priv.zhaoyang.monkey.ast;

import priv.zhaoyang.monkey.token.Token;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/* 和原作者的实现不太一样, 因为我用的是java */
public class MapLiteral implements Expression{

    private Token token;
    private Map<Expression, Expression> map;

    public MapLiteral(Token token){
        this.token = token;
        this.map = new HashMap<>();
    }

    public Token token(){
        return this.token;
    }
    public Map<Expression, Expression> map(){
        return Collections.unmodifiableMap(this.map);
    }

    public MapLiteral putMap(Map<Expression, Expression> map){
        this.map = map;
        return this;
    }

    @Override
    public String tokenLiteral() {
        return this.token.literal;
    }

    @Override
    public String toString(){
        return this.map.toString();
    }
}
