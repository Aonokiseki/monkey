package priv.zhaoyang.monkey.parser;

import priv.zhaoyang.monkey.ast.Expression;

/* 在golang中, 函数是一等公民, 可以直接定义map[string]fn() prefixParseFn, 根据token返回待处理的函数
 * 在java中就比较难受了, 既是只有一个方法, 即使有lambda表达式, 也得定义一个接口
 *  */
public interface PrefixParseFunction {

    Expression process();
}
