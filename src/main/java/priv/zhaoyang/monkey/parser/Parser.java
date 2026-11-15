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
      4.10: 函数表达式
      4.11: while表达式, 拓展原书没有的拓展
      4.12: 调用表达式
      4.13: 定义数组
      4.14: 数组索引
      4.15: 定义map
      4.16: map索引
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
        registerPrefix(TokenType.FUNCTION, this::parseFunctionExpression);
        registerPrefix(TokenType.WHILE, this::parseWhileExpression);
        registerPrefix(TokenType.STRING, this::parseStringLiteral);
        registerPrefix(TokenType.LBRACKETS, this::parseArrayLiteral);
        registerPrefix(TokenType.LBRACE, this::parseMapLiteral);

        /* 中缀注册 */
        registerInfix(TokenType.PLUS, this::parseInfixExpression);
        registerInfix(TokenType.MINUS, this::parseInfixExpression);
        registerInfix(TokenType.SLASH, this::parseInfixExpression);
        registerInfix(TokenType.ASTERISK, this::parseInfixExpression);
        registerInfix(TokenType.EQ, this::parseInfixExpression);
        registerInfix(TokenType.NOT_EQ, this::parseInfixExpression);
        registerInfix(TokenType.LT, this::parseInfixExpression);
        registerInfix(TokenType.GT, this::parseInfixExpression);
        registerInfix(TokenType.LPAREN, this::parseCallExpression);
        registerInfix(TokenType.LBRACKETS, this::parseIndexExpression);

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

        /* 现在current指向的是 ASSIGN, 还得向后挪动一位 */
        moveNext();

        /* 在实现完parseExpression()后, 终于可以回过头来补全这里的代码了 */
        Expression expression = parseExpression(Precedence.LOWEST);
        letStatement.putExpression(expression);

        /* 条件消费分号：只有 next 确实是分号才前进，否则会吞掉下一条语句的开头
         * （parseExpressionStatement 曾因无条件 moveNext 在这里翻过车） */
        if (next.type == TokenType.SEMICOLON){
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

        /* 同样不管expression 的处理
        while (current.type != TokenType.EOF && current.type != TokenType.SEMICOLON){
            moveNext();
        }
        */
        /* 实现完parseExpression, 现在可以补全了 */
        Expression expression = parseExpression(Precedence.LOWEST);
        returnStatement.putReturnExpression(expression);

        /* 你也一样, 先判断下一位是SEMICOLON再消费 */
        if (next.type == TokenType.SEMICOLON){
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

        /* 【bug 修复·parseIfExpression 吞掉 } 后面的 token】
         * 触发用例（eval 阶段失败）：if (false) { return 10; } return 20; 30;
         *   期望 20，实际却得到 30。
         * 失败原因：parseBlockStatement 结束后 current 停在右大括号 }，旧实现无条件 moveNext()
         *   越过 }，把 } 后面的 token（return 关键字）吃进了 current；随后 parseProgram 又
         *   moveNext() 一次，return 被跳过，`return 20; 30;` 被错误解析成 `20; 30;`，
         *   最后一条是 30，于是 return 20 的短路没生效。
         * 修改方案：不要“消费” } 后面的 token，改成“窥探” next 是不是 else——
         *   只有真的遇到 else 才前进，否则让 current 停在 }，把前进交给 parseProgram。
         * 修改后为什么正确：current 停在 }（或 else 块的 }），parseProgram 的 moveNext()
         *   正好前进到下一条语句的开头（return），return 语句得以被正确解析、提前短路。
         *
         * 为什么有 bug 却可能侥幸不暴露：只有当被吞的 token 不影响最终结果时才看不出来：
         *   1. return 正好是最后一条：if (false) { return 10; } return 20; 被吞后剩 `20`，
         *      最后一条的值恰好还是 20，和 return 20 短路出的 20 一致；
         *   2. if 内部提前 return 短路：if (true) { return 10; } return 20; 无论后半句
         *      是否被吞，都先返回 10，被吞的 return 20 根本没执行到。
         *   要戳破它，得让“被吞的 token 影响结果”：在 return 后面再跟一条语句，如
         *   `... return 20; 30;`，短路出 20 vs 被吞成 `20; 30;` 出 30，结果才会不一致。 */
        if (next.type == TokenType.ELSE) {
            moveNext();  // current = else

            if (next.type != TokenType.LBRACE){
                peekError(TokenType.LBRACE);
                return null;
            }
            moveNext();  // current = else 的 {

            ifExpression.putAlternative(parseBlockStatement());
            /* 不要 moveNext，让 current 停在 else 的右大括号，交给 parseProgram 前进 */
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

    /* 解析函数表达式: fn(参数列表) { 函数体 } */
    /* 指针轨迹示例（以 fn(x,y,z){x+1;y+2;z+3;} 为例）：
     *
     * token 编号: T0=fn  T1=(  T2=x  T3=,  T4=y  T5=,  T6=z  T7=)  T8={  T9=x ...
     *
     * 构造后:                          current=T0(fn),  next=T1(()
     *
     * ① 校验左括号(next==LPAREN) 后 moveNext×2:
     *                                   current=T2(x),   next=T3(,)   ← 越过 "fn("，停在第一个参数 x
     *
     * ② 参数循环（只要 next 是逗号或右括号，就把 current 解析成一个参数）:
     *    第1轮: current=T2(x) 解析为参数 x; 之后 moveNext×2 → current=T4(y), next=T5(,)
     *    第2轮: current=T4(y) 解析为参数 y; 之后 moveNext×2 → current=T6(z), next=T7())
     *    第3轮: current=T6(z) 解析为参数 z; next==RPAREN → moveNext → current=T7()), next=T8({) 然后 break
     *    循环结束: parameters=[x,y,z],  current=T7()),  next=T8({)
     *
     * ③ 校验左大括号(next==LBRACE) 后 moveNext×1:
     *                                   current=T8({),   next=T9(x)   ← 停在 { 上，交给 parseBlockStatement
     *
     * ④ parseBlockStatement(): 它期望 current 指向 {（内部会自己 moveNext 跳过 {），
     *    负责解析 body 内部（指针分析见 parseBlockStatement 的注释），返回时 current=RBRACE。
     */
    private FunctionExpression parseFunctionExpression() {
        FunctionExpression functionExpression = new FunctionExpression(current);

        /* ① 校验左括号并越过 fn( */
        if(next.type != TokenType.LPAREN) {
            peekError(TokenType.LPAREN);
            return null;
        }
        /* 借助上方的例子来理解, current=FUNCTION, next=LPAREN */
        moveNext();
        /* current=LPAREN, next=第一个argument, 也即x */
        moveNext();
        /* current=x, next=COMMA */

        /* ② 解析参数列表 */
        List<Identifier> arguments = new ArrayList<>();
        while (next.type == TokenType.COMMA || next.type == TokenType.RPAREN){
            Identifier identifier = parseIdentifierExpression();
            arguments.add(identifier);

            /* 最后一个参数：next 是右括号，越过它并结束循环 */
            if (next.type == TokenType.RPAREN){
                moveNext();
                break;
            }
            /* 非最后一个参数：越过逗号、停在下一个参数上（moveNext×2） */
            /* current=identifier, next=COMMA */
            moveNext();
            /* current=COMMA, next指向了下一个argument */
            moveNext();
            /* 移动后, current指向了下一个argument */
        }
        functionExpression.putParameters(arguments);

        /* ③ 校验左大括号，停在 { 上（parseBlockStatement 期望 current 是 {） */
        if (next.type != TokenType.LBRACE) {
            peekError(TokenType.LBRACE);
            return null;
        }
        moveNext();

        /* ④ 解析函数体 */
        BlockStatement body = parseBlockStatement();
        functionExpression.putBody(body);

        return functionExpression;
    }

    /* 解析while循环表达式: while(condition){blockStatement}
     * 注: 原书上没有 */
    private WhileExpression parseWhileExpression(){
        WhileExpression whileExpression = new WhileExpression(current);

        if (next.type != TokenType.LPAREN){
            peekError(TokenType.LPAREN);
            return null;
        }
        moveNext();  // current=LPAREN, next=condition 开头
        moveNext();  // current=condition 开头

        Expression condition = parseExpression(Precedence.LOWEST);
        whileExpression.putCondition(condition);

        if (next.type != TokenType.RPAREN){
            peekError(TokenType.RPAREN);
            return null;
        }
        moveNext();  // current=RPAREN, next=LBRACE

        /* 校验并前进到左大括号：调用 parseBlockStatement 前 current 必须停在 { 上 */
        if (next.type != TokenType.LBRACE){
            peekError(TokenType.LBRACE);
            return null;
        }
        moveNext();  // current=LBRACE

        BlockStatement body = parseBlockStatement();
        whileExpression.putBody(body);

        return whileExpression;
    }

    /* 解析调用表达式 */
    /* 之前自然而然的就把调用当作了前缀, 这是错误的, 这就是思考逻辑:
     * 1.思维惯性, 解析函数字面量是前缀的, 解析表达式默认也挂在前缀处理函数上, 这个想法不能算错, 问题在于后边发现了矛盾而不自知
     * 2.之所以有这个思维惯性, 是因为想当然的把 expression1(expression2, expression3...)中的expression当作了一个关键字,
     *   但它实际上只是个Expression, 不在词典里, 一厢情愿的将函数名硬是当作关键字FUNCTION处理.
     * 3.更要命的还在后面, 又是左括号, 左括号之前已经注册了分组表达式了, 这里又是一厢情愿想把左括号再注册一次, 想着执行时某种天意
     *   可以正确的帮我区分这是分组表达式还是调用表达式。到这里其实已经爆出了明显的矛盾了, 只要再多想一步, 就可以得出它应该挂在
     *   中缀表达式的处理逻辑中
     * 4.受到上述影响, 自然不会在参数列表中添加参数, 这个中缀表达式需要传入LPAREN左侧的表达式
     * 嗯, 重新实现
     * 5. 另外, 这个相通了以后, 对于[和{也就可以类比了:
     * 对于'[', 挂在前缀则是定义列表, 如 dict = [1,2,3], 挂在中缀则是对列表或map索引, 如list[1]
     * 对于'{', 前缀定义map, 中缀悬空, 当然可以用来解析结构体, 不过结构体的解析是一个很难的事, 如a.b.c.d=1, 同时涉及到解析和求值,
     * 再比如 Person{name:zhangsan}和 Person{name:zhangsan}该如何判定相等, 总之就是太难了, 可能原作者顾虑到这些选择不实现
     */
    private CallExpression parseCallExpression(Expression function){
        CallExpression callExpression = new CallExpression(current);  // current=LPAREN
        callExpression.putFunction(function);

        /* 为什么"第一个参数要放到循环外解析"（而不是在循环里统一解析所有参数）：
         *
         * 错误写法是"循环里解析所有参数"：
         *   moveNext();                  // 到第一个参数
         *   while (条件) {
         *       parse current;           // 解析参数
         *       moveNext(); moveNext();  // 跳到下一个参数
         *   }
         * 致命点：moveNext×2 之后，next 指向"下一个参数内部"的 token，而不是"分隔符"。
         *
         * 反例1 add(1, 2 * 3, 4 + 5)：
         *   解析完 1 后：  current=1, next=,
         *   moveNext×2：   current=2, next=*     ← next 是 *（参数内部），既不是逗号也不是右括号
         *   于是无论条件写 next==COMMA 还是 next==COMMA||RPAREN，都在这里提前退出，2*3、4+5 被漏掉。
         *
         * 反例2 add(1, 2) 且条件写 next==COMMA：
         *   moveNext×2 后：current=2, next=)     ← 最后一个参数前面是右括号，不是逗号
         *   条件 next==COMMA 为假，2 被漏掉。
         *
         * 正解（原书结构）：先在循环外解析第一个参数，循环里只处理"逗号分隔的后续参数"。
         * 这样每次判断循环条件时，next 都正好停在"分隔符"（逗号或右括号）上。
         *
         * Q:那么, 如果强行在循环里统一解析, 会带来什么问题或风险呢?
         * A:这样一来, 循环条件就必须得是 next.type != TokenType.EOF 才能保证循环正常推进, 但这个条件太宽松了
         *   遇到 add(1 2)（缺逗号）这类脏输入时行为会不一样
         *
         * 吐槽: 这些都是原作者不讲的地方, 这种细节上的坑就是这样, 说起来既无必要, 还破坏行文节奏。
         *      但你要真想懂原因, 不得不再踩一遍坑 */
        List<Expression> arguments = new ArrayList<>();

        /* 无参: fn() */
        if (next.type == TokenType.RPAREN){
            moveNext();
            callExpression.putArguments(arguments);
            return callExpression;
        }

        /* 第一个参数（循环外） */
        moveNext();
        arguments.add(parseExpression(Precedence.LOWEST));

        /* 后续参数（循环里，只在 next 是逗号时进入） */
        while (next.type == TokenType.COMMA){
            moveNext();  // current=COMMA
            moveNext();  // current=下一个参数
            arguments.add(parseExpression(Precedence.LOWEST));
        }

        /* 校验并跳过右括号 */
        if (next.type != TokenType.RPAREN){
            peekError(TokenType.RPAREN);
            return null;
        }
        moveNext();

        callExpression.putArguments(arguments);
        return callExpression;
    }

    /* 解析字符串字面量 */
    private StringLiteral parseStringLiteral(){
        StringLiteral stringLiteral = new StringLiteral(current);
        return stringLiteral;
    }

    /* 解析数组字面量 [1,2,3] */
    /* 又是表达式列表, 注意第一个元素从循环外拿出来 */
    private ArrayLiteral parseArrayLiteral(){
        ArrayLiteral arrayLiteral = new ArrayLiteral(current);

        /* 空数组的情况 */
        if (next.type == TokenType.RBRACKETS){
            moveNext();
            return arrayLiteral.putExpressionList(Collections.emptyList());
        }
        /* 非空的情况 */
        moveNext();

        List<Expression> expressions = new ArrayList<>();
        Expression first = parseExpression(Precedence.LOWEST);
        expressions.add(first);

        while(next.type == TokenType.COMMA){
            moveNext();
            moveNext();
            Expression expression = parseExpression(Precedence.LOWEST);
            expressions.add(expression);
        }

        arrayLiteral.putExpressionList(expressions);

        if (next.type != TokenType.RBRACKETS){
            peekError(TokenType.RBRACKETS);
            return null;
        }
        moveNext();

        return arrayLiteral;
    }

    /* 解析索引表达式
     * 它是个中缀, 也就是扫描到‘[’时, 一定将target解析完了
     *
     */
    private IndexExpression parseIndexExpression(Expression target){
        IndexExpression indexExpression = new IndexExpression(current);
        indexExpression.putTarget(target);

        /* 这里是不是还要考虑a[]这种不写索引值的情况?
         * 没必要, 直接moveNext就行, 不管下一位是不是], 都想当作表达式来解析, 遇到错误, 也就是这种没有索引的情况
         * 直接就报错了 */
        moveNext();

        Expression index = parseExpression(Precedence.LOWEST);
        indexExpression.putIndex(index);

        if (!expectPeek(TokenType.RBRACKETS)){
            return null;
        }

        return indexExpression;
    }

    /* 解析map的定义, 遇到大括号, 进入前缀表达式
     *  let dict = {"name": "Rean", "age": 22, "isMale", true};
     *  */
    private MapLiteral parseMapLiteral(){
        MapLiteral mapLiteral = new MapLiteral(current);

        /* 空 map 的情况（先于 moveNext，看 next 是不是 }） */
        if (next.type == TokenType.RBRACE){
            moveNext();
            return mapLiteral.putMap(Collections.emptyMap());
        }

        /* 跳过大括号 */
        moveNext();

        Map<Expression, Expression> map = new HashMap<>();

        /* 第一个键值对单独处理 */
        Expression firstKey = parseExpression(Precedence.LOWEST);
        if(!expectPeek(TokenType.COLON)) {
            return null;
        }
        moveNext();
        Expression firstValue = parseExpression(Precedence.LOWEST);
        map.put(firstKey, firstValue);

        /* 后续统一放循环 */
        while (next.type == TokenType.COMMA) {
            moveNext();
            moveNext();

            Expression key = parseExpression(Precedence.LOWEST);
            if(!expectPeek(TokenType.COLON)) {
                return null;
            }
            moveNext();
            Expression value = parseExpression(Precedence.LOWEST);
            map.put(key, value);
        }

        mapLiteral.putMap(map);

        if (!expectPeek(TokenType.RBRACE)){
            return null;
        }

        return mapLiteral;
    }
}
