package priv.zhaoyang.test;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import priv.zhaoyang.monkey.ast.Program;
import priv.zhaoyang.monkey.eval.Evaluator;
import priv.zhaoyang.monkey.lexer.Lexer;
import priv.zhaoyang.monkey.object.BooleanObject;
import priv.zhaoyang.monkey.object.IntegerObject;
import priv.zhaoyang.monkey.object.MonkeyObject;
import priv.zhaoyang.monkey.object.NullObject;
import priv.zhaoyang.monkey.object.ObjectType;
import priv.zhaoyang.monkey.parser.Parser;

import java.lang.reflect.Method;
import java.util.List;

public class EvaluatorTest {

    /* ==================== 数字字面量 ==================== */
    @DataProvider(name = "testEvalIntegerLiteralDataProvider")
    private Object[][] testEvalIntegerLiteralDataProvider(Method method){
        return new Object[][]{
                /* 基础 */
                new Object[]{"5", 5L},
                new Object[]{"6;", 6L},
                new Object[]{"0", 0L},
                new Object[]{"1", 1L},
                new Object[]{"10", 10L},
                new Object[]{"42", 42L},
                /* 前导 0 */
                new Object[]{"007", 7L},
                /* 多位与 long 边界 */
                new Object[]{"1234567890", 1234567890L},
                new Object[]{"9223372036854775807", 9223372036854775807L}, /* Long.MAX_VALUE */
                /* 空白与换行（Lexer 会跳过空白） */
                new Object[]{" 5 ", 5L},
                new Object[]{"5\n", 5L},
                new Object[]{"\t5\t", 5L},
        };
    }

    @Test(dataProvider = "testEvalIntegerLiteralDataProvider")
    public void testEvalIntegerLiteral(String input, long expectedValue){
        MonkeyObject object = testProgram(input);
        testIntegerObject(object, expectedValue);
    }

    /* ==================== 布尔字面量 ==================== */
    @DataProvider(name = "testEvalBooleanLiteralDataProvider")
    private Object[][] testEvalBooleanLiteral(Method method){
        return new Object[][]{
                new Object[]{"true;", true},
                new Object[]{"false;", false},
                new Object[]{"true", true},
                new Object[]{"false", false},
                /* 空白与换行 */
                new Object[]{" true ", true},
                new Object[]{"false\n", false},
                new Object[]{"\ttrue\t", true},
        };
    }

    @Test(dataProvider = "testEvalBooleanLiteralDataProvider")
    public void testEvalBooleanLiteral(String input, boolean expectedValue){
        MonkeyObject object = testProgram(input);
        testBooleanObject(object, expectedValue);
    }

    /* ==================== 多条语句：返回最后一条的值 ====================
     * evalStatements 逐条求值，最终返回最后一条语句的值，类型随最后一条语句变化。
     * 这里用 inspect() 比对，因为期望值可能是 int 也可能是 bool。 */
    @DataProvider(name = "testEvalStatementSequenceDataProvider")
    private Object[][] testEvalStatementSequenceDataProvider(Method method){
        return new Object[][]{
                new Object[]{"5;6;", 6L},
                new Object[]{"5;6", 6L},
                new Object[]{"5; 6; 7;", 7L},
                new Object[]{"true; false;", false},
                new Object[]{"false; true", true},
                new Object[]{"false; 42;", 42L},
                new Object[]{"5; true;", true},
                new Object[]{"42; false", false},
        };
    }

    @Test(dataProvider = "testEvalStatementSequenceDataProvider")
    public void testEvalStatementSequence(String input, Object expectedValue){
        MonkeyObject object = testProgram(input);
        Assert.assertNotNull(object, "多条语句求值不应返回 null, input=" + input);
        Assert.assertEquals(object.inspect(), String.valueOf(expectedValue), "input=" + input);
    }

    /* ==================== 边界：空程序 ====================
     * evalStatements 初始化为 NullObject.NULL，空程序求值返回 Monkey 的 null 值（而非 Java null）。
     * 断言结果类型为 NULL、inspect() 为 "null"，且正是 NullObject.NULL 单例。 */
    @Test
    public void testEvalEmptyProgramReturnsNullObject(){
        MonkeyObject object = testProgram("");
        Assert.assertNotNull(object, "空程序应返回 NullObject.NULL，而不是 Java null");
        Assert.assertEquals(object.type(), ObjectType.NULL);
        Assert.assertEquals(object.inspect(), "null");
        Assert.assertSame(object, NullObject.NULL);
    }

    /* 直接对 null 节点求值：eval 的 null 防御应返回 NullObject.NULL 而非抛异常 */
    @Test
    public void testEvalNullNodeReturnsNullObject(){
        Assert.assertSame(Evaluator.eval(null), NullObject.NULL);
    }

    /* ==================== 边界：未实现的节点“大声失败” ====================
     * 当前只实现 IntegerLiteral/BooleanLiteral 求值，其余节点求值会抛 IllegalStateException。
     * 用测试锁住这个“有意为之”的行为；后续实现对应分支时，删除对应行即可。 */
    @DataProvider(name = "testEvalUnsupportedNodeDataProvider")
    private Object[][] testEvalUnsupportedNodeDataProvider(Method method){
        return new Object[][]{
                new Object[]{"-5"},      /* PrefixExpression */
                new Object[]{"!true"},   /* PrefixExpression */
                new Object[]{"5 + 5"},   /* InfixExpression */
                new Object[]{"x"},       /* Identifier */
        };
    }

    @Test(dataProvider = "testEvalUnsupportedNodeDataProvider")
    public void testEvalUnsupportedNodeThrows(String input){
        Assert.expectThrows(IllegalStateException.class, () -> testProgram(input));
    }

    /* ==================== 辅助方法 ==================== */
    private static MonkeyObject testProgram(String input){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);
        return Evaluator.eval(program);
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

    private static void testIntegerObject(MonkeyObject object, long expected){
        if (!(object instanceof IntegerObject)){
            Assert.fail("object is not instance of IntegerObject, which is " + object.getClass().getName());
        }
        IntegerObject integerObject = (IntegerObject) object;
        Assert.assertEquals(integerObject.value(), expected);
    }

    private static void testBooleanObject(MonkeyObject object, boolean expected){
        if (!(object instanceof BooleanObject)){
            Assert.fail("object is not instanceof BooleanObject, which is " + object.getClass().getName());
        }
        BooleanObject booleanObject = (BooleanObject) object;
        Assert.assertEquals(booleanObject.value(), expected);
    }
}
