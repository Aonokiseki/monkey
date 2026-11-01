package priv.zhaoyang.monkey.parser;

/* 目标1: 解析let语句
   let x = 10;
   let y = 15;

   let add = fn(x, y){
       x + y;
   };

   要能识别出三个语句, 三个标识符x, y, add

   目标2: 添加最基础的错误处理机制
 * */


import priv.zhaoyang.monkey.ast.Identifier;
import priv.zhaoyang.monkey.ast.LetStatement;
import priv.zhaoyang.monkey.ast.Program;
import priv.zhaoyang.monkey.ast.Statement;
import priv.zhaoyang.monkey.lexer.Lexer;
import priv.zhaoyang.monkey.token.Token;
import priv.zhaoyang.monkey.token.TokenType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Parser {

    private Lexer lexer;
    /* 和Lexer不一样, 因为是一边词法分析一边解析token, 不能用下标来指向当前和下一个token */
    private Token current;
    private Token next;
    private List<String> errors;

    public Parser(Lexer lexer){
        this.lexer = lexer;
        this.errors = new ArrayList<>();

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

    public Statement parseStatement(){

        if(current.type.equals(TokenType.LET)){
            return parseLetStatement();
        }

        /* 中间还有其它语句， 先不管 */

        else {
            return null;
        }
    }

    public LetStatement parseLetStatement(){
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
         */

        /* 遇到Token.ASSIGN, 挪动指针 */
        /* 本函数的两个重要校验2:校验是不是赋值符 */
        if (!expectPeek(TokenType.ASSIGN)){
            return null;
        }

        /* 后边直至;，都是一个expression，至于expression怎么解析，先不管，假设有个上帝函数帮我们解析完成了 */
        /* 原书中这里是 current 指向的token来判断的, 先打个标 FLAG, 以后遇到问题搜索关键字
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
}
