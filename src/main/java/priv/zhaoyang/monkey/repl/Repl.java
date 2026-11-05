package priv.zhaoyang.monkey.repl;

import priv.zhaoyang.monkey.ast.Program;
import priv.zhaoyang.monkey.lexer.Lexer;
import priv.zhaoyang.monkey.parser.Parser;
import priv.zhaoyang.monkey.token.Token;
import priv.zhaoyang.monkey.token.TokenType;

import java.io.*;
import java.util.List;

public class Repl {

    private final static String PROMPT = ">>";

    public static void main(String[] args) throws IOException {
        start(
                new BufferedReader(new InputStreamReader(System.in)),
                new OutputStreamWriter(System.out)
        );
    }

    private static void start(BufferedReader in, Writer out) throws IOException {

        while(true){

            out.write(PROMPT);
            out.flush();

            String line = in.readLine();
            if (line == null){
                out.write('\n');
                out.flush();
                return;
            }

            Lexer lexer = new Lexer(line);
            Parser parser = new Parser(lexer);
            Program program = parser.parseProgram();
            if (!parser.errors().isEmpty()){
                printParseErrors(out, parser.errors());
                continue;
            }

            for(Token t = lexer.nextToken(); t.type != TokenType.EOF; t = lexer.nextToken()){
                out.write(t.toString());
                out.write('\n');
            }
            out.flush();

        }
    }

    private static void printParseErrors(Writer out, List<String> errors) throws IOException {
        for (String error :errors){
            out.write(error);
        }
    }
}
