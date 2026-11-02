package priv.zhaoyang.monkey.parser;

/* 目标1: 解析let语句
   let x = 10;
   let y = 15;

   let add = fn(x, y){
       x + y;
   };

   要能识别出三个语句, 三个标识符x, y, add

   目标2: 添加最基础的错误处理机制

   目标3: 解析Return语句

   目标4: 解析expression
      4.1: 添加prefix和infix的词典
      4.2: 添加标识符的解析(标识符就是expression)
      4.3: 添加数字字面量的解析
      4.4: 添加前缀表达式的解析, 如 -5
      4.5: 中缀表达式 如 5 + 5
      4.6: 实现普拉特递归下降
      4.7: 实现布尔字面量
      4.8: 实现分组表达式
      4.9: if表达式
 * */


import priv.zhaoyang.monkey.ast.*;
import priv.zhaoyang.monkey.lexer.Lexer;
import priv.zhaoyang.monkey.token.Token;
import priv.zhaoyang.monkey.token.TokenType;

import java.util.*;


public class Parser {

    private Lexer lexer;
    /* 和Lexer不一样, 因为是一边词法分析一边解析token, 不能用下标来指向当前和下一个token */
    private Token current;
    private Token next;
    private List<String> errors;

    private Map<TokenType, PrefixParseFunction> prefixParseFunctionMap;
    private Map<TokenType, InfixParseFunction> infixParseFunctionMap;

    public Parser(Lexer lexer){
        this.lexer = lexer;
        this.errors = new ArrayList<>();

        this.prefixParseFunctionMap = new HashMap<>();
        this.infixParseFunctionMap = new HashMap<>();

        /* 前缀注册 */
        registerPrefix(TokenType.IDENT, this::parseIdentifierExpression);
        registerPrefix(TokenType.INT, this::parseIntegerLiteral);
        registerPrefix(TokenType.MINUS, this::parsePrefixExpression);
        registerPrefix(TokenType.BANG, this::parsePrefixExpression);
        registerPrefix(TokenType.TRUE, this::parseBooleanLiteral);
        registerPrefix(TokenType.FALSE, this::parseBooleanLiteral);
        registerPrefix(TokenType.LPAREN, this::parseGroupExpression);
        registerPrefix(TokenType.IF, this::parseIfExpression);

        /* 中缀注册 */
        registerInfix(TokenType.PLUS, this::parseInfixExpression);
        registerInfix(TokenType.MINUS, this::parseInfixExpression);
        registerInfix(TokenType.SLASH, this::parseInfixExpression);
        registerInfix(TokenType.ASTERISK, this::parseInfixExpression);
        registerInfix(TokenType.EQ, this::parseInfixExpression);
        registerInfix(TokenType.NOT_EQ, this::parseInfixExpression);
        registerInfix(TokenType.LT, this::parseInfixExpression);
        registerInfix(TokenType.GT, this::parseInfixExpression);

        /* 连续执行2次moveNext, 让current和next都有指向的对象 */
        moveNext();
        moveNext();
    }

    private void moveNext(){
        current = next;
        next = lexer.nextToken();
    }

    private void registerPrefix(TokenType type, PrefixParseFunction fn){
        prefixParseFunctionMap.put(type, fn);
    }

    private void registerInfix(TokenType type, InfixParseFunction fn){
        infixParseFunctionMap.put(type, fn);
    }

    public Program parseProgram(){
        Program program = new Program();

        List<Statement> statements = new ArrayList<>();
        while(current.type != TokenType.EOF){

            /* 思考: 现在加入了BlockStatement了, 就不能直接parseStatement(), 是不是需要套一层?
             * 不不不, 先不要, 我这里需要BlockStatement是因为我遇到了IfExpression */
            Statement statement = parseStatement();
            if (statement != null) {
                statements.add(statement);
            }
            /* 这里别忘了挪动指针 */
            moveNext();

        }

        program.putStatements(statements);
        return program;
    }

    private Statement parseStatement(){

        if(current.type.equals(TokenType.LET)){

            return parseLetStatement();

        } else if (current.type.equals(TokenType.RETURN)){

            return parseReturnStatement();

        }
        /* 中间还有其它语句， 先不管 */
        /* 当你实现了LetStatement和ReturnStatement就要继续实现了, 没有其它了, 只剩ExpressionStatement了 */
        else {
            return parseExpressionStatement();
        }
    }

    private LetStatement parseLetStatement(){
        /* 先判断token是不是let
         * 错误的，原书不是校验是不是let, let的校验已经由外部parseStatement完成了, 这里应该校验这里是不是identifier
         * 不过冗余校验就冗余吧 */
        if (current.type != TokenType.LET){
            return null;
        }

        LetStatement letStatement = new LetStatement(current);
        /* 本函数的两个重要校验1:校验是不是标识符 */
        if (!expectPeek(TokenType.IDENT)) {
            return null;
        }

        Identifier identifier = new Identifier(current, current.literal);
        letStatement.putIdentifier(identifier);

        /* 下一个Token.ASSIGN之前, 都是identifier */
        /* 一开始用了while，为了防止没盖住 a_b_c_d 这种标志符名称, 但是在Lexer中已经将其视为一个整体了
         * 只需要挪动一次指针就行。
         * 等下这个if块好像没进...
        if(next.type != TokenType.EOF && next.type != TokenType.ASSIGN) {
            moveNext();
        }
         * 但故意放到这里, 你看下重新实现时是不是又犯了同一个错误
         */

        /* 遇到Token.ASSIGN, 挪动指针 */
        /* 本函数的两个重要校验2:校验是不是赋值符 */
        if (!expectPeek(TokenType.ASSIGN)){
            return null;
        }

        /* 后边直至;，都是一个expression，至于expression怎么解析，先不管，假设有个上帝函数帮我们解析完成了 */
        /* 原书中这里是 current 指向的token来判断的
         * 要改成current.type != TokenType.SEMICOLON, 可以避免模糊的边界: next是半角分号如何处理 */
        while(current.type != TokenType.EOF && current.type != TokenType.SEMICOLON){
            /* 中间的处理以后再说 */
            moveNext();
        }

        /* 返回 */
        return letStatement;
    }

    public List<String> errors(){
        return Collections.unmodifiableList(errors);
    }

    /* 这么一说，这个方法之前是真的没在意 */
    private void peekError(TokenType tokenType){
        String message = String.format("expected next token to be %s, got %s instead", tokenType.literal(), next.type.literal());
        errors.add(message);
    }

    private boolean expectPeek(TokenType tokenType){
        if (next.type == tokenType){
            moveNext();
            return true;
        }
        peekError(tokenType);
        return false;
    }

    private ReturnStatement parseReturnStatement(){
        /* return {expression}; */
        /* 这次长教训, 外部已经校验了return关键字, 这里不校验 */
        /* 也不存在identifier, 也没有assign */
        ReturnStatement returnStatement = new ReturnStatement(current);

        /* 这里又遗漏了moveNext() */
        /* LetStatement是通过校验next.type==IDENT顺便移动了位置
         * return 后无 identifier/assign 可校验，expectPeek 没有机会帮忙前进，
         * 所以必须显式 moveNext() 越过 return 本身，否则 while 会从 return 开始跳 */
        moveNext();

        /* 同样不管expression 的处理 */
        while (current.type != TokenType.EOF && current.type != TokenType.SEMICOLON){
            moveNext();
        }

        return returnStatement;
    }

    /* 等下, 这里应该是递归下降的主流程, 那还需要前置操作, 以后再说 */
    /* 递归下降是后话, 这里是处理所有表达式的通用方法, 会用到prefixParseFunctionMap和infixParseFunctionMap */
    private Expression parseExpression(Precedence parentPrecedence){

        /* current肯定是expression的一部分, 所以应该先new一个Expression
         * 但是等等, Expression是一个interface，不能new, 要先细致到是哪个具体类的实例才能new,
         * 这就必须要查表, 也就是prefixParseFunctionMap */
        PrefixParseFunction prefixParseFunction = prefixParseFunctionMap.get(current.type);

        if (prefixParseFunction == null) {
            noPrefixParseFnError();
            return null;
        }
        Expression expression = prefixParseFunction.process();

        /* 思考回路: 就是这里了，递归下降的核心逻辑, 应该是for循环判断operator的优先级
         * 之所以要这样, 是因为要考虑 5 + 2 * 3 这种右边粘性更强的可能
         * 偶然想起来acwj这里针对token准备了一个map, monkey怎么做的忘了, 不过可以先试试
         * 字典构造完了，现在的问题是谁和谁比，肯定有一个是current，另外一个应该由外部传入, 改下形参
         * 那应该是这样的: 只要current的优先级比parent的优先级高，也就是粘性更强, 那就一直优先右结合 */

        /* 易错点1: 应该是使用next来判断优先级, 举个例子:
         * 解析 1 + 2; 在解析1时, current就指向了1, next 指向了 '+', 如果用current比较, 直接取最低优先, 不进入循环,
         * 后方的 +2 成为新的表达式 */
        /* 易错点2: 要用 SEMICOLON 判断是否结束语句 */

        /* 暗坑: 循环外计算优先级, 容易导致 1 + 2 * 3 这种表达式会计算错误, 要改为在循环中计算 */
        while(next.type != TokenType.SEMICOLON &&
                PrecedenceMap.get(next.type).value() > parentPrecedence.value()){
            /* 思考回路: 现在该用到中缀表达式词典了 */
            /* 易错点4: 这里也不能忘了用 运算符查找(也就是next指向的token) 中缀函数 */
            InfixParseFunction infixParseFunction = infixParseFunctionMap.get(next.type);

            /* 易错点4: 假如没有对应的中缀表达式, 应该返回已经解析完成的部分,而不是null */
            if (infixParseFunction == null){
                return expression;
            }
            /* 易错点5: 解析完了infixParseFunction, 要moveNext() */
            moveNext();

            /* 思考: 欸欸欸, 我是不是自己写出来了! */
            /* 嘿嘿, 把返回值忘了 */
            expression = infixParseFunction.process(expression);
        }

        return expression;
    }


    /* 解析表达式语句，在monkey中就是 {expression} 和 TokenType.SEMI */
    private ExpressionStatement parseExpressionStatement(){

        ExpressionStatement expressionStatement = new ExpressionStatement(current);

        /* 这里实现的有问题, 既然你已经知道表达式语句就是 {expression} 和 TokenType.SEMI,
         * 那么处理expresion应该交给parseExpression(), 这里只要moveNext()越过TokenType.SEMICOLON即可
         *  */

        /* 有问题的实现
        if(current.type == TokenType.IDENT){
            Identifier identifier = parseIdentifier();
            return expressionStatement.putExpression(identifier);
        }
         还有别的，暂时先不管
         else {
            return expressionStatement;
        }
        */
        expressionStatement.putExpression(parseExpression(Precedence.LOWEST));

        /* 处理SEMI */
        /* 出错啦, 只在确实遇到分号时才消费，否则会把下一条语句的关键字吃掉。
         * 因此不能是无条件挪动指针
         * moveNext();
         *
         * 举个例子，这还是个异常场景
         * let = 10; let foo_bar ...
         *
         * 由于第一句解析遇到异常, =, 10和分号都残留。current仍然指向了第一个let
         * 初始: current=let(T0)
         * ① parseProgram.moveNext():   current = = (T1)
         * ② parseExprStmt.moveNext():  current = 10 (T2)      ← 帮凶第1次
         * ③ parseProgram.moveNext():   current = ; (T3)
         * ④ parseExprStmt.moveNext():  current = let (T4)     ← 帮凶第2次，把 current 停到第二个 let 上
         * ⑤ parseProgram.moveNext():   current = foo_bar (T5) ← 第二个 let 在这里被吞
         *
         * 修复后，parseExpressionStatement 只在 next 是分号时才前进去消费它；否则原地不动，把前进交给 parseProgram，
         * 于是残留 token 被一个接一个地推进，不会一次跳两个。
         * 换句话说，每当解析异常，异常的每个token都被当作了一个新的statement
         * 依靠parseProgram的moveNext一个一个的移动指针，就不会让第二个let被吞掉
         * */
        if (next.type == TokenType.SEMICOLON){
            moveNext();
        }

        return expressionStatement;
    }

    /* 这个方法处理Identifier, 要注册到prefixParseFunctionMap中 */
    private Identifier parseIdentifierExpression(){
        Identifier identifier = new Identifier(current, current.literal);
        return identifier;
    }

    /* 处理数字字面量，注册到prefixParseFunctionMap */
    /* 原书用的是int64, 那这里也就使用long */
    private IntegerLiteral parseIntegerLiteral(){
        IntegerLiteral integerLiteral = new IntegerLiteral(current);
        long value = Long.parseLong(current.literal);
        return integerLiteral.putValue(value);
    }

    /* 处理前缀表达式, 注册到prefixParseFunctionMap */
    private PrefixExpression parsePrefixExpression(){
        /* 假设这个表达式是-5, 第一个符号是MINUS, 第二个是expression */
        PrefixExpression prefixExpression = new PrefixExpression(current);

        /* 但如果下一个字符不是expression呢? 比如 - + 5, 这里应该断言再移动,
         * 不对不对, 既然后边也是个expression, 那应该交给parseExpression 处理
         * 是不是漏了指针后移, 现在还是operator
         *
         * ↑能在看书前想到, 是个值得欣慰的结果
         */
        prefixExpression.putOperator(current.literal);
        moveNext();

        /* 易错点: 这是前缀右操作数, 必须PREFIX */
        Expression expression = parseExpression(Precedence.PREFIX);
        prefixExpression.putExpression(expression);

        return prefixExpression;
    }

    /* 漏了错误处理, 应该考虑查表查不到的情况 */
    private void noPrefixParseFnError(){
        String noPrefixFunctionError = String.format(
                "Token is %s, no suitable prefix function in function map", current.literal);
        errors.add(noPrefixFunctionError);
    }

    private InfixExpression parseInfixExpression(Expression left){
        /* 思考逻辑: 能进到这个分支，说明之前已经读入了一个expression了，当前的current应该是operator
         * operator的后边还是要按照expression解析
         * 照例应该new一个InfixExpression */
        InfixExpression infixExpression = new InfixExpression(current);

        /* 欸等下, 那left得从外部传过来, 因此要修改函数形参 */
        infixExpression.putLeftExpression(left);
        /* 易错点: 手工设置operator */
        infixExpression.putOperator(current.literal);
        /* 易错点2: 手工记录当前operator的优先级 */
        Precedence operatorPrecedence = PrecedenceMap.get(current.type);

        /* 移动指针 */
        moveNext();

        /* 右侧应该又要按照expression来解析, 别忘了这里要使用当前operator的优先级 */
        Expression right = parseExpression(operatorPrecedence);
        if (right != null) {
            infixExpression.putRightExpression(right);
        }

        return infixExpression;
    }

    /* 解析布尔表达式 */
    /* 和原书的实现类似但有不同, 利用java enum的特性, 直接将BooleanLiteral做成枚举, 同时实现Expression接口 */
    private BooleanLiteral parseBooleanLiteral(){
        return Boolean.parseBoolean(current.literal) ? BooleanLiteral.TRUE : BooleanLiteral.FALSE;
    }

    /* 解析分组表达式, 就是左右括号包裹着expression */
    private Expression parseGroupExpression(){
        /* 左括号, 移动到下一位 */
        moveNext();
        /* 解析表达式 */
        Expression expression = parseExpression(Precedence.LOWEST);
        /* 这里应该是右括号了, 等等等, 这里还得expectPeek探测一下, 不然又重蹈覆辙了 */
        if(!expectPeek(TokenType.RPAREN)){
            return null;
        }
        return expression;
    }

    /* 解析条件表达式: if (condition) { ... } else { ... } */
    /* 思想转变记录（关于"如何校验一个 token 并前进"）：
     * ① 最初写的是 moveNext(); if (current != X) return null; —— 先移动、后断言，最贴近直觉、好追踪指针。
     * ② 后来意识到：先移动后断言，校验失败时指针已经动过了；改成"先断言 next、再 moveNext"，
     *    失败时指针原地不动。
     * ③ 再后来发现：if (next != X) return null 是静默失败，丢了错误信息；补上 peekError(X)。
     * ④ 最后恍然大悟：if (next != X) { peekError(X); return null; } moveNext(); 正是 expectPeek 的展开。
     *    —— 所以下面这段"手写展开"其实等价于直接调 expectPeek。 */
    private IfExpression parseIfExpression(){
        /* current=IF, 先 new 一个 IfExpression */
        IfExpression ifExpression = new IfExpression(current);

        /* 校验并前进：IF → 左括号 → 条件表达式开头 */
        if (next.type != TokenType.LPAREN){
            peekError(TokenType.LPAREN);
            return null;
        }
        moveNext();
        moveNext();

        ifExpression.putCondition(parseExpression(Precedence.LOWEST));

        /* 校验并前进到右括号 */
        if (next.type != TokenType.RPAREN){
            peekError(TokenType.RPAREN);
            return null;
        }
        moveNext();

        /* 校验并前进到左大括号 */
        if (next.type != TokenType.LBRACE){
            peekError(TokenType.LBRACE);
            return null;
        }
        moveNext();

        ifExpression.putConsequence(parseBlockStatement());

        /* 【连锁调整】修复 parseBlockStatement 后它停在右大括号（current==RBRACE），
         * 所以这里直接 moveNext() 越过右大括号即可。 */
        moveNext();

        /* 此时要么是 ELSE，要么该结束了 */
        if (current.type == TokenType.ELSE){
            moveNext();  // current=else 的左大括号

            ifExpression.putAlternative(parseBlockStatement());

            /* 修复后 parseBlockStatement 已停在右大括号，直接越过 */
            moveNext();
        }

        return ifExpression;
    }

    private BlockStatement parseBlockStatement(){
        BlockStatement blockStatement = new BlockStatement(current);
        /* 跳过大括号 */
        moveNext();

        List<Statement> statements = new ArrayList<>();
        /* 【bug 记录·位置】原来的循环条件是 current.type != TokenType.SEMICOLON：
         *   - 块是用 {} 包围的、不是 ; 结尾，所以这里应该用 RBRACE。
         *   - 后果1（死循环）: if (x < y) { x } 这种块内最后一条语句没有分号时，
         *     current 永远到不了 SEMICOLON，死循环。
         *   - 后果2（截断）: if (x < y) { x; y; } 这种多条语句时，第一条语句解析完
         *     current 停在 ;，循环立即退出，后面的 y 被丢弃。
         *   - 当时没暴露，是因为测试只写了 if (2 > 1) { true; } else { false; }
         *     （单条语句 + 有分号），parseExpressionStatement 内部恰好把 current 推到 ;，
         *     让"错误的条件"碰巧退出了循环。
         * 修复：条件改成 RBRACE。 */
        while(current.type != TokenType.RBRACE && current.type != TokenType.EOF){
            Statement statement = parseStatement();

            if (statement != null){
                statements.add(statement);
            }
            /* 【bug 记录·缺少的语句】原来循环体里漏了这一行 moveNext()：
             *   - 缺少它，current 不前进，即使条件正确也会死循环。
             * 修复：每解析完一条语句前进一次。 */
            moveNext();
        }
        blockStatement.putStatements(statements);

        return blockStatement;
    }

}
