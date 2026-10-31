package priv.zhaoyang.monkey.token;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Token {

    public final TokenType type;
    public final String literal;

    public Token(TokenType type, String literal){
        this.type = type;
        this.literal = literal;
    }

    @Override
    public String toString() {
        return "Token{" + "type=" + type + ", literal='" + literal + '\'' + '}';
    }
}
