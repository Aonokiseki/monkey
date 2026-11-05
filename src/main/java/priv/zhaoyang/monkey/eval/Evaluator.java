package priv.zhaoyang.monkey.eval;

import priv.zhaoyang.monkey.ast.*;
import priv.zhaoyang.monkey.object.BooleanObject;
import priv.zhaoyang.monkey.object.IntegerObject;
import priv.zhaoyang.monkey.object.MonkeyObject;
import priv.zhaoyang.monkey.object.NullObject;
import priv.zhaoyang.monkey.token.TokenType;

import java.util.List;

/* 求值, 目标:
 * 1.IntegerLiteral求值
 * 2.BooleanLiteral求值
 * 3.前缀表达式
 *  */
public class Evaluator {

    public static MonkeyObject eval(Node node){

        /* 实现时遵守这样一个村规:
         * eval只负责分发和转型, 处理丢给其它方法, 力求简洁 */

        /* eval(null) 直接返回Monkey的NULL */
        if (node == null) {
            return NullObject.NULL;
        }

        if (node instanceof Program){
            return evalStatements((Program) node);
        }

        if (node instanceof ExpressionStatement) {
            ExpressionStatement expressionStatement = (ExpressionStatement) node;
            Expression expression = expressionStatement.expression();
            return eval(expression);
        }

        if (node instanceof IntegerLiteral) {
            return evalIntegerLiteral((IntegerLiteral) node);
        }

        if (node instanceof BooleanLiteral) {
            return evalBooleanLiteral((BooleanLiteral) node);
        }

        /* 处理前缀表达式 */
        if (node instanceof PrefixExpression) {
            return evalPrefixExpression((PrefixExpression) node);
        }

        /* 没必要执行如下判断:
         * node instanceof Expression
         * node instanceof Statement
         * 因为接口不定义行为, 转型为interface, 还要继续向具体类型转型
         * 换句话说, 你为什么执行这个判断呢?
         * node instanceof Node
         * 转型了么? 如转!
         *  */

        /* 原书这个分支直接返回nil, emmm, 作者明明定义了object.NULL
         * 我这里的实现, 一开始想返回NullObject.NULL, 不过比起静默失败, 还有更好的做法:抛出异常
         * 那这样的话, NullObject.NULL可能用不上 */

        /* node本身为空指针的情况直接返回NullObject.NULL, 这里不需要考虑空指针异常 */
        throw new IllegalStateException(String.format("Invalid Node[%s]", node.getClass().getName()));
    }

    private static MonkeyObject evalStatements(Program program){
        List<Statement> statements = program.statements();

        MonkeyObject result = NullObject.NULL;
        for(Statement statement : statements){
            result = eval(statement);
        }

        return result;
    }

    private static MonkeyObject evalIntegerLiteral(IntegerLiteral integerLiteral){
        return new IntegerObject(integerLiteral.value());
    }

    private static MonkeyObject evalBooleanLiteral(BooleanLiteral booleanLiteral){
        return booleanLiteral.value() ? BooleanObject.TRUE : BooleanObject.FALSE;
    }

    /* 【教训·旧实现】前缀表达式要先“求值右操作数、再按对象类型分派”，不要按 AST 节点类型分派。
     * 旧实现曾按 `expression instanceof IntegerLiteral / BooleanLiteral` 分派，简单表达式没问题，
     * 但操作数一旦是嵌套表达式（!!true、-(-5)），它就是一个 PrefixExpression 节点，
     * 走不到任何分支、直接抛异常。正确姿势见下：先 right = eval(expression) 拿到 MonkeyObject，
     * 再按 operator 与对象类型分派。 */
    private static MonkeyObject evalPrefixExpression(PrefixExpression prefixExpression){
        String operator = prefixExpression.operator();
        /* ↓这一行当时没想出来, 可能也加重了我按照AST判断的倾向, 需要先搞出右值 */
        MonkeyObject right = eval(prefixExpression.expression());

        if (operator.equals(TokenType.MINUS.literal())) {
            return evalMinusPrefixOperatorExpression(right);
        }

        if (operator.equals(TokenType.BANG.literal())) {
            return evalBangPrefixOperatorExpression(right);
        }

        throw new IllegalStateException("unknown prefix operator: " + operator);

    }

    /* 对带着BANG的前缀表达式求值。! 是“真值判断”，作用于任何值（原书语义）：
     *   !true  -> false
     *   !false -> true
     *   !null  -> true
     *   !整数   -> false（整数都是“真”值，取反为假） */
    private static MonkeyObject evalBangPrefixOperatorExpression(MonkeyObject right){
        if (right instanceof IntegerObject) {
            return BooleanObject.FALSE;
        }
        if (right instanceof BooleanObject) {
            BooleanObject booleanObject = (BooleanObject) right;
            return booleanObject == BooleanObject.TRUE ? BooleanObject.FALSE : BooleanObject.TRUE;
        }
        if (right instanceof NullObject) {
            return BooleanObject.TRUE;
        }

        throw new IllegalStateException(String.format(
                "unsupported operand type for ! : [%s]", right.type().name()));
    }

    /* MINUS 是算术取负，只对整数有意义；非整数（boolean、null）一律返回 NULL，与原书一致。 */
    private static MonkeyObject evalMinusPrefixOperatorExpression(MonkeyObject right){
        if (right instanceof IntegerObject) {
            IntegerObject integerObject = (IntegerObject) right;
            return new IntegerObject(-integerObject.value());
        }
        return NullObject.NULL;
    }
}
