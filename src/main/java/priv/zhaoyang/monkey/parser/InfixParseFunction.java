package priv.zhaoyang.monkey.parser;

import priv.zhaoyang.monkey.ast.Expression;

public interface InfixParseFunction {

    Expression process(Expression left);
}
