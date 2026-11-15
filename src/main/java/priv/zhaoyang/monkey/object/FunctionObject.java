package priv.zhaoyang.monkey.object;

import priv.zhaoyang.monkey.ast.FunctionExpression;
import priv.zhaoyang.monkey.eval.Environment;

import java.util.Objects;

public class FunctionObject implements MonkeyObject{

    private final FunctionExpression functionExpression;
    private final Environment environment;

    public FunctionObject(FunctionExpression functionExpression, Environment environment){
        this.functionExpression = functionExpression;
        this.environment = environment;
    }

    public FunctionExpression functionExpression(){
        return this.functionExpression;
    }
    public Environment environment(){
        return this.environment;
    }


    @Override
    public ObjectType type() {
        return ObjectType.FUNCTION;
    }

    @Override
    public String inspect() {
        return functionExpression.toString();
    }

    @Override
    public String toString(){
        return functionExpression.toString();
    }
}
