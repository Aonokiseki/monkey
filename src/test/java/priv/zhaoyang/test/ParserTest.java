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
import java.util.Collections;
import java.util.List;

public class ParserTest {

    /* ============ Let 语句 ============ */

    @DataProvider(name = "letStatementsDataProvider")
    public Object[][] letStatementsDataProvider(){
        return new Object[][]{
                new Object[]{"let x = 5; let y = 10; let foobar = 838383;", Arrays.asList("x", "y", "foobar")},
                new Object[]{"let abc = 1; let bcd=2; let foo_bar=114514;", Arrays.asList("abc", "bcd", "foo_bar")},
                /* 838 383 是两部分，但当前 let 的值表达式尚未接入，因此放行 */
//                new Object[]{"let aaa = 838 383;", Arrays.asList("aaa")},
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
                new Object[]{"!true;", "!", "true"},
                new Object[]{"!false;", "!", "false"},
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

    @DataProvider(name = "testInfixExpression2DataProvider")
    public Object[][] testInfixExpression2DataProvider(Method method){
        return new Object[][]{
                new Object[]{"true != false", true, "!=", false},
                new Object[]{"true == true", true, "==", true},
                new Object[]{"true > false", true, ">", false},
        };
    }

    @Test(dataProvider = "testInfixExpression2DataProvider")
    public void testInfixExpression2(String input, boolean leftValue, String operator, boolean rightValue){

        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        Expression expression = firstExpression(program);
        Assert.assertTrue(expression instanceof InfixExpression, "expression is not InfixExpression");
        InfixExpression infixExpression = (InfixExpression) expression;

        testBooleanLiteral(infixExpression.left(), leftValue);
        Assert.assertEquals(infixExpression.operator(), operator);
        testBooleanLiteral(infixExpression.right(), rightValue);
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
                new Object[]{"2 > 1 == true", "((2 > 1) == true)"},
                new Object[]{"2 < 1 == false", "((2 < 1) == false)"},
                new Object[]{"((1 + 2) * 3) + 4", "(((1 + 2) * 3) + 4)"},
                new Object[]{"(1 + 3) * (4 / 2)", "((1 + 3) * (4 / 2))"},
                new Object[]{"3 > 5 == false", "((3 > 5) == false)"},
                new Object[]{"3 < 5 == true", "((3 < 5) == true)"},
                new Object[]{"1 + (2 + 3) + 4", "((1 + (2 + 3)) + 4)"},
                new Object[]{"(5 + 5) * 2", "((5 + 5) * 2)"},
                new Object[]{"2 / (5 + 5)", "(2 / (5 + 5))"},
                new Object[]{"-(5 + 5)", "(-(5 + 5))"},
                new Object[]{"!(true == true)", "(!(true == true))"},
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

    private void testBooleanLiteral(Expression expression, boolean value){
        Assert.assertTrue(expression instanceof BooleanLiteral, "expression is not BooleanLiteral");
        BooleanLiteral booleanLiteral = (BooleanLiteral) expression;
        Assert.assertEquals(booleanLiteral.value(), value);
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

    @Test
    public void testBooleanLiteral(){
        String input = "true; false;";

        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        List<Statement> statements = program.statements();
        Assert.assertEquals(statements.size(), 2);

        List<BooleanLiteral> expectation = Arrays.asList(BooleanLiteral.TRUE, BooleanLiteral.FALSE);
        for(int i = 0; i < statements.size(); i++){
            Statement statement = statements.get(i);

            if (!(statement instanceof ExpressionStatement)){
                Assert.fail("statement is not instance of ExpressionStatement");
            }
            ExpressionStatement expressionStatement = (ExpressionStatement) statement;
            Expression expression = expressionStatement.expression();

            if (!(expression instanceof BooleanLiteral)){
                Assert.fail("expression is not instance of BooleanLiteral");
            }
            BooleanLiteral booleanLiteral = (BooleanLiteral) expression;
            Assert.assertEquals(booleanLiteral, expectation.get(i));
        }
    }

    @Test
    public void testIfExpression(){
        String input = "if (2 > 1) { true; } else { false; }";

        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        /* 等下, 此时BlockStatement是不是算作1个Statement */
        List<Statement> statements = program.statements();
        Assert.assertEquals(statements.size(), 1);

        Statement statement = statements.get(0);
        if (!(statement instanceof ExpressionStatement)){
            Assert.fail("statement is not instance of ExpressionStatement");
        }
        ExpressionStatement expressionStatement = (ExpressionStatement) statement;
        Expression expression = expressionStatement.expression();
        if (!(expression instanceof IfExpression)){
            Assert.fail("expression is not instance of IfExpression");
        }
        IfExpression ifExpression = (IfExpression) expression;

        /* 断言条件表达式 */
        Expression condition = ifExpression.condition();
        /* condition得是个中缀表达式 */
        if (!(condition instanceof InfixExpression)){
            Assert.fail("condition is not instance of InfixExpression");
        }
        InfixExpression condComps = (InfixExpression) condition;
        Assert.assertEquals(condComps.left().tokenLiteral(), "2");
        Assert.assertEquals(condComps.operator(), ">");
        Assert.assertEquals(condComps.right().tokenLiteral(), "1");

        /* 断言Consequence, 这个通过了 */
        BlockStatement consequence = ifExpression.consequence();
        List<Statement> consequenceStatements = consequence.statements();
        Assert.assertEquals(consequenceStatements.size(), 1);
        Statement firstInConsequence = consequenceStatements.get(0);
        if (!(firstInConsequence instanceof ExpressionStatement)){
            Assert.fail("first statement in consequence is not instance of ExpressionStatement");
        }
        ExpressionStatement firstExpressionStatement = (ExpressionStatement) firstInConsequence;
        Expression expressionInFirst = firstExpressionStatement.expression();
        if (!(expressionInFirst instanceof BooleanLiteral)){
            Assert.fail("expression in first is not instance of BooleanLiteral");
        }
        BooleanLiteral booleanLiteralInConsequence = (BooleanLiteral) expressionInFirst;
        Assert.assertEquals(booleanLiteralInConsequence, BooleanLiteral.TRUE);

        /* 断言Alternative */
        BlockStatement alternative = ifExpression.alternative();
        List<Statement> alternativeStatements = alternative.statements();
        Assert.assertEquals(alternativeStatements.size(), 1);
        Statement firstInAliternative = alternativeStatements.get(0);
        if (!(firstInAliternative instanceof ExpressionStatement)){
            Assert.fail("first statement in alternative is not instance of ExpressionStatement");
        }
        ExpressionStatement firstExpressionStatement1 = (ExpressionStatement) firstInAliternative;
        Expression expressionInFirst1 = firstExpressionStatement1.expression();
        if (!(expressionInFirst1 instanceof BooleanLiteral)){
            Assert.fail("expression in first 1 is not instance of BooleanLiteral");
        }
        BooleanLiteral booleanLiteralInAlternative = (BooleanLiteral) expressionInFirst1;
        Assert.assertEquals(booleanLiteralInAlternative, BooleanLiteral.FALSE);
    }

    /* ============ if 表达式：无分号 / 多语句 / else ============ */

    @Test
    public void testIfExpressionWithoutElse(){
        IfExpression ifExpression = parseIfExpression("if (x < y) { x }");

        /* 条件: x < y */
        Assert.assertTrue(ifExpression.condition() instanceof InfixExpression);
        InfixExpression condition = (InfixExpression) ifExpression.condition();
        testIdentifier(condition.left(), "x");
        Assert.assertEquals(condition.operator(), "<");
        testIdentifier(condition.right(), "y");

        /* consequence: 1 条语句 x，无分号也不死循环 */
        Assert.assertEquals(ifExpression.consequence().statements().size(), 1);
        testIdentifier(((ExpressionStatement) ifExpression.consequence().statements().get(0)).expression(), "x");

        /* 无 else */
        Assert.assertNull(ifExpression.alternative());
    }

    @Test
    public void testIfExpressionWithElse(){
        IfExpression ifExpression = parseIfExpression("if (x < y) { x } else { y }");

        Assert.assertEquals(ifExpression.consequence().statements().size(), 1);
        testIdentifier(((ExpressionStatement) ifExpression.consequence().statements().get(0)).expression(), "x");

        Assert.assertNotNull(ifExpression.alternative());
        Assert.assertEquals(ifExpression.alternative().statements().size(), 1);
        testIdentifier(((ExpressionStatement) ifExpression.alternative().statements().get(0)).expression(), "y");
    }

    @Test
    public void testIfExpressionMultiStatement(){
        IfExpression ifExpression = parseIfExpression("if (x < y) { x; y; } else { z; }");

        /* consequence 有 2 条语句 */
        Assert.assertEquals(ifExpression.consequence().statements().size(), 2);
        testIdentifier(((ExpressionStatement) ifExpression.consequence().statements().get(0)).expression(), "x");
        testIdentifier(((ExpressionStatement) ifExpression.consequence().statements().get(1)).expression(), "y");

        /* alternative 有 1 条语句 */
        Assert.assertEquals(ifExpression.alternative().statements().size(), 1);
        testIdentifier(((ExpressionStatement) ifExpression.alternative().statements().get(0)).expression(), "z");
    }

    private IfExpression parseIfExpression(String input){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);
        Expression expression = firstExpression(program);
        Assert.assertTrue(expression instanceof IfExpression, "expression is not IfExpression");
        return (IfExpression) expression;
    }

    @Test
    public void testFunctionExpression(){
        String input = "fn(x,y,z){x + 1; y + 2; z + 3;}";

        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        List<Statement> statements = program.statements();
        Assert.assertEquals(statements.size(), 1);

        Statement statement = statements.get(0);
        if (!(statement instanceof ExpressionStatement)){
            Assert.fail("statement is not instance of ExpressionStatement");
        }
        ExpressionStatement topStatement = (ExpressionStatement)statement;
        Expression topExpression = topStatement.expression();

        if(!(topExpression instanceof FunctionExpression)){
            Assert.fail("topExpression is not instance of FunctionExpression");
        }
        FunctionExpression fe = (FunctionExpression) topExpression;

        /* 断言参数数量 */
        List<Identifier> parameters = fe.parameters();
        List<String> expectedParameterNames = Arrays.asList("x", "y", "z");
        Assert.assertEquals(parameters.size(), expectedParameterNames.size());
        for(int i = 0; i < parameters.size(); i++){
            Assert.assertEquals(parameters.get(i).tokenLiteral(), expectedParameterNames.get(i));
        }

        /* 断言函数体 */
        BlockStatement body = fe.body();
        List<Statement> statementsInBody = body.statements();
        List<String> expectedRights = Arrays.asList("1", "2", "3");
        Assert.assertEquals(statementsInBody.size(), 3);
        for(int i = 0; i < statementsInBody.size(); i++){
            Statement statementInBody = statementsInBody.get(i);

            if(!(statementInBody instanceof ExpressionStatement)){
                Assert.fail("statementInBody is not instance of ExpressionStatement");
            }

            ExpressionStatement es = (ExpressionStatement) statementInBody;
            Expression e = es.expression();

            if (!(e instanceof InfixExpression)){
                Assert.fail("e is not instance of InfixExpression");
            }
            InfixExpression ie = (InfixExpression) e;

            Assert.assertEquals(ie.left().tokenLiteral(), expectedParameterNames.get(i));
            Assert.assertEquals(ie.operator(), "+");
            Assert.assertEquals(ie.right().tokenLiteral(), expectedRights.get(i));
        }
    }

    /* ============ 函数表达式：参数解析 ============ */

    @DataProvider(name = "functionParameterParsingDataProvider")
    public Object[][] functionParameterParsingDataProvider(){
        return new Object[][]{
                new Object[]{"fn() {}", Collections.emptyList()},
                new Object[]{"fn(x) {}", Arrays.asList("x")},
                new Object[]{"fn(x, y, z) {}", Arrays.asList("x", "y", "z")},
        };
    }

    @Test(dataProvider = "functionParameterParsingDataProvider")
    public void testFunctionParameterParsing(String input, List<String> expectedParams){
        FunctionExpression fe = parseFunctionExpression(input);

        List<Identifier> parameters = fe.parameters();
        Assert.assertEquals(parameters.size(), expectedParams.size());
        for (int i = 0; i < parameters.size(); i++){
            Assert.assertEquals(parameters.get(i).tokenLiteral(), expectedParams.get(i));
        }
    }

    @Test
    public void testFunctionLiteralBody(){
        FunctionExpression fe = parseFunctionExpression("fn(x, y) { x + y; }");

        /* 参数 x, y */
        Assert.assertEquals(fe.parameters().size(), 2);

        /* body: 1 条语句 x + y */
        BlockStatement body = fe.body();
        Assert.assertEquals(body.statements().size(), 1);

        Statement statement = body.statements().get(0);
        Assert.assertTrue(statement instanceof ExpressionStatement, "body statement is not ExpressionStatement");
        Expression expression = ((ExpressionStatement) statement).expression();
        Assert.assertTrue(expression instanceof InfixExpression, "expression is not InfixExpression");
        InfixExpression infix = (InfixExpression) expression;
        testIdentifier(infix.left(), "x");
        Assert.assertEquals(infix.operator(), "+");
        testIdentifier(infix.right(), "y");
    }

    private FunctionExpression parseFunctionExpression(String input){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);
        Expression expression = firstExpression(program);
        Assert.assertTrue(expression instanceof FunctionExpression, "expression is not FunctionExpression");
        return (FunctionExpression) expression;
    }

    /* ============ while 表达式 ============ */

    @Test
    public void testWhileExpression(){
        WhileExpression we = parseWhileExpression("while (x < 5) { x }");

        /* 条件: x < 5 */
        Assert.assertTrue(we.condition() instanceof InfixExpression, "condition is not InfixExpression");
        InfixExpression condition = (InfixExpression) we.condition();
        testIdentifier(condition.left(), "x");
        Assert.assertEquals(condition.operator(), "<");
        testIntegerLiteral(condition.right(), 5L);

        /* body: 1 条语句 x，无分号也不死循环 */
        Assert.assertEquals(we.body().statements().size(), 1);
        testIdentifier(((ExpressionStatement) we.body().statements().get(0)).expression(), "x");
    }

    @Test
    public void testWhileExpressionMultiStatement(){
        WhileExpression we = parseWhileExpression("while (x > 0) { x - 1; x }");

        /* body: 2 条语句 */
        Assert.assertEquals(we.body().statements().size(), 2);

        /* 第一条: x - 1 */
        Expression first = ((ExpressionStatement) we.body().statements().get(0)).expression();
        Assert.assertTrue(first instanceof InfixExpression, "first is not InfixExpression");
        InfixExpression minus = (InfixExpression) first;
        testIdentifier(minus.left(), "x");
        Assert.assertEquals(minus.operator(), "-");
        testIntegerLiteral(minus.right(), 1L);

        /* 第二条: x */
        testIdentifier(((ExpressionStatement) we.body().statements().get(1)).expression(), "x");
    }

    @Test
    public void testWhileExpressionBooleanCondition(){
        WhileExpression we = parseWhileExpression("while (true) { 5 }");

        Assert.assertTrue(we.condition() instanceof BooleanLiteral, "condition is not BooleanLiteral");
        Assert.assertEquals(we.condition(), BooleanLiteral.TRUE);

        Assert.assertEquals(we.body().statements().size(), 1);
        testIntegerLiteral(((ExpressionStatement) we.body().statements().get(0)).expression(), 5L);
    }

    private WhileExpression parseWhileExpression(String input){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);
        Expression expression = firstExpression(program);
        Assert.assertTrue(expression instanceof WhileExpression, "expression is not WhileExpression");
        return (WhileExpression) expression;
    }

    /* ============ 调用表达式 ============ */

    @Test
    public void testCallExpression(){
        CallExpression ce = parseCallExpression("add(1, 2 * 3, 4 + 5);");

        /* function: add */
        testIdentifier(ce.function(), "add");

        /* 3 个参数 */
        Assert.assertEquals(ce.arguments().size(), 3);
        testIntegerLiteral(ce.arguments().get(0), 1L);

        /* 2 * 3 */
        Assert.assertTrue(ce.arguments().get(1) instanceof InfixExpression, "arg1 is not InfixExpression");
        InfixExpression arg1 = (InfixExpression) ce.arguments().get(1);
        testIntegerLiteral(arg1.left(), 2L);
        Assert.assertEquals(arg1.operator(), "*");
        testIntegerLiteral(arg1.right(), 3L);

        /* 4 + 5 */
        InfixExpression arg2 = (InfixExpression) ce.arguments().get(2);
        testIntegerLiteral(arg2.left(), 4L);
        Assert.assertEquals(arg2.operator(), "+");
        testIntegerLiteral(arg2.right(), 5L);
    }

    @Test
    public void testCallExpressionNoArgs(){
        CallExpression ce = parseCallExpression("add();");

        testIdentifier(ce.function(), "add");
        Assert.assertEquals(ce.arguments().size(), 0);
    }

    @Test
    public void testNestedCallExpression(){
        CallExpression ce = parseCallExpression("add(1, sub(2, 3));");

        testIdentifier(ce.function(), "add");
        Assert.assertEquals(ce.arguments().size(), 2);
        testIntegerLiteral(ce.arguments().get(0), 1L);

        Assert.assertTrue(ce.arguments().get(1) instanceof CallExpression, "arg1 is not CallExpression");
        CallExpression inner = (CallExpression) ce.arguments().get(1);
        testIdentifier(inner.function(), "sub");
        Assert.assertEquals(inner.arguments().size(), 2);
        testIntegerLiteral(inner.arguments().get(0), 2L);
        testIntegerLiteral(inner.arguments().get(1), 3L);
    }

    private CallExpression parseCallExpression(String input){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);
        Expression expression = firstExpression(program);
        Assert.assertTrue(expression instanceof CallExpression, "expression is not CallExpression");
        return (CallExpression) expression;
    }

    /* ============ let 语句的值表达式（验证 parseExpression 已接入 let） ============ */

    @Test
    public void testLetStatementIntegerValue(){
        LetStatement stmt = parseLetStatement("let x = 5;");
        testIdentifier(stmt.identifier(), "x");
        testIntegerLiteral(stmt.expression(), 5L);
    }

    @Test
    public void testLetStatementBooleanValue(){
        LetStatement stmt = parseLetStatement("let y = true;");
        testIdentifier(stmt.identifier(), "y");
        testBooleanLiteral(stmt.expression(), true);
    }

    @Test
    public void testLetStatementIdentifierValue(){
        LetStatement stmt = parseLetStatement("let foobar = y;");
        testIdentifier(stmt.identifier(), "foobar");
        testIdentifier(stmt.expression(), "y");
    }

    @Test
    public void testLetStatementInfixValue(){
        LetStatement stmt = parseLetStatement("let z = 5 + 5;");
        testIdentifier(stmt.identifier(), "z");

        Assert.assertTrue(stmt.expression() instanceof InfixExpression, "value is not InfixExpression");
        InfixExpression infix = (InfixExpression) stmt.expression();
        testIntegerLiteral(infix.left(), 5L);
        Assert.assertEquals(infix.operator(), "+");
        testIntegerLiteral(infix.right(), 5L);
    }

    /* ============ return 语句的值表达式（验证 parseExpression 已接入 return） ============ */

    @Test
    public void testReturnStatementIntegerValue(){
        ReturnStatement stmt = parseReturnStatement("return 5;");
        testIntegerLiteral(stmt.returnExpression(), 5L);
    }

    @Test
    public void testReturnStatementBooleanValue(){
        ReturnStatement stmt = parseReturnStatement("return true;");
        testBooleanLiteral(stmt.returnExpression(), true);
    }

    @Test
    public void testReturnStatementIdentifierValue(){
        ReturnStatement stmt = parseReturnStatement("return foobar;");
        testIdentifier(stmt.returnExpression(), "foobar");
    }

    @Test
    public void testReturnStatementInfixValue(){
        ReturnStatement stmt = parseReturnStatement("return x + 5;");

        Assert.assertTrue(stmt.returnExpression() instanceof InfixExpression, "value is not InfixExpression");
        InfixExpression infix = (InfixExpression) stmt.returnExpression();
        testIdentifier(infix.left(), "x");
        Assert.assertEquals(infix.operator(), "+");
        testIntegerLiteral(infix.right(), 5L);
    }

    @Test
    public void testReturnStatementCallValue(){
        ReturnStatement stmt = parseReturnStatement("return add(1, 2);");

        Assert.assertTrue(stmt.returnExpression() instanceof CallExpression, "value is not CallExpression");
        CallExpression call = (CallExpression) stmt.returnExpression();
        testIdentifier(call.function(), "add");
        Assert.assertEquals(call.arguments().size(), 2);
        testIntegerLiteral(call.arguments().get(0), 1L);
        testIntegerLiteral(call.arguments().get(1), 2L);
    }

    /* ============ 辅助：解析单条 let / return ============ */

    private LetStatement parseLetStatement(String input){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        List<Statement> statements = program.statements();
        Assert.assertEquals(statements.size(), 1);
        Assert.assertTrue(statements.get(0) instanceof LetStatement, "statement is not LetStatement");
        return (LetStatement) statements.get(0);
    }

    private ReturnStatement parseReturnStatement(String input){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        List<Statement> statements = program.statements();
        Assert.assertEquals(statements.size(), 1);
        Assert.assertTrue(statements.get(0) instanceof ReturnStatement, "statement is not ReturnStatement");
        return (ReturnStatement) statements.get(0);
    }

    @Test
    public void testParseStringLiteral(){
        String input = "let str = \"Monkey\"; let str_two = \"Lang\";";

        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);

        List<Statement> statements = program.statements();

        List<StringLiteral> stringLiterals = Arrays.asList(
                new StringLiteral(new Token(TokenType.STRING, "Monkey")),
                new StringLiteral(new Token(TokenType.STRING, "Lang"))
        );
        Assert.assertEquals(statements.size(), stringLiterals.size());

        for(int i = 0; i < statements.size(); i++){
            Statement statement = statements.get(i);
            if (!(statement instanceof LetStatement)){
                Assert.fail("statement is not instance of ExpressionStatement");
            }
            LetStatement letStatement = (LetStatement) statement;
            Expression expression = letStatement.expression();
            if (!(expression instanceof StringLiteral)){
                Assert.fail("expression is not instance of StringLiteral");
            }
            StringLiteral stringLiteral = (StringLiteral) expression;
            Assert.assertEquals(stringLiteral.value(), stringLiterals.get(i).value());
        }
    }

    @DataProvider(name = "stringLiteralExpressionDataProvider")
    public Object[][] stringLiteralExpressionDataProvider(Method method){
        return new Object[][]{
                new Object[]{"\"hello world\";", "hello world"},
                new Object[]{"\"Monkey\";", "Monkey"},
                new Object[]{"\"\";", ""},
        };
    }

    @Test(dataProvider = "stringLiteralExpressionDataProvider")
    public void testStringLiteralExpression(String input, String expectedValue){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);
        ExpressionStatement statement = (ExpressionStatement) program.statements().get(0);
        Expression expression = statement.expression();
        Assert.assertTrue(expression instanceof StringLiteral, "expression is not StringLiteral");
        StringLiteral stringLiteral = (StringLiteral) expression;
        Assert.assertEquals(stringLiteral.value(), expectedValue);
    }
}
