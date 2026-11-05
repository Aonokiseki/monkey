package priv.zhaoyang.monkey.eval;

import priv.zhaoyang.monkey.ast.*;
import priv.zhaoyang.monkey.object.BooleanObject;
import priv.zhaoyang.monkey.object.IntegerObject;
import priv.zhaoyang.monkey.object.MonkeyObject;
import priv.zhaoyang.monkey.object.NullObject;

import java.util.List;

/* 求值, 目标:
 * 1.IntegerLiteral求值
 * 2.BooleanLiteral求值
 *  */
public class Evaluator {

    public static MonkeyObject eval(Node node){

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
        else {
            /* node本身为空指针的情况直接返回NullObject.NULL, 这里不需要考虑空指针异常 */
            throw new IllegalStateException(String.format("Invalid Node[%s]", node.getClass().getName()));
        }
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
}
