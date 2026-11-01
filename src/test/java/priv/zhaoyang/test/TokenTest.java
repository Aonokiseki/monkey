package priv.zhaoyang.test;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import priv.zhaoyang.monkey.lexer.Lexer;
import priv.zhaoyang.monkey.token.Token;
import priv.zhaoyang.monkey.token.TokenType;

import java.lang.reflect.Method;

public class TokenTest {

    @DataProvider(name = "testNextTokenDataProvider")
    public Object[][] testNextTokenDataProvider(Method method){
        return new Object[][]{
                new Object[]{
                        "=+(){},;",
                        new Token[]{
                                new Token(TokenType.ASSIGN, "="),
                                new Token(TokenType.PLUS, "+"),
                                new Token(TokenType.LPAREN, "("),
                                new Token(TokenType.RPAREN, ")"),
                                new Token(TokenType.LBRACE, "{"),
                                new Token(TokenType.RBRACE, "}"),
                                new Token(TokenType.COMMA, ","),
                                new Token(TokenType.SEMICOLON, ";"),
                                new Token(TokenType.EOF, ""),

                        }
                },
                new Object[]{
                        "let,fn(x)={}",
                        new Token[]{
                                new Token(TokenType.LET, "let"),
                                new Token(TokenType.COMMA, ","),
                                new Token(TokenType.FUNCTION, "fn"),
                                new Token(TokenType.LPAREN, "("),
                                new Token(TokenType.IDENT, "x"),
                                new Token(TokenType.RPAREN, ")"),
                                new Token(TokenType.ASSIGN, "="),
                                new Token(TokenType.LBRACE, "{"),
                                new Token(TokenType.RBRACE, "}"),
                                new Token(TokenType.EOF, ""),
                        }
                },
                new Object[]{
                        "let func=fn(x){66=66;};",
                        new Token[]{
                                new Token(TokenType.LET, "let"),
                                new Token(TokenType.IDENT, "func"),
                                new Token(TokenType.ASSIGN, "="),
                                new Token(TokenType.FUNCTION, "fn"),
                                new Token(TokenType.LPAREN, "("),
                                new Token(TokenType.IDENT, "x"),
                                new Token(TokenType.RPAREN, ")"),
                                new Token(TokenType.LBRACE, "{"),
                                new Token(TokenType.INT, "66"),
                                new Token(TokenType.ASSIGN, "="),
                                new Token(TokenType.INT, "66"),
                                new Token(TokenType.SEMICOLON, ";"),
                                new Token(TokenType.RBRACE, "}"),
                                new Token(TokenType.SEMICOLON, ";"),
                                new Token(TokenType.EOF, ""),
                        }
                },
                new Object[]{
                        "let five=5; let ten=10; let add=fn(x,y){x+y;}; " +
                                "!-/*5; 5<10>5; " +
                                "if (5 > 10) {return true;} else {return false;}",
                        new Token[]{
                                new Token(TokenType.LET, "let"),
                                new Token(TokenType.IDENT, "five"),
                                new Token(TokenType.ASSIGN, "="),
                                new Token(TokenType.INT, "5"),
                                new Token(TokenType.SEMICOLON, ";"),
                                new Token(TokenType.LET, "let"),
                                new Token(TokenType.IDENT, "ten"),
                                new Token(TokenType.ASSIGN, "="),
                                new Token(TokenType.INT, "10"),
                                new Token(TokenType.SEMICOLON, ";"),
                                new Token(TokenType.LET, "let"),
                                new Token(TokenType.IDENT, "add"),
                                new Token(TokenType.ASSIGN, "="),
                                new Token(TokenType.FUNCTION, "fn"),
                                new Token(TokenType.LPAREN, "("),
                                new Token(TokenType.IDENT, "x"),
                                new Token(TokenType.COMMA, ","),
                                new Token(TokenType.IDENT, "y"),
                                new Token(TokenType.RPAREN, ")"),
                                new Token(TokenType.LBRACE, "{"),
                                new Token(TokenType.IDENT, "x"),
                                new Token(TokenType.PLUS, "+"),
                                new Token(TokenType.IDENT, "y"),
                                new Token(TokenType.SEMICOLON, ";"),
                                new Token(TokenType.RBRACE, "}"),
                                new Token(TokenType.SEMICOLON, ";"),
                                new Token(TokenType.BANG, "!"),
                                new Token(TokenType.MINUS, "-"),
                                new Token(TokenType.SLASH, "/"),
                                new Token(TokenType.ASTERISK, "*"),
                                new Token(TokenType.INT, "5"),
                                new Token(TokenType.SEMICOLON, ";"),
                                new Token(TokenType.INT, "5"),
                                new Token(TokenType.LT, "<"),
                                new Token(TokenType.INT, "10"),
                                new Token(TokenType.GT, ">"),
                                new Token(TokenType.INT, "5"),
                                new Token(TokenType.SEMICOLON, ";"),
                                new Token(TokenType.IF, "if"),
                                new Token(TokenType.LPAREN, "("),
                                new Token(TokenType.INT, "5"),
                                new Token(TokenType.GT, ">"),
                                new Token(TokenType.INT, "10"),
                                new Token(TokenType.RPAREN, ")"),
                                new Token(TokenType.LBRACE, "{"),
                                new Token(TokenType.RETURN, "return"),
                                new Token(TokenType.TRUE, "true"),
                                new Token(TokenType.SEMICOLON, ";"),
                                new Token(TokenType.RBRACE, "}"),
                                new Token(TokenType.ELSE, "else"),
                                new Token(TokenType.LBRACE, "{"),
                                new Token(TokenType.RETURN, "return"),
                                new Token(TokenType.FALSE, "false"),
                                new Token(TokenType.SEMICOLON, ";"),
                                new Token(TokenType.RBRACE, "}"),
                                new Token(TokenType.EOF, ""),
                        },
                },
                new Object[]{
                        "10 == 10; 10 != 9",
                        new Token[]{
                                new Token(TokenType.INT, "10"),
                                new Token(TokenType.EQ, "=="),
                                new Token(TokenType.INT, "10"),
                                new Token(TokenType.SEMICOLON, ";"),
                                new Token(TokenType.INT, "10"),
                                new Token(TokenType.NOT_EQ, "!="),
                                new Token(TokenType.INT, "9"),
                                new Token(TokenType.EOF, ""),
                        }
                }
        };
    }

    @Test(dataProvider = "testNextTokenDataProvider")
    public void testNextToken(String input, Token[] expectedTokens){

        Lexer lexer = new Lexer(input);

        for (Token expected : expectedTokens) {
            Token actual = lexer.nextToken();
//            String log = String.format("actual=[%s], expected=[%s]", actual, expected);
//            System.out.println(log);
            Assert.assertEquals(actual.literal, expected.literal);
            Assert.assertEquals(actual.type, expected.type);
        }
    }
}
