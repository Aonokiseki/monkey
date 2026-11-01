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
        /* 这里负责注册 */

        /* 注册标识符处理方法 */
        this.prefixParseFunctionMap.put(TokenType.IDENT, () -> {return parseIdentifierExpression();});
        /* 注册数字字面量方法 */
        this.prefixParseFunctionMap.put(TokenType.INT, () -> {return parseIntegerLiteral();});

        this.infixParseFunctionMap = new HashMap<>();
        /* 这里负责注册 */

        /* 连续执行2次moveNext, 让current和next都有指向的对象 */
        moveNext();
        moveNext();
    }

    private void moveNext(){
        current = next;
        next = lexer.nextToken();
    }

    public Program parseProgram(){
        Program program = new Program();

        List<Statement> statements = new ArrayList<>();
        while(current.type != TokenType.EOF){

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
    private Expression parseExpression(){

        /* current肯定是expression的一部分, 所以应该先new一个Expression
         * 但是等等, Expression是一个interface，不能new, 要先细致到是哪个具体类的实例才能new,
         * 这就必须要查表, 也就是prefixParseFunctionMap */
        PrefixParseFunction prefixParseFunction = prefixParseFunctionMap.get(current.type);

        if (prefixParseFunction == null) {
            return null;
        }
        Expression expression = prefixParseFunction.process();

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
        expressionStatement.putExpression(parseExpression());

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
}
