package priv.zhaoyang.monkey.repl;

import priv.zhaoyang.monkey.ast.Program;
import priv.zhaoyang.monkey.eval.Environment;
import priv.zhaoyang.monkey.eval.Evaluator;
import priv.zhaoyang.monkey.lexer.Lexer;
import priv.zhaoyang.monkey.object.MonkeyObject;
import priv.zhaoyang.monkey.object.NullObject;
import priv.zhaoyang.monkey.parser.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

public class Repl {

    private final static String PROMPT = ">> ";

    public static void main(String[] args) throws IOException {
        start(
                new BufferedReader(new InputStreamReader(System.in)),
                new OutputStreamWriter(System.out)
        );
    }

    private static void start(BufferedReader in, Writer out) throws IOException {

        /* 关键：环境只创建一次、跨行共享。否则 let x = 5; 下一行 x 就找不到了。 */
        Environment environment = new Environment(null);

        while (true) {

            out.write(PROMPT);
            out.flush();

            String line = in.readLine();
            if (line == null) {
                out.write('\n');
                out.flush();
                return;
            }

            if (line.trim().equalsIgnoreCase("quit") ||
                    line.trim().equalsIgnoreCase("exit")) {
                out.write("Bye!");
                out.flush();
                System.exit(0);
            }

            Lexer lexer = new Lexer(line);
            Parser parser = new Parser(lexer);
            Program program = parser.parseProgram();

            if (!parser.errors().isEmpty()) {
                printParseErrors(out, parser.errors());
                continue;
            }

            MonkeyObject evaluated = Evaluator.eval(program, environment);
            /* 结果是 Monkey 的 null 就不打印（原书行为），其它都打印 inspect() */
            if (evaluated != NullObject.NULL) {
                out.write(evaluated.inspect());
                out.write('\n');
            }
            out.flush();
        }
    }

    private static void printParseErrors(Writer out, List<String> errors) throws IOException {
        for (String error : errors) {
            out.write(error);
            out.write('\n');
        }
    }
}
