package priv.zhaoyang.test;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import priv.zhaoyang.monkey.ast.LetStatement;
import priv.zhaoyang.monkey.ast.Program;
import priv.zhaoyang.monkey.ast.Statement;
import priv.zhaoyang.monkey.lexer.Lexer;
import priv.zhaoyang.monkey.parser.Parser;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
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
                /* 故意失败的用例 */
                new Object[]{
                        "let x 5; let = 10; let foo_bar 838 383;",
                        Collections.emptyList()
                }
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
}
