package priv.zhaoyang.monkey.token;

import java.util.HashMap;
import java.util.Map;

public class Keywords {

    private final static Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("fn", TokenType.FUNCTION);
        KEYWORDS.put("let", TokenType.LET);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("while", TokenType.WHILE);
    }

    public static boolean contains(String letters){
        return KEYWORDS.containsKey(letters);
    }

    public static TokenType get(String letters){
        return get(letters, TokenType.ILLEGAL);
    }

    public static TokenType get(String letters, TokenType defaultValue){
        return KEYWORDS.getOrDefault(letters, defaultValue);
    }
}
