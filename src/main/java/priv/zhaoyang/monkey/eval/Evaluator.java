package priv.zhaoyang.monkey.eval;

import priv.zhaoyang.monkey.ast.*;
import priv.zhaoyang.monkey.object.*;
import priv.zhaoyang.monkey.token.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* 求值, 目标:
 * 1.IntegerLiteral求值
 * 2.BooleanLiteral求值
 * 3.前缀表达式
 * 4.中缀表达式
 * 5.if表达式
 * 6.return语句求值
 * 7.let语句求值
 * 8.函数和表达式求值
 * 9.while循环表达式求值
 * 10.字符串拼接
 * 11.数组定义和索引求值
 * 12.map的定义和索引求值
 * 13.内置函数,字符串索引
 *  */
public class Evaluator {

    public static MonkeyObject eval(Node node,
            /* 由于引入对LetStatement的求值添加参数: */Environment environment){

        /* 实现时遵守这样一个村规:
         * eval只负责分发和转型, 处理丢给其它方法, 力求简洁 */

        /* eval(null) 直接返回Monkey的NULL */
        if (node == null) {
            return NullObject.NULL;
        }

        if (node instanceof Program){
            /* 在实现ReturnStatement时, 可能因短路返回前一个return
             * 比如 if (10 > 1) {if (10 > 1) return 1;} return 2;
             *
             * 为此引入了ReturnValue,
             * ReturnValue本身实现了MonkeyObject, 又包含了一个MonkeyObject, 遍历statement并求值时, 遇到可以返回的值时
             * 就填入ReturnValue中,
             * 相当于给原本要返回的IntegerObject, BooleanObject, NullObject打了标, 标记这个实例可以返回了
             *
             * 这样一来产生了微妙的差异: Return语句返回的IntegerObject, BooleanObject, NullObject 它们此时还是ReturnValue包裹的
             * 而普通表达式返回的IntegerObject, BooleanObject, NullObject就是自己.
             * 在这里要统一, 将提前返回的ReturnValue解包出来.
             *  */

            /* 在最开始实现时, 还不需要考虑return语句, 可以先这么实现
             * return evalStatements((Program) node);
             */

            MonkeyObject result = evalStatements((Program) node, environment);
            if (result instanceof ReturnValue){
                return ((ReturnValue) result).value();
            }
            return result;
        }

        if (node instanceof ExpressionStatement) {
            ExpressionStatement expressionStatement = (ExpressionStatement) node;
            Expression expression = expressionStatement.expression();
            return eval(expression, environment);
        }

        if (node instanceof IntegerLiteral) {
            return evalIntegerLiteral((IntegerLiteral) node);
        }

        if (node instanceof BooleanLiteral) {
            return evalBooleanLiteral((BooleanLiteral) node);
        }

        /* 处理前缀表达式 */
        if (node instanceof PrefixExpression) {
            return evalPrefixExpression((PrefixExpression) node, environment);
        }

        /* 处理中缀表达式 */
        if (node instanceof InfixExpression) {
            return evalInfixExpression((InfixExpression) node, environment);
        }

        /* 处理if表达式 */
        if (node instanceof IfExpression) {
            return evalIfExpression((IfExpression) node, environment);
        }

        if (node instanceof BlockStatement){
            return evalBlockStatement((BlockStatement) node, environment);
        }

        if (node instanceof ReturnStatement) {
            return evalReturnStatement((ReturnStatement) node, environment);
        }

        if (node instanceof LetStatement){
            return evalLetStatement((LetStatement) node, environment);
        }

        if (node instanceof Identifier){
            return evalIdentifier((Identifier) node, environment);
        }

        if (node instanceof FunctionExpression){
            return evalFunctionExpression((FunctionExpression) node, environment);
        }

        if (node instanceof CallExpression) {
            return evalCallFunction((CallExpression)node, environment);
        }

        if (node instanceof WhileExpression){
            return evalWhileExpression((WhileExpression) node, environment);
        }

        if (node instanceof StringLiteral) {
            return evalStringLiteral((StringLiteral) node, environment);
        }

        if (node instanceof ArrayLiteral) {
            return evalArrayLiteral((ArrayLiteral) node, environment);
        }

        if (node instanceof IndexExpression) {
            /* 要考虑这是map还是list */
            return evalIndexExpression((IndexExpression) node, environment);
        }

        if (node instanceof MapLiteral) {
            return evalMapLiteral((MapLiteral) node, environment);
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

    private static MonkeyObject evalStatements(Program program, Environment environment){
        List<Statement> statements = program.statements();

        MonkeyObject result = NullObject.NULL;
        for(Statement statement : statements){
            result = eval(statement, environment);

            /* 在实现evalReturnExpression时需要加判断 */
            if (result instanceof ReturnValue) {
                return result;
            }
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
    private static MonkeyObject evalPrefixExpression(PrefixExpression prefixExpression, Environment environment){
        String operator = prefixExpression.operator();
        /* ↓这一行当时没想出来, 可能也加重了我按照AST判断的倾向, 需要先搞出右值 */
        MonkeyObject right = eval(prefixExpression.expression(), environment);

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

    /* ==================== 中缀表达式求值 ====================
     * 【心路历程·踩坑记录】
     * 1. 一开始完全漏掉了 null：旧实现只分派 int/int 和 bool/bool 两种组合，遇到 null 直接掉到最后抛异常。
     *    但 Monkey 里 null 是一等值（-false 就会产出 NULL），它完全可能出现在 == / != 两侧。
     * 2. 后来补 null 又“补过头”：单独给「左右都是 NULL」写分支，还一度把它无条件返回 TRUE，
     *    于是 (-false) != (-false) 错成 true、(-false) < (-false) 也错成 true——只盯着 null==null，
     *    忘了还有 != 和其它运算符。
     * 3. 再往后，单侧 null（情况4/5）调用 helper 时忘了写 return，结果算出来被丢掉、掉到最后 throw，
     *    导致 (-false) == 5 也崩。
     * 4. 最根本的偏差：对「类型不匹配」(5 == true) 和「非整数运算」(true + true) 一律抛异常，
     *    而原书对 ==/!= 返回 false/true，对其余运算符返回 NULL。
     *
     * 为什么会这么想？两点心理：
     *   a. 懒狗心理——类型都不一样了，凭什么还要我费劲去比较？
     *   b. 对弱类型不放心——不先校验类型，怎么敢继续往下比？
     * 但 Monkey 是动态语言，类型要到运行时才知道，左侧算出 boolean、右侧算出 int 是常态。
     * 对 == 来说，类型不同直接判「不相等」即可；对其它运算符则给 NULL，而不是让程序崩溃
     * （否则 5 == true 这种本应返回 false 的合法表达式会让程序直接崩，反而更糟）。
     *
     * 【为什么现在这样写就对了】
     * 照搬原书的分派顺序：
     *   - 左右都是整数 → evalIntegerInfixExpression，按“值”比较（+ - * / < > == !=）。
     *   - 否则，== / != 对任意两个值成立：直接比较“是不是同一个对象”（同一性）。
     *     BooleanObject / NullObject 都是单例，== 即身份相等；int vs bool 等类型不同自然不等。
     *   - 剩下的 + - * / < > 只对整数有意义，一律返回 NULL。
     * 于是 null==null→true、null!=null→false、5==true→false、5!=true→true、true+true→null，
     * 全部和原书一致。 */
    private static MonkeyObject evalInfixExpression(InfixExpression infixExpression, Environment environment){
        /* 和前缀一样：先递归求值左右操作数，再按“对象类型 + 运算符”分派，不要看 AST 节点类型 */
        MonkeyObject left = eval(infixExpression.left(), environment);
        MonkeyObject right = eval(infixExpression.right(), environment);
        String operator = infixExpression.operator();

        /* 情况1：左右都是整数 → 整数运算（+ - * / < > == !=） */
        if (left instanceof IntegerObject && right instanceof IntegerObject){
            return evalIntegerInfixExpression((IntegerObject) left, operator, (IntegerObject) right);
        }

        /* 情况2：== 对任意两个值成立（同一性）。整数在上面已按值比较过，
         * 走到这的 boolean / null 都是单例，== 即身份相等；类型不同（如 5 == true）自然不等。 */
        if (operator.equals(TokenType.EQ.literal())) {
            return left == right ? BooleanObject.TRUE : BooleanObject.FALSE;
        }

        /* 情况3：!= 同理，取反 */
        if (operator.equals(TokenType.NOT_EQ.literal())) {
            return left != right ? BooleanObject.TRUE : BooleanObject.FALSE;
        }

        /* 真·情况4: 连接字符串 */
        if (left instanceof StringObject && right instanceof StringObject && operator.equals(TokenType.PLUS.literal())){
            StringObject leftObject = (StringObject) left;
            StringObject rightObject = (StringObject) right;
            return new StringObject(leftObject.value() + rightObject.value());
        }

        /* 情况4：剩下的 + - * / < > 只对整数有意义，其它类型一律 NULL（原书语义） */
        return NullObject.NULL;
    }

    /* operator可以是PLUS, MINUS, ASTERISK, SLASH, LT, GT, EQ, NOT_EQ */
    private static MonkeyObject evalIntegerInfixExpression(IntegerObject left, String operator, IntegerObject right){
        /* PLUS */
        if (operator.equals(TokenType.PLUS.literal())) {
            return new IntegerObject(left.value() + right.value());
        }
        /* MINUS */
        if (operator.equals(TokenType.MINUS.literal())) {
            return new IntegerObject(left.value() - right.value());
        }
        /* ASTERISK */
        if (operator.equals(TokenType.ASTERISK.literal())){
            return new IntegerObject(left.value() * right.value());
        }
        /* SLASH */
        if (operator.equals(TokenType.SLASH.literal())) {
            return new IntegerObject(left.value() / right.value());
        }
        /* LT */
        if (operator.equals(TokenType.LT.literal())) {
            return left.value() < right.value() ? BooleanObject.TRUE : BooleanObject.FALSE;
        }
        /* GT */
        if (operator.equals(TokenType.GT.literal())) {
            return left.value() > right.value() ? BooleanObject.TRUE : BooleanObject.FALSE;
        }
        /* EQ */
        if (operator.equals(TokenType.EQ.literal())) {
            return left.value() == right.value() ? BooleanObject.TRUE : BooleanObject.FALSE;
        }
        /* NOT_EQ */
        if (operator.equals(TokenType.NOT_EQ.literal())) {
            return left.value() != right.value() ? BooleanObject.TRUE : BooleanObject.FALSE;
        }
        return NullObject.NULL;
    }

    /* 对条件表达式求值 */
    private static MonkeyObject evalIfExpression(IfExpression ifExpression, Environment environment){

        MonkeyObject conditionObject = eval(ifExpression.condition(), environment);

        boolean finalCondition = isTruthy(conditionObject);

        /* 如果condition==true, 对consequence求值 */
        if (finalCondition) {
            return eval(ifExpression.consequence(), environment);
        }

        /* 如果没有alternative, 直接返回 */
        /* 这行实际没有必要, 因为进入下一块, 会经过eval, 也会遇到eval(null), 返回空值, emmm, 留着也无妨 */
        if (ifExpression.alternative() == null){
            return NullObject.NULL;
        }

        /* 如果有alternative, 再求值 */
        return eval(ifExpression.alternative(), environment);
    }

    /* 判断ifExpression最终的条件表达式
     *
     * 原来的实现如下
     * if (conditionObject instanceof BooleanObject){}
     * else if (conditionObject instanceof IntegerObject){}
     * else if (conditionObject instanceof NullObject){}
     * else {throw new Exception...}
     *
     * 它有两处值得调整:
     * 1.实现非常冗长, 可以简化为下边的逻辑, 也就是原书的逻辑
     *   一下子就整洁很多, 只抄书是理解不了简洁的写法是如何优化出来的
     * 2.凡没有特别标记为false的, 要求的一律返回true, 为什么会这样, 因为monkey是动态语言
     *   类型不匹配是常态, 不要动辄就抛异常
     *
     *  */
    private static boolean isTruthy(MonkeyObject object){
        if (object == NullObject.NULL){
            return false;
        }
        if (object == BooleanObject.FALSE){
            return false;
        }
        return true;
    }

    /* 对块语句求值 */
    private static MonkeyObject evalBlockStatement(BlockStatement blockStatement, Environment environment) {
        MonkeyObject result = NullObject.NULL;

        for (Statement statement : blockStatement.statements()){
            result = eval(statement, environment);

            /* 在实现evalReturnExpression时需要加判断 */
            if (result instanceof ReturnValue) {
                return result;
            }
        }

        return result;
    }

    /* 处理LetStatement */
    /* 这个方法要求传入Environment, 而eval()需要调用它, 因此eval()也需要传入Environment, 所有调用eval()又会再次受到影响 */
    private static MonkeyObject evalLetStatement(LetStatement letStatement, Environment environment){
        String identifierName = letStatement.identifier().value();
        MonkeyObject evaluated = eval(letStatement.expression(), environment);
        environment.put(identifierName, evaluated);
        return evaluated;
    }

    /* 啊我去, 漏了evalIdentifier了 */
    private static MonkeyObject evalIdentifier(Identifier identifier, Environment environment){

        if (environment.contains(identifier.value())){
            return environment.get(identifier.value(), NullObject.NULL);
        }

        /* 需要加入内置函数支持, 判断identifier是不是内置函数 */
        if (Builtin.contains(identifier.value())){
            return Builtin.get(identifier.value());
        }

        throw new IllegalStateException(String.format(
                "Unknown identifier[type=%s, value=%s] in evalIdentifier()",
                identifier.getClass().getName(), identifier.value()));
    }

    /* 对returnStatement求值 */
    /* 重点考虑这种情况如何实现:
    *
    * 1. 1; return 2; 3; 应该返回2
    * 2. if(10 > 1){ if(10 > 1){ return 1; }} return 2; 应该返回1 */
    private static ReturnValue evalReturnStatement(ReturnStatement returnStatement, Environment environment){
        /* 但是话又说回来了, 都到return了, 还怎么控制之前有没有return, 让我看看书上怎么实现的
         * 啊, 要定义一个returnValue, 还要改造evalProgram(我没有这个函数, 不考虑), evalBlockStatement, evalStatements
         * 只要有循环对statement求值, 就在循环中检测这是不是个returnObject, 是就提前返回 */
        MonkeyObject returnedObject = eval(returnStatement.returnExpression(), environment);
        ReturnValue returnValue = new ReturnValue(returnedObject);
        return returnValue;
    }

    /* 对函数表达式以及调用表达式求值 */
    /* 考虑几种情况:
     * ① let a = 7; fn(x,y,z){x + a; y + 2 * a; z + 3 * a;}; return fn(0,0,1) + 1; 预期结果是23
     *
     * ② fn(x){if (x > 0) {return true;} return false; }; return !fn(-1); 预期是true
     *
     * ③ let a = 1 fn(){return -a;}; fn(); 预期是-1
     *
     * ④fn(x, y, z){}; return fn(0,0,0); 预期是NULL
     *
     * ⑤let c = 3; fn(x){return x * 2 + c;} let b = 1; return fn(2) + b; 预期8
     *
     */
    private static MonkeyObject evalFunctionExpression(FunctionExpression functionExpression, Environment environment){
        /* 重新翻了一遍书, 大开眼界, 大概是这样的:
         * 在对函数定义的求值, 就是将抽象语法树捆绑上当前的Environment, 将这个整体视为一个函数体对象, 返回就可以
         * 不需要其它操作.
         *
         * 这就解决了我卡在这里的疑问: 那就是没有调用, 什么时候准备局部变量表, 什么时候将这些形参注册到局部变量表中,
         * 又要如何将局部变量表传递给调用方, 又要如何让调用方知道局部变量表的顺序(你瞧, 这毕竟是两个函数)
         *
         * 书上告诉我, 对定义的求值, 就是不处理, 一股脑丢给callFunction就可以了
         * 先定义FunctionObject
         *
         * emmm, 名为求值, 实际上还在定义
         * */
        return new FunctionObject(functionExpression, environment);
    }

    /* ==================== 调用表达式求值·踩坑记录 ====================
     * 【最重要的坑：两个 environment 弄反了】
     * 调用函数涉及两个环境，极易 swap：
     *   1. 实参(argument)是“调用处”写出的表达式 → 必须在【调用方环境 environment】里求值。
     *   2. 局部变量表的 outer（闭包往外看的出口）→ 必须是【定义时环境 functionObject.environment()】，
     *      这样函数体才能看见它定义处的变量（闭包）。
     * 我第一次实现把这两处用反了：实参用定义环境、局部表 outer 用调用方环境。
     * 因为当时 5 个用例里函数都在顶层定义、也在顶层调用，两个环境恰好相同，swap 完全测不出来——
     * 直到补了闭包用例 `let newAdder = fn(x){fn(y){x+y;}}; let addTwo=newAdder(2); addTwo(2);`
     * 才暴露出 addTwo(2) 找不到 x（x 在 addTwo 的定义环境里，而我把局部表 outer 设成了调用方顶层）。
     * 教训：两个语义不同的 environment 别共用同一个模糊身份；写之前先分清“实参在谁家求值、函数体往谁家找”。
     *
     * 【ReturnValue 要在函数边界解包】
     * 函数体里的 return 会让 eval(body) 返回 ReturnValue 包装，必须 unwrap 再交给调用方，
     * 否则 !fn(...) 拿到的是 ReturnValue 而不是 Boolean。程序顶层解包过一次，函数边界是第二个解包点。 */
    private static MonkeyObject evalCallFunction(CallExpression callExpression, Environment environment){
        /* 在【调用方】环境里求值“被调用的函数表达式”，得到 FunctionObject */
        MonkeyObject maybeAFunctionObject = eval(callExpression.function(), environment);

        /* 如果它是内置函数, 用内置函数处理 */
        if (maybeAFunctionObject instanceof BuiltinFunction) {

            List<MonkeyObject> builtinArguments = new ArrayList<>();
            for(Expression argument : callExpression.arguments()){
                builtinArguments.add(eval(argument, environment));
            }
            return ((BuiltinFunction) maybeAFunctionObject).process(builtinArguments);
        }

        if (!(maybeAFunctionObject instanceof FunctionObject)){
            throw new IllegalStateException(String.format(
                    "maybeAFunctionObject[type=%s] in not instance of FunctionObject in evalCallFunction()", maybeAFunctionObject.getClass().getName()));
        }
        FunctionObject functionObject = (FunctionObject) maybeAFunctionObject;
        List<Identifier> identifiers = functionObject.functionExpression().parameters();

        /* 局部变量表的 outer = 定义时环境（闭包靠它往外看）；实参稍后在调用方环境求值 */
        Environment localVariableTable = new Environment(functionObject.environment());

        List<Expression> arguments = callExpression.arguments();
        MonkeyObject argumentExpression = NullObject.NULL;

        if (arguments.size() != identifiers.size()){
            throw new IllegalStateException(String.format(
                    "arguments.size(%d) not equal identifiers.size(%d), while arguments=[%s], identifiers=[%s]",
                    arguments.size(), identifiers.size(), arguments, identifiers));
        }

        for (int i = 0; i < arguments.size(); i++){
            Expression argument = arguments.get(i);
            /* 实参在【调用方】环境求值 */
            argumentExpression = eval(argument, environment);
            /* 形参名 → 实参值，绑进局部变量表 */
            localVariableTable.put(identifiers.get(i).value(), argumentExpression);
        }

        /* 在局部变量表（含定义时 outer）里求值函数体，并解包 ReturnValue */
        MonkeyObject monkeyObject = eval(functionObject.functionExpression().body(), localVariableTable);
        if (monkeyObject instanceof ReturnValue){
            return ((ReturnValue)(monkeyObject)).value();
        }
        return monkeyObject;
    }

    /* 解析循环表达式 */
    private static MonkeyObject evalWhileExpression(WhileExpression whileExpression, Environment environment){

        MonkeyObject result = NullObject.NULL;
        while(true){

            MonkeyObject conditionObject = eval(whileExpression.condition(), environment);

            /* 啥, 我画蛇添足了, 没必要断言它一定得是BooleanObject, isTruthy已经囊括了所有可能! */
            boolean isTruth = isTruthy(conditionObject);
            if (!isTruth){
                break;
            }

            result = eval(whileExpression.body(), environment);

            /* 再吃一堑, 再看看能不能张一智, 循环体内遇到return的情况 */
            if (result instanceof ReturnValue) {
                return ((ReturnValue) result).value();
            }
        }
        return result;
    }

    /* 对字符串求值 */
    /* 不考虑字符串常量池, 永远new一个出来 */
    private static MonkeyObject evalStringLiteral(StringLiteral stringLiteral, Environment environment){
        return new StringObject(stringLiteral.value());
    }

    /* 对数组求值 */
    /* 这还是定义 */
    private static MonkeyObject evalArrayLiteral(ArrayLiteral arrayLiteral, Environment environment){
        List<MonkeyObject> objects = new ArrayList<>();

        List<Expression> expressions = arrayLiteral.expressionList();
        for (Expression expression : expressions) {
            objects.add(eval(expression, environment));
        }

        ArrayObject arrayObject = new ArrayObject(objects);
        return arrayObject;
    }

    /* 对数组索引的求值, 比如
     * let a = [1,2,3]; a[1] 要计算出这是2
     * let [1,2,3][1], 也要计算出这是2
     *
     */
    private static MonkeyObject evalIndexArrayLiteral(ArrayObject arrayObject, Expression index, Environment environment){

        List<MonkeyObject> values = arrayObject.values();

        MonkeyObject indexObject = eval(index, environment);

        if (!(indexObject instanceof IntegerObject)) {
            throw new IllegalStateException(String.format(
                    "indexObject[type=%s] is not instance of IntegerObject", indexObject.getClass().getName()));
        }

        IntegerObject integerObject = (IntegerObject) indexObject;
        if (integerObject.value() >= values.size() || integerObject.value() < 0) {
            return NullObject.NULL;
        }

        MonkeyObject result = arrayObject.values().get((int)(integerObject.value()));
        return result;
    }

    /* 对"定义map"的求值 */
    private static MonkeyObject evalMapLiteral(MapLiteral mapLiteral, Environment environment){
        Map<Expression, Expression> content = mapLiteral.map();

        Map<MonkeyObject, MonkeyObject> objectMap = new HashMap<>();
        for(Map.Entry<Expression, Expression> e : content.entrySet()){

            MonkeyObject evaluatedKey = eval(e.getKey(), environment);
            MonkeyObject evaluatedValue = eval(e.getValue(), environment);
            objectMap.put(evaluatedKey, evaluatedValue);
        }

        return new MapObject(objectMap);
    }

    /* "解析map"的求值, 例如 map["key1"] */
    private static MonkeyObject evalIndexMapLiteral(MapObject mapObject, Expression index, Environment environment){

        Map<MonkeyObject, MonkeyObject> content = mapObject.map();

        MonkeyObject indexObject = eval(index, environment);

        /* 我知道了, 要是不实现hashcode和equals, 就得在这里展开: 先判断MonkeyObject的实际类型, 转型后在对比value
         * 对上了再用当前这个MonkeyObject去获取代表value的MonkeyObject
         * 可现在实现了, 那就无需考虑 */

        /* 漏了一处, 判断类型是否可散列 */
        if (!(indexObject instanceof Hashable)){
            throw new IllegalStateException(String.format(
                    "indexObject[type=%s] is not instance of Hashable.", indexObject.getClass().getName()));
        }

        return content.getOrDefault(indexObject, NullObject.NULL);
    }

    /* 初步解析索引表达式, 根据索引目标的不同分发给不同处理方法 */
    private static MonkeyObject evalIndexExpression(IndexExpression indexExpression, Environment environment){
        Expression target = indexExpression.target();

        /* 直接判断字面量, 遇到Identifier铁定失败, 不知道原因, 那这样, 先对target求值, 根据求值结果的类型再分发 */
        MonkeyObject evaluatedTarget = eval(target, environment);

        if (evaluatedTarget instanceof ArrayObject) {
            return evalIndexArrayLiteral((ArrayObject) evaluatedTarget, indexExpression.index(), environment);
        }
        if (evaluatedTarget instanceof MapObject) {
            return evalIndexMapLiteral((MapObject) evaluatedTarget, indexExpression.index(), environment);
        }

        /* 补充内置函数的时候顺便补充了这里 */
        if (evaluatedTarget instanceof StringObject){
            return evalIndexStringLiteral((StringObject) evaluatedTarget, indexExpression.index(), environment);
        }

        throw new IllegalStateException(String.format(
                "Invalid target type[%s] in indexExpression", evaluatedTarget.getClass().getName()));
    }

    /* 字符串的索引表达式求值 */
    private static MonkeyObject evalIndexStringLiteral(StringObject stringObject, Expression index, Environment environment){
        String content = stringObject.value();

        MonkeyObject indexObject = eval(index, environment);
        if (!(indexObject instanceof IntegerObject)) {
            throw new IllegalStateException(String.format(
                    "indexObject[type=%s] is not instance of IntegerObject", indexObject.getClass().getName()));
        }

        IntegerObject integerObject = (IntegerObject) indexObject;
        if (integerObject.value() >= content.length() || integerObject.value() < 0) {
            return NullObject.NULL;
        }
        String resultContent = content.substring((int)integerObject.value(), (int)integerObject.value() + 1);
        return new StringObject(resultContent);
    }
}

