package priv.zhaoyang.monkey.parser;

import priv.zhaoyang.monkey.token.TokenType;

import java.util.HashMap;
import java.util.Map;

public class PrecedenceMap {

    private final static Map<TokenType, Precedence> PRECEDENCE_MAP = new HashMap<>();

    static {
        PRECEDENCE_MAP.put(TokenType.EQ, Precedence.EQUAL);
        PRECEDENCE_MAP.put(TokenType.NOT_EQ, Precedence.EQUAL);
        PRECEDENCE_MAP.put(TokenType.GT, Precedence.LESSGREATER);
        PRECEDENCE_MAP.put(TokenType.LT, Precedence.LESSGREATER);
        PRECEDENCE_MAP.put(TokenType.PLUS, Precedence.SUM);
        PRECEDENCE_MAP.put(TokenType.MINUS, Precedence.SUM);
        PRECEDENCE_MAP.put(TokenType.ASTERISK, Precedence.PRODUCT);
        PRECEDENCE_MAP.put(TokenType.SLASH, Precedence.PRODUCT);
        PRECEDENCE_MAP.put(TokenType.BANG, Precedence.PREFIX);
    }

    public static boolean contains(TokenType tokenType){
        return PRECEDENCE_MAP.containsKey(tokenType);
    }

    public static Precedence get(TokenType tokenType) {
        return PRECEDENCE_MAP.getOrDefault(tokenType, Precedence.LOWEST);
    }

    public static Precedence getOrDefault(TokenType tokenType, Precedence defaultValue){
        return PRECEDENCE_MAP.getOrDefault(tokenType, defaultValue);
    }
}
