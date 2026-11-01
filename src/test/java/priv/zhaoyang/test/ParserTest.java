package priv.zhaoyang.test;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import priv.zhaoyang.monkey.ast.*;
import priv.zhaoyang.monkey.lexer.Lexer;
import priv.zhaoyang.monkey.parser.Parser;
import priv.zhaoyang.monkey.token.Token;
import priv.zhaoyang.monkey.token.TokenType;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class ParserTest {

    @DataProvider(name = "testLetStatementsDataProvider")
    public Object[][] testLetStatementsDataProvider(Method method){
        return new Object[][]{
                new Object[]{
                        "let x = 5; let y = 10; let foobar = 838383;",
                        Arrays.asList("x", "y", "foobar")
                },
                new Object[]{
                        "let abc = 1; let bcd=2; let foo_bar=114514;",
                        Arrays.asList("abc", "bcd", "foo_bar")
                },
                /* 838 383 是两部分, 但当下的逻辑直接忽略当作尚未解析的expression,因此这个地方要放行 */
                new Object[]{
                        "let aaa = 838 383;",
                        Arrays.asList("aaa")
                },
        };
    }

    @Test(dataProvider = "testLetStatementsDataProvider")
    public void testLetStatements(String input, List<String> identififerNames){

        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);

        Program program = parser.parseProgram();
        checkParserErrors(parser);

        List<Statement> statements = program.statements();
        Assert.assertEquals(statements.size(), identififerNames.size());

        for (int i = 0; i < statements.size(); i++){
            Statement statement = statements.get(i);
            testLetStatement(statement, identififerNames.get(i));
        }
    }

    @Test
    public void testLetStatementFailure(){
        String invalidInput = "let x 5; let = 10; let foo_bar 838 383;";

        Lexer lexer = new Lexer(invalidInput);
        Parser parser = new Parser(lexer);

        Program program = parser.parseProgram();

        List<String> errors = parser.errors();
        /* 真是每次加代码, 总是你过不去... 比如加上找不到prefixParseFunction时，错误就变成4个了 */
        Assert.assertEquals(errors.size(), 4);

        Assert.assertEquals(errors.get(0), "expected next token to be =, got INT instead");
        Assert.assertEquals(errors.get(1), "expected next token to be IDENT, got = instead");
        /* -> 新增, 上一个=是非法的, current没移动 */
        Assert.assertEquals(errors.get(2), "Token is =, no suitable prefix function in function map");
        Assert.assertEquals(errors.get(3), "expected next token to be =, got INT instead");

    }

    @Test
    public void testString(){
        Program program = new Program();

        program.putStatements(Arrays.asList(
                new LetStatement(new Token(TokenType.LET, "let"))
                        .putIdentifier(new Identifier(new Token(TokenType.IDENT, "myVar"), "myVar"))
                        /* identifier也是expression */
                        .putExpression(new Identifier(new Token(TokenType.IDENT, "anotherVar"), "anotherVar"))
        ));

        Assert.assertEquals(program.toString(), "let myVar = anotherVar;");
    }

    private void testLetStatement(Statement statement, String name){
        Assert.assertEquals(statement.tokenLiteral(), "let");

        if (!(statement instanceof LetStatement)) {
            Assert.fail("statement is not instanceof LetStatement.");
        }

        LetStatement letStatement = (LetStatement)statement;
        Assert.assertEquals(letStatement.identifier().tokenLiteral(), name);
    }

    private static void checkParserErrors(Parser parser){
        List<String> errors = parser.errors();

        if (errors.isEmpty())
            return;

        StringBuilder failMessage = new StringBuilder();
        failMessage
                .append("parser has ").append(errors.size()).append(" errors.").append("\n");
        for (String error : errors) {
            failMessage.append(error).append("\n");
        }
        Assert.fail(failMessage.toString());
    }

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

            if (!(statement instanceof ReturnStatement)){
                Assert.fail(String.format("statement[%s] is not ReturnStatement", statement.tokenLiteral()));
            }
            /* 字面量也要断言 */
            ReturnStatement returnStatement = (ReturnStatement) statement;
            Assert.assertEquals(returnStatement.tokenLiteral(), "return");
        }
    }

    @Test
    public void testIdentifierExpression(){
        String input = "foobar;";

        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        List<Statement> statements = program.statements();
        Assert.assertEquals(statements.size(), 1);

        for (Statement statement : statements) {

            if (!(statement instanceof ExpressionStatement)) {
                Assert.fail("statement is not instance of ExpressionStatement");
            }
            ExpressionStatement expressionStatement = (ExpressionStatement) statement;
            Expression expression = expressionStatement.expression();

            if (!(expression instanceof Identifier)) {
                Assert.fail("statement.expression is not instance of Identifier");
            }
            Identifier identifier = (Identifier) expression;
            Assert.assertEquals(identifier.tokenLiteral(), "foobar");
            Assert.assertEquals(identifier.value(), "foobar");
        }
    }

    @Test
    public void testIntExpression(){
        String input = "123; 456; 2147483649";

        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        List<Statement> statements = program.statements();
        List<Long> expectedInts = Arrays.asList(123L, 456L, 2147483649L);
        Assert.assertEquals(statements.size(), expectedInts.size());

        for (int i = 0; i < statements.size(); i++){
            Statement statement = statements.get(i);

            if (!(statement instanceof ExpressionStatement)) {
                Assert.fail("statement is not instanceof ExpressionStatement");
            }
            ExpressionStatement expressionStatement = (ExpressionStatement) statement;
            Expression expression = expressionStatement.expression();

            if (!(expression instanceof IntegerLiteral)){
                Assert.fail("expression is not instanceof IntExpression");
            }
            IntegerLiteral intExpression = (IntegerLiteral) expression;

            long expectedValue = expectedInts.get(i).longValue();
            Assert.assertEquals(intExpression.value(), expectedValue);
            Assert.assertEquals(intExpression.tokenLiteral(), String.valueOf(expectedValue));
        }
    }

    @Test
    public void testPrefixExpression(){
        String input = "-2147483649;";

        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        List<Statement> statements = program.statements();
        Assert.assertEquals(statements.size(), 1);

        Statement statement = statements.get(0);
        if (!(statement instanceof ExpressionStatement)) {
            Assert.fail("statement is not instance of ExpressionStatement");
        }
        ExpressionStatement expressionStatement = (ExpressionStatement) statement;
        Expression expression = expressionStatement.expression();

        if (!(expression instanceof PrefixExpression)){
            Assert.fail("expression is not instance of PrefixExpression");
        }
        PrefixExpression prefixExpression = (PrefixExpression) expression;

        Assert.assertEquals(prefixExpression.tokenLiteral(), "-");
        Assert.assertEquals(prefixExpression.operator(), "-");

        Expression subExpression = prefixExpression.expression();
        if(!(subExpression instanceof IntegerLiteral)) {
            Assert.fail("subExpression is not instance of IntegerLiteral");
        }
        IntegerLiteral integerLiteral = (IntegerLiteral)subExpression;
        Assert.assertEquals(integerLiteral.tokenLiteral(), "2147483649");
        Assert.assertEquals(integerLiteral.value(), 2147483649L);
    }

    @Test
    public void testPrefixExpressionFailure(){
        String input = "> 1;";

        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();

        List<String> errors = parser.errors();
        Assert.assertEquals(errors.size(), 1);
        Assert.assertEquals(errors.get(0), "Token is >, no suitable prefix function in function map");
    }
}
