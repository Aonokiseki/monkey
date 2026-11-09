package priv.zhaoyang.test;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import priv.zhaoyang.monkey.ast.Program;
import priv.zhaoyang.monkey.eval.Environment;
import priv.zhaoyang.monkey.eval.Evaluator;
import priv.zhaoyang.monkey.lexer.Lexer;
import priv.zhaoyang.monkey.object.*;
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
                new Object[]{"5; -3;", -3L},
                new Object[]{"true; !false;", true},
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
        Assert.assertSame(Evaluator.eval(null, new Environment(null)), NullObject.NULL);
    }

    /* ==================== 辅助方法 ==================== */
    private static MonkeyObject testProgram(String input){
        return testProgram(input, new Environment(null));
    }

    private static MonkeyObject testProgram(String input, Environment environment){
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        checkParserErrors(parser);
        return Evaluator.eval(program, environment);
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

    @DataProvider(name = "testPrefixExpressionDataProvider")
    public Object[][] testPrefixExpressionDataProvider(Method method){
        return new Object[][]{
                new Object[]{"!true", BooleanObject.FALSE},
                new Object[]{"!false", BooleanObject.TRUE},
                new Object[]{"!true;", BooleanObject.FALSE},
                new Object[]{"!false;", BooleanObject.TRUE},
                /* 断言的是value, 不是IntegerObject, 不需要考虑对象是否相同 */
                new Object[]{"-2", new IntegerObject(-2)},
                new Object[]{"-2;", new IntegerObject(-2)},
                /* 更多数值 & 空白分隔 */
                new Object[]{"-5", new IntegerObject(-5L)},
                new Object[]{"-0", new IntegerObject(0L)},
                new Object[]{"-10", new IntegerObject(-10L)},
                new Object[]{"-1234567890", new IntegerObject(-1234567890L)},
                new Object[]{"-9223372036854775807", new IntegerObject(-9223372036854775807L)},
                new Object[]{"- 5", new IntegerObject(-5L)},
                new Object[]{"! true", BooleanObject.FALSE},
                new Object[]{"!false\n", BooleanObject.TRUE},
        };
    }

    /* 这个用例只考虑合法场景 */
    @Test(dataProvider = "testPrefixExpressionDataProvider")
    public void testPrefixExpression(String input, MonkeyObject expected){
        MonkeyObject object = testProgram(input);
        testPrefixExpression(object, expected);
    }

    private static void testPrefixExpression(MonkeyObject actual, MonkeyObject expected){
        Assert.assertEquals(actual.type(), expected.type());

        if (actual instanceof IntegerObject) {
            IntegerObject actualInteger = (IntegerObject) actual;
            IntegerObject expectedInteger = (IntegerObject) expected;
            Assert.assertEquals(actualInteger.value(), expectedInteger.value());
            return;
        }

        if (actual instanceof BooleanObject) {
            Assert.assertEquals((BooleanObject)actual, (BooleanObject)expected);
            return;
        }

        if (actual instanceof NullObject) {
            Assert.assertSame(actual, expected);
            return;
        }

        Assert.fail("Unknown monkey object: " + actual.getClass().getName());
    }

    /* ==================== 嵌套前缀（回归测试） ====================
     * 这些用例曾经失败：旧实现按 AST 节点类型分派，遇到操作数是 PrefixExpression 就抛异常。
     * 改为“先 eval 右操作数、按对象类型分派”后全部通过，保留作为回归。 */
    @DataProvider(name = "testEvalNestedPrefixExpressionDataProvider")
    private Object[][] testEvalNestedPrefixExpressionDataProvider(Method method){
        return new Object[][]{
                new Object[]{"!!true", BooleanObject.TRUE},
                new Object[]{"!!false", BooleanObject.FALSE},
                new Object[]{"!!true;", BooleanObject.TRUE},
                new Object[]{"-(-5)", new IntegerObject(5L)},
                new Object[]{"-(-5);", new IntegerObject(5L)},
                new Object[]{"-(-(-5))", new IntegerObject(-5L)},
                new Object[]{"-(-(-(-5)))", new IntegerObject(5L)},
                new Object[]{"!!!true", BooleanObject.FALSE},
                new Object[]{"!!!!true", BooleanObject.TRUE},
                new Object[]{"!(!5)", BooleanObject.TRUE},
                new Object[]{"!(-5)", BooleanObject.FALSE},
                new Object[]{"-(!5)", NullObject.NULL},
                /* - 作用于非整数 → NULL；! 作用于 NULL → true */
                new Object[]{"!(-false)", BooleanObject.TRUE},
                new Object[]{"-(!false)", NullObject.NULL},
                new Object[]{"!(-(!false))", BooleanObject.TRUE},
                new Object[]{"-(-false)", NullObject.NULL},
                new Object[]{"-(-true)", NullObject.NULL},
                new Object[]{"-(-(-false))", NullObject.NULL},
                new Object[]{"!(-(-false))", BooleanObject.TRUE},
        };
    }

    @Test(dataProvider = "testEvalNestedPrefixExpressionDataProvider")
    public void testEvalNestedPrefixExpression(String input, MonkeyObject expected){
        MonkeyObject object = testProgram(input);
        testPrefixExpression(object, expected);
    }

    @DataProvider(name = "testEvalInfixExpressionDataProvider")
    public Object[][] testEvalInfixExpressionDataProvider(Method method){
        return new Object[][]{
                new Object[]{"5 + 5", new IntegerObject(10)},
                new Object[]{"5 - 3", new IntegerObject(2)},
                new Object[]{"5 * 3", new IntegerObject(15)},
                new Object[]{"6 / 3", new IntegerObject(2)},
                new Object[]{"6 > 3", BooleanObject.TRUE},
                new Object[]{"6 < 3", BooleanObject.FALSE},
                new Object[]{"6 == 6", BooleanObject.TRUE},
                new Object[]{"6 != 6", BooleanObject.FALSE},
                new Object[]{"true == true", BooleanObject.TRUE},
                new Object[]{"true == false", BooleanObject.FALSE},
                new Object[]{"true != true", BooleanObject.FALSE},
                new Object[]{"true != false", BooleanObject.TRUE},

                new Object[]{"5 + 5 + 5;", new IntegerObject(15)},
                new Object[]{"5 + 6 * (12 + 12) / 4;", new IntegerObject(41)},
                new Object[]{"5 + 6 * (12 + 12) / 4 > 40;", BooleanObject.TRUE},
                new Object[]{"5 + 6 * (12 + 12) / 4 == 41;", BooleanObject.TRUE},

                new Object[]{"!!!!true == true", BooleanObject.TRUE},
                new Object[]{"!!!true == true", BooleanObject.FALSE},

                /* 整除（向零截断） */
                new Object[]{"7 / 2", new IntegerObject(3)},
                new Object[]{"5 / 2", new IntegerObject(2)},
                new Object[]{"0 / 5", new IntegerObject(0)},
                /* 负数与正数混合 */
                new Object[]{"-5 + 10", new IntegerObject(5)},
                new Object[]{"5 - 10", new IntegerObject(-5)},
                new Object[]{"-5 * -3", new IntegerObject(15)},
                new Object[]{"-10 / 3", new IntegerObject(-3)},
                /* 优先级与左结合 */
                new Object[]{"5 + 5 * 2", new IntegerObject(15)},
                new Object[]{"10 - 5 - 3", new IntegerObject(2)},
                new Object[]{"2 * 3 + 4", new IntegerObject(10)},
                /* 比较的边界 */
                new Object[]{"5 < 5", BooleanObject.FALSE},
                new Object[]{"5 > 5", BooleanObject.FALSE},
                new Object[]{"5 == 5", BooleanObject.TRUE},
                new Object[]{"5 != 6", BooleanObject.TRUE},
                /* 前缀与中缀结合 */
                new Object[]{"-5 == -5", BooleanObject.TRUE},
                new Object[]{"-5 < 0", BooleanObject.TRUE},
                new Object[]{"0 > -5", BooleanObject.TRUE},
                new Object[]{"!false == true", BooleanObject.TRUE},
                new Object[]{"!true == false", BooleanObject.TRUE},
                /* == / != 对任意类型成立（同一性）：类型不同判“不相等” */
                new Object[]{"5 == true", BooleanObject.FALSE},
                new Object[]{"true == 5", BooleanObject.FALSE},
                new Object[]{"5 != true", BooleanObject.TRUE},
                new Object[]{"(-false) == 5", BooleanObject.FALSE},
                new Object[]{"5 == (-false)", BooleanObject.FALSE},
                new Object[]{"(-false) != 5", BooleanObject.TRUE},
                new Object[]{"5 != (-false)", BooleanObject.TRUE},
                new Object[]{"(-false) == true", BooleanObject.FALSE},
                /* 非整数上的 + - * / < > → NULL（原书语义） */
                new Object[]{"true + true", NullObject.NULL},
                new Object[]{"true < true", NullObject.NULL},
                new Object[]{"(-false) < (-false)", NullObject.NULL},
                new Object[]{"(-false) + 5", NullObject.NULL},
                new Object[]{"5 < true", NullObject.NULL},
        };
    }

    @Test(dataProvider = "testEvalInfixExpressionDataProvider")
    public void testEvalInfixExpression(String input, MonkeyObject expected){
        MonkeyObject object = testProgram(input);
        testInfixExpression(object, expected);
    }

    /* ==================== null 参与的中缀比较 ====================
     * Monkey 没有 null 字面量，但 -false 会求值出 NULL，可借它测 null 的中缀语义。
     * 原书：== / != 对任意值可用（同一性比较），所以 null == null → true、null != null → false。 */
    @Test
    public void testEvalInfixNullOperand(){
        Assert.assertEquals(testProgram("(-false) == (-false)"), BooleanObject.TRUE);
        Assert.assertEquals(testProgram("(-false) != (-false)"), BooleanObject.FALSE);
    }

    private static void testInfixExpression(MonkeyObject object, MonkeyObject expected){
        if (object instanceof IntegerObject) {
            IntegerObject integerObject = (IntegerObject) object;
            if (!(expected instanceof IntegerObject)){
                Assert.fail("expected is not instance of IntegerObject");
            }
            IntegerObject expectIntegerObject = (IntegerObject) expected;
            Assert.assertEquals(integerObject.value(), expectIntegerObject.value());
            return;
        }
        if (object instanceof BooleanObject) {
            BooleanObject booleanObject = (BooleanObject) object;
            if (!(expected instanceof BooleanObject)){
                Assert.fail("expected is not instance of BooleanObject");
            }
            BooleanObject expectedBooleanObject = (BooleanObject) expected;
            Assert.assertEquals(booleanObject.value(), expectedBooleanObject.value());
            return;
        }
        if (object instanceof NullObject) {
            Assert.assertSame(object, expected);
            return;
        }
        Assert.fail("Unknown monkey object: " + object.getClass().getName());
    }

    /* ==================== if 表达式 ====================
     * 语义（原书）：条件按“真值性”判断——NULL 和 false 为假，其余（true、整数等）为真。
     * 为真求值 consequence，为假求值 alternative；无 else 时为假返回 NULL。 */
    @DataProvider(name = "testEvalIfExpressionDataProvider")
    private Object[][] testEvalIfExpressionDataProvider(Method method){
        return new Object[][]{
                /* 原书 TestIfElseExpressions */
                new Object[]{"if (true) { 10 }", new IntegerObject(10)},
                new Object[]{"if (false) { 10 }", NullObject.NULL},
                new Object[]{"if (1) { 10 }", new IntegerObject(10)},
                new Object[]{"if (1 < 2) { 10 }", new IntegerObject(10)},
                new Object[]{"if (1 > 2) { 10 }", NullObject.NULL},
                new Object[]{"if (1 > 2) { 10 } else { 20 }", new IntegerObject(20)},
                new Object[]{"if (1 < 2) { 10 } else { 20 }", new IntegerObject(10)},
                /* 真值性：0 也是 truthy */
                new Object[]{"if (0) { 10 }", new IntegerObject(10)},
                new Object[]{"if (0) { 10 } else { 20 }", new IntegerObject(10)},
                /* 布尔 / 前缀条件 */
                new Object[]{"if (!true) { 10 }", NullObject.NULL},
                new Object[]{"if (!false) { 10 } else { 20 }", new IntegerObject(10)},
                /* 多语句块：返回最后一条 */
                new Object[]{"if (true) { 1; 2; 3; }", new IntegerObject(3)},
                new Object[]{"if (false) { 1; 2; } else { 3; 4; }", new IntegerObject(4)},
                /* 空块 → NULL */
                new Object[]{"if (true) { }", NullObject.NULL},
                /* 嵌套 if */
                new Object[]{"if (true) { if (false) { 10 } else { 20 } }", new IntegerObject(20)},
                new Object[]{"if (false) { 10 } else { if (true) { 20 } }", new IntegerObject(20)},
                /* null 条件（-false 产出 NULL，falsy） */
                new Object[]{"if (-false) { 10 }", NullObject.NULL},
                /* 中缀布尔条件 */
                new Object[]{"if (true == true) { 10 } else { 20 }", new IntegerObject(10)},
        };
    }

    @Test(dataProvider = "testEvalIfExpressionDataProvider")
    public void testEvalIfExpression(String input, MonkeyObject expected){
        MonkeyObject object = testProgram(input);
        testInfixExpression(object, expected);
    }

    @DataProvider(name = "testEvalReturnExpressionDataProvider")
    private Object[][] testEvalReturnExpressionDataProvider(Method method){
        return new Object[][]{
                new Object[]{"return 1;", new IntegerObject(1)},
                new Object[]{"return true;", BooleanObject.TRUE},
                /* 注：没有 return null; 这种写法——null 不是关键字，会被 lexer 当成标识符。
                 * 想返回 Monkey 的 null 值，用求值结果为 null 的表达式，如 return -false;（见下方用例） */
                new Object[]{"return !!!false == true", BooleanObject.TRUE},
                new Object[]{"return 5 + 6 * (12 + 12) / 4;", new IntegerObject(41)},
                new Object[]{"return -false;", NullObject.NULL},
                new Object[]{"return !5;", BooleanObject.FALSE},
                new Object[]{"return if(1 > 2) {1} else {2};", new IntegerObject(2)},
                /* 提前 return：return 后方的语句不可达 */
                new Object[]{"1; return 2; 3;", new IntegerObject(2)},
                /* return 穿透嵌套 if，提前返回 */
                new Object[]{"if (10 > 1) { if (10 > 1) {return 1;} } return 2;", new IntegerObject(1)},
                /* 原书 TestReturnStatements 补充 */
                new Object[]{"return 10; 9;", new IntegerObject(10)},
                new Object[]{"9; return 2 * 5; 9;", new IntegerObject(10)},
                /* return 在 if 块内传播出来 */
                new Object[]{"if (true) { return 10; } return 20;", new IntegerObject(10)},
                new Object[]{"if (false) { return 10; } return 20;", new IntegerObject(20)},
                /* 戳破 parseIfExpression 吞 token 的 bug：return 后面再跟一条语句，短路结果才会不同 */
                new Object[]{"if (false) { return 10; } return 20; 30;", new IntegerObject(20)},
        };
    }

    @Test(dataProvider = "testEvalReturnExpressionDataProvider")
    public void testEvalReturnExpression(String input, MonkeyObject expected){
        MonkeyObject actual = testProgram(input);
        compare(actual, expected);
    }

    @DataProvider(name = "testEvalLetStatementDataProvider")
    public Object[][] testEvalLetStatementDataProvider(Method method){
        return new Object[][]{
                new Object[]{"let a = 1; let b = 2;let c = 3; return a + b * c / 2 - 1;", new IntegerObject(3)},
                new Object[]{"let a = !!!!true; let b = !a; return b;", BooleanObject.FALSE},
                new Object[]{"let a = -false; return a;", NullObject.NULL},
                /* 原书 TestLetStatements：末尾用 a; 取回 */
                new Object[]{"let a = 5; a;", new IntegerObject(5)},
                new Object[]{"let a = 5 * 5; a;", new IntegerObject(25)},
                new Object[]{"let a = 5; let b = a; b;", new IntegerObject(5)},
                new Object[]{"let a = 5; let b = a; let c = a + b + 5; c;", new IntegerObject(15)},
                /* 布尔变量 */
                new Object[]{"let b = true; b;", BooleanObject.TRUE},
                new Object[]{"let b = false; b;", BooleanObject.FALSE},
                /* 变量参与运算 */
                new Object[]{"let a = 5; a + 1;", new IntegerObject(6)},
                new Object[]{"let a = 5; !a;", BooleanObject.FALSE},
                new Object[]{"let a = 5; a == 5;", BooleanObject.TRUE},
                new Object[]{"let a = 5; a != 6;", BooleanObject.TRUE},
                new Object[]{"let a = 5; let b = a + 5; a + b;", new IntegerObject(15)},
                /* 变量用于 return / if */
                new Object[]{"let a = 5; return a;", new IntegerObject(5)},
                new Object[]{"let a = 5; if (a > 3) { 10 } else { 20 }", new IntegerObject(10)},
                /* 重新赋值（末尾再用一次取回） */
                new Object[]{"let a = 1; let a = 2; a;", new IntegerObject(2)},
                /* 重新赋值后作为最后一条语句 */
                new Object[]{"let a = 1; let a = 2;", new IntegerObject(2)},
        };
    }

    @Test(dataProvider = "testEvalLetStatementDataProvider")
    public void testEvalLetStatement(String input, MonkeyObject expected){
        Environment environment = new Environment(null);
        MonkeyObject actual = testProgram(input, environment);
        compare(actual, expected);
    }

    private void compare(MonkeyObject actual, MonkeyObject expected){
        if (actual instanceof IntegerObject) {

            if (!(expected instanceof IntegerObject)) {
                Assert.fail("expected is not instance of IntegerObject");
            }
            IntegerObject expectIntegerObject = (IntegerObject) expected;
            IntegerObject integerObject = (IntegerObject) actual;
            Assert.assertEquals(integerObject.value(), expectIntegerObject.value());

        } else if (actual instanceof BooleanObject){

            if (!(expected instanceof BooleanObject)){
                Assert.fail("expected is ot instance of BooleanObject");
            }
            BooleanObject expectBooleanObject = (BooleanObject) expected;
            BooleanObject booleanObject = (BooleanObject) actual;
            Assert.assertEquals(booleanObject, expectBooleanObject);

        } else if (actual instanceof NullObject) {
            Assert.assertTrue(expected instanceof NullObject);

        } else {
            /* 如果遇到FunctionObject也应该报错 */

            Assert.fail("Invalid actual type: " + actual.getClass().getName());
        }
    }

    @DataProvider(name = "testEvalFunctionExpressionDataProvider")
    public Object[][] testEvalFunctionExpressionDataProvider(Method method){
        return new Object[][]{
                new Object[]{"let a = 7; let func = fn(x,y,z){x + a; y + 2 * a; return z + 3 * a;}; return func(0,0,1) + 1;", new IntegerObject(23)},
                new Object[]{"let func = fn(x){if (x > 0) {return true;} return false; }; return !func(-1);", BooleanObject.TRUE},
                new Object[]{"let a = 1; let f = fn(){return -a;}; f();", new IntegerObject(-1)},
                new Object[]{"let f = fn(x, y, z){}; return f(0,0,0);", NullObject.NULL},
                new Object[]{"let c = 3; let f = fn(x){return x * 2 + c;} let b = 1; return f(2) + b;",new IntegerObject(8)},
                /* 闭包：函数返回函数，捕获定义时的 x */
                new Object[]{"let newAdder = fn(x) { fn(y) { x + y; }; }; let addTwo = newAdder(2); addTwo(2);", new IntegerObject(4)},
                /* 递归：函数体通过闭包引用自身 */
                new Object[]{"let fact = fn(n) { if (n == 0) { 1 } else { n * fact(n - 1); } }; fact(5);", new IntegerObject(120)},
                /* 高阶函数：函数作为参数 */
                new Object[]{"let apply = fn(f, x) { f(x); }; let double = fn(n) { n * 2; }; apply(double, 5);", new IntegerObject(10)},
                /* 闭包：inner 捕获定义时的 x=5，实参 y 取自调用方（顶层 y=100）→ 100 + 5 = 105
                 * 注：此例主要验证"闭包捕获 x"，不严格验证"实参在调用方求值"——
                 * 因为 y 也能通过 inner 的定义链（外层=顶层）找到 y=100。
                 * 真正戳"实参在调用方求值"的是下面 add/outer 那条。 */
                new Object[]{"let make = fn() { let x = 5; fn(y) { y + x; }; }; let inner = make(); let y = 100; inner(y);", new IntegerObject(105)},
                /* 实参必须在【调用方】求值：add 定义在顶层（无 x），却在 outer 的局部作用域里被调用，
                 * 实参 x 只能从 outer 的局部环境(x=5)取；若从 add 的定义环境(顶层无 x)取就会崩。 */
                new Object[]{"let add = fn(a, b) { a + b; }; let outer = fn() { let x = 5; add(x, 10); }; outer();", new IntegerObject(15)},
        };
    }

    @Test(dataProvider = "testEvalFunctionExpressionDataProvider")
    public void testEvalFunctionExpression(String input, MonkeyObject expected){
        Environment environment = new Environment(null);
        MonkeyObject actual = testProgram(input, environment);
        compare(actual, expected);
    }
}
