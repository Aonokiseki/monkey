package priv.zhaoyang.test;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import priv.zhaoyang.monkey.ast.*;
import priv.zhaoyang.monkey.lexer.Lexer;
import priv.zhaoyang.monkey.parser.Parser;
import priv.zhaoyang.monkey.token.Token;
import priv.zhaoyang.monkey.token.TokenType;

import java.util.Arrays;
import java.util.List;

public class ParserTest {

    /* ============ Let 语句 ============ */

    @DataProvider(name = "letStatementsDataProvider")
    public Object[][] letStatementsDataProvider(){
        return new Object[][]{
                new Object[]{"let x = 5; let y = 10; let foobar = 838383;", Arrays.asList("x", "y", "foobar")},
                new Object[]{"let abc = 1; let bcd=2; let foo_bar=114514;", Arrays.asList("abc", "bcd", "foo_bar")},
                /* 838 383 是两部分，但当前 let 的值表达式尚未接入，因此放行 */
                new Object[]{"let aaa = 838 383;", Arrays.asList("aaa")},
        };
    }

    @Test(dataProvider = "letStatementsDataProvider")
    public void testLetStatements(String input, List<String> expectedNames){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        List<Statement> statements = program.statements();
        Assert.assertEquals(statements.size(), expectedNames.size());

        for (int i = 0; i < statements.size(); i++){
            Statement statement = statements.get(i);
            Assert.assertEquals(statement.tokenLiteral(), "let");
            Assert.assertTrue(statement instanceof LetStatement, "statement is not LetStatement");
            Assert.assertEquals(((LetStatement) statement).identifier().tokenLiteral(), expectedNames.get(i));
        }
    }

    @Test
    public void testLetStatementFailure(){
        String input = "let x 5; let = 10; let foo_bar 838 383;";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        parser.parseProgram();

        List<String> errors = parser.errors();
        Assert.assertEquals(errors.size(), 4);
        Assert.assertEquals(errors.get(0), "expected next token to be =, got INT instead");
        Assert.assertEquals(errors.get(1), "expected next token to be IDENT, got = instead");
        /* 残留的 "=" 不是合法前缀，报 no-prefix 错误 */
        Assert.assertEquals(errors.get(2), "Token is =, no suitable prefix function in function map");
        Assert.assertEquals(errors.get(3), "expected next token to be =, got INT instead");
    }

    /* ============ Return 语句 ============ */

    @Test
    public void testReturnStatements(){
        String input = "return 5; return abc;";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        List<Statement> statements = program.statements();
        Assert.assertEquals(statements.size(), 2);

        for (Statement statement : statements){
            Assert.assertTrue(statement instanceof ReturnStatement, "statement is not ReturnStatement");
            Assert.assertEquals(statement.tokenLiteral(), "return");
        }
    }

    /* ============ 标识符表达式 ============ */

    @DataProvider(name = "identifierExpressionDataProvider")
    public Object[][] identifierExpressionDataProvider(){
        return new Object[][]{
                new Object[]{"foobar;", "foobar"},
                new Object[]{"x;", "x"},
                new Object[]{"my_var;", "my_var"},
        };
    }

    @Test(dataProvider = "identifierExpressionDataProvider")
    public void testIdentifierExpression(String input, String expectedValue){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        testIdentifier(firstExpression(program), expectedValue);
    }

    /* ============ 整数字面量 ============ */

    @DataProvider(name = "integerLiteralDataProvider")
    public Object[][] integerLiteralDataProvider(){
        return new Object[][]{
                new Object[]{"5;", 5L},
                new Object[]{"10;", 10L},
                new Object[]{"838383;", 838383L},
                /* 超过 int 范围，验证 int64 (long) */
                new Object[]{"2147483649;", 2147483649L},
        };
    }

    @Test(dataProvider = "integerLiteralDataProvider")
    public void testIntegerLiteral(String input, long expectedValue){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        testIntegerLiteral(firstExpression(program), expectedValue);
    }

    /* ============ 前缀表达式 ============ */

    @DataProvider(name = "prefixExpressionDataProvider")
    public Object[][] prefixExpressionDataProvider(){
        return new Object[][]{
                new Object[]{"!5;", "!", "5"},
                new Object[]{"-15;", "-", "15"},
                new Object[]{"!a;", "!", "a"},
                new Object[]{"-a;", "-", "a"},
        };
    }

    @Test(dataProvider = "prefixExpressionDataProvider")
    public void testPrefixExpression(String input, String operator, String rightTokenLiteral){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        Expression expression = firstExpression(program);
        Assert.assertTrue(expression instanceof PrefixExpression, "expression is not PrefixExpression");
        PrefixExpression prefixExpression = (PrefixExpression) expression;

        Assert.assertEquals(prefixExpression.operator(), operator);
        Assert.assertEquals(prefixExpression.expression().tokenLiteral(), rightTokenLiteral);
    }

    /* ============ 中缀表达式 ============ */

    @DataProvider(name = "infixExpressionDataProvider")
    public Object[][] infixExpressionDataProvider(){
        return new Object[][]{
                new Object[]{"5 + 5;", 5L, "+", 5L},
                new Object[]{"5 - 5;", 5L, "-", 5L},
                new Object[]{"5 * 5;", 5L, "*", 5L},
                new Object[]{"5 / 5;", 5L, "/", 5L},
                new Object[]{"5 > 5;", 5L, ">", 5L},
                new Object[]{"5 < 5;", 5L, "<", 5L},
                new Object[]{"5 == 5;", 5L, "==", 5L},
                new Object[]{"5 != 5;", 5L, "!=", 5L},
        };
    }

    @Test(dataProvider = "infixExpressionDataProvider")
    public void testInfixExpression(String input, long leftValue, String operator, long rightValue){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        Expression expression = firstExpression(program);
        Assert.assertTrue(expression instanceof InfixExpression, "expression is not InfixExpression");
        InfixExpression infixExpression = (InfixExpression) expression;

        testIntegerLiteral(infixExpression.left(), leftValue);
        Assert.assertEquals(infixExpression.operator(), operator);
        testIntegerLiteral(infixExpression.right(), rightValue);
    }

    /* ============ 运算符优先级 ============ */

    @DataProvider(name = "operatorPrecedenceDataProvider")
    public Object[][] operatorPrecedenceDataProvider(){
        return new Object[][]{
                new Object[]{"-a * b", "((-a) * b)"},
                new Object[]{"!-a", "(!(-a))"},
                new Object[]{"a + b + c", "((a + b) + c)"},
                new Object[]{"a + b - c", "((a + b) - c)"},
                new Object[]{"a * b * c", "((a * b) * c)"},
                new Object[]{"a * b / c", "((a * b) / c)"},
                new Object[]{"a + b / c", "(a + (b / c))"},
                new Object[]{"a + b * c + d / e - f", "(((a + (b * c)) + (d / e)) - f)"},
                new Object[]{"3 + 4; -5 * 5", "(3 + 4)((-5) * 5)"},
                new Object[]{"5 > 4 == 3 < 4", "((5 > 4) == (3 < 4))"},
                new Object[]{"5 < 4 != 3 > 4", "((5 < 4) != (3 > 4))"},
                new Object[]{"3 + 4 * 5 == 3 * 1 + 4 * 5", "((3 + (4 * 5)) == ((3 * 1) + (4 * 5)))"},
        };
    }

    @Test(dataProvider = "operatorPrecedenceDataProvider")
    public void testOperatorPrecedence(String input, String expected){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        Assert.assertEquals(program.toString(), expected);
    }

    /* ============ 错误用例 ============ */

    @Test
    public void testPrefixExpressionFailure(){
        String input = "> 1;";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        parser.parseProgram();

        List<String> errors = parser.errors();
        Assert.assertEquals(errors.size(), 1);
        Assert.assertEquals(errors.get(0), "Token is >, no suitable prefix function in function map");
    }

    /* ============ String 打印 ============ */

    @Test
    public void testString(){
        Program program = new Program();
        program.putStatements(Arrays.asList(
                new LetStatement(new Token(TokenType.LET, "let"))
                        .putIdentifier(new Identifier(new Token(TokenType.IDENT, "myVar"), "myVar"))
                        /* identifier 也是 expression */
                        .putExpression(new Identifier(new Token(TokenType.IDENT, "anotherVar"), "anotherVar"))
        ));
        Assert.assertEquals(program.toString(), "let myVar = anotherVar;");
    }

    /* ============ 辅助方法 ============ */

    private Expression firstExpression(Program program){
        List<Statement> statements = program.statements();
        Assert.assertEquals(statements.size(), 1);
        Statement statement = statements.get(0);
        Assert.assertTrue(statement instanceof ExpressionStatement, "statement is not ExpressionStatement");
        return ((ExpressionStatement) statement).expression();
    }

    private void testIdentifier(Expression expression, String value){
        Assert.assertTrue(expression instanceof Identifier, "expression is not Identifier");
        Identifier identifier = (Identifier) expression;
        Assert.assertEquals(identifier.value(), value);
        Assert.assertEquals(identifier.tokenLiteral(), value);
    }

    private void testIntegerLiteral(Expression expression, long value){
        Assert.assertTrue(expression instanceof IntegerLiteral, "expression is not IntegerLiteral");
        IntegerLiteral integerLiteral = (IntegerLiteral) expression;
        Assert.assertEquals(integerLiteral.value(), value);
        Assert.assertEquals(integerLiteral.tokenLiteral(), String.valueOf(value));
    }

    private static void checkParserErrors(Parser parser){
        List<String> errors = parser.errors();
        if (errors.isEmpty())
            return;

        StringBuilder failMessage = new StringBuilder();
        failMessage.append("parser has ").append(errors.size()).append(" errors.").append(" ");
        for (String error : errors) {
            failMessage.append(error).append(" ");
        }
        Assert.fail(failMessage.toString());
    }
}
