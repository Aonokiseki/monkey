package priv.zhaoyang.monkey.lexer;

import priv.zhaoyang.monkey.token.Keywords;
import priv.zhaoyang.monkey.token.Token;
import priv.zhaoyang.monkey.token.TokenType;

public class Lexer {

    private final String input;
    private int current;
    private int readPoint;

    public Lexer(String input){
        this.input = input;
        current = -1;
        readPoint = 0;
    }

    /**
     * 读取当前字符, 并将current和readPoint向后移动一位
     * @return 当前字符
     */
    private char readChar(){
        /* 假设只有1个字符 'a'
         * current:-1, readPoint=0, 不进入if, current变为0, readPoint变为1, 返回'a'
         * readPoint: 1, 进入if块, 返回 '0'
         *  */
        if (readPoint >= input.length()){
            return 0;
        }
        current = readPoint;
        readPoint++;
        return input.charAt(current);
    }

    private char getCurrentChar(){
        return input.charAt(current);
    }

    private char peekNextChar(){
        return readPoint >= input.length() ? 0 : input.charAt(readPoint);
    }

    public Token nextToken(){

        char ch = readChar();

        while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
            ch = readChar();
        }

        char detected = peekNextChar();

        switch (ch) {

            case '=':

                if (detected == '=') {
                    readChar();
                    return new Token(TokenType.EQ, "==");
                }

                return new Token(TokenType.ASSIGN, "=");

            case '+':
                return new Token(TokenType.PLUS, "+");

            case '-':
                return new Token(TokenType.MINUS, "-");

            case '!':

                if (detected == '='){
                    readChar();
                    return new Token(TokenType.NOT_EQ, "!=");
                }

                return new Token(TokenType.BANG, "!");

            case '*':
                return new Token(TokenType.ASTERISK, "*");

            case '/':
                return new Token(TokenType.SLASH, "/");

            case '<':
                return new Token(TokenType.LT, "<");

            case '>':
                return new Token(TokenType.GT, ">");

            case ',':
                return new Token(TokenType.COMMA, ",");

            case ';':
                return new Token(TokenType.SEMICOLON, ";");

            case '(':
                return new Token(TokenType.LPAREN, "(");

            case ')':
                return new Token(TokenType.RPAREN, ")");

            case '{':
                return new Token(TokenType.LBRACE, "{");

            case '}':
                return new Token(TokenType.RBRACE, "}");

            case '[':
                return new Token(TokenType.LBRACKETS, "[");

            case ']':
                return new Token(TokenType.RBRACKETS, "]");

            case '"':
                String stringLiteral = readString();
                return new Token(TokenType.STRING, stringLiteral);

            case 0:
                return new Token(TokenType.EOF, "");

            default:

                if (isDigit(ch)) {

                    String intLiteral = readIntLiteral();
                    return new Token(TokenType.INT, intLiteral);

                } else if (isLetter(ch)) {

                    String letters = readLettersLiteral();
                    if (Keywords.contains(letters)){
                        return new Token(Keywords.get(letters), letters);
                    }
                    return new Token(TokenType.IDENT, letters);

                } else {

                    return new Token(TokenType.ILLEGAL, String.valueOf(ch));
                }
        }
    }

    private String readIntLiteral(){
        StringBuilder numberString = new StringBuilder();

        char ch = getCurrentChar();

        while (isDigit(ch)) {

            numberString.append(ch);

            char readPointChar = peekNextChar();
            if (!isDigit(readPointChar))
                break;

            ch = readChar();
        }

        return numberString.toString();
    }

    private String readLettersLiteral(){
        StringBuilder identifierString = new StringBuilder();

        char ch = getCurrentChar();

        /* 不使用Character.isLetter(), 下划线会返回false, 但monkey中需要认为这是一个合法的'字母' */
        while (isLetter(ch)) {

            identifierString.append(ch);

            /* 探测下一个字符, 没有移动readPoint */
            char nextChar = peekNextChar();
            /* 如果下一个字符不再是英文字母, 就跳出, 不要再读下一个字符了
             * 因为在readChar() 中，current 和 readPoint 指针都会向后移动,
             * 这样会导致紧跟在英文字母后方的字符在下一次readChar()被吞掉(相当于连续移动了2次) */

            if (!isLetter(nextChar))
                break;

            ch = readChar();
        }

        return identifierString.toString();
    }

    private static boolean isDigit(char ch){
        return ch >= '0' && ch <= '9';
    }

    private static boolean isLetter(char ch){
        boolean isLower = ch >= 'a' && ch <= 'z';
        boolean isUpper = ch >= 'A' && ch <= 'Z';
        boolean isBottom = ch == '_';
        return isLower || isUpper || isBottom;
    }

    /* 读取字符串 */
    private String readString(){
        StringBuilder sb = new StringBuilder();

        char ch = readChar();
        /* 特殊情况: \" 转义符先不考虑支持 */
        while (ch != '"' && ch != 0){
            sb.append(ch);
            ch = readChar();
        }
        return sb.toString();
    }

}
