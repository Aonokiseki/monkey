package priv.zhaoyang.monkey.object;

import java.io.*;
import java.util.*;

public class Builtin{

    private Builtin(){}

    private final static Map<String, BuiltinFunction> DICTIONARY = new HashMap<>();

    static {
        DICTIONARY.put("len", new BuiltinFunction(){

            @Override
            public ObjectType type() {
                return ObjectType.BUILTIN;
            }

            @Override
            public String inspect() {
                return "len";
            }

            @Override
            public MonkeyObject process(List<MonkeyObject> objects) {
                if (objects.size() != 1){
                    throw new IllegalArgumentException(String.format("Invalid arguments count(%d)", objects.size()));
                }
                MonkeyObject monkeyObject = objects.get(0);

                if (monkeyObject instanceof StringObject) {
                    /* 统计的是字符数, 而不是字节数, 这一点比原书实现的更接近语义 */
                    int stringLength = ((StringObject) monkeyObject).value().length();
                    return new IntegerObject(stringLength);
                }
                if (monkeyObject instanceof ArrayObject) {
                    int arraySize = ((ArrayObject) monkeyObject).values().size();
                    return new IntegerObject(arraySize);
                }
                /* 不知不觉比原作者还多支持一种 */
                if (monkeyObject instanceof MapObject) {
                    int mapSize = ((MapObject) monkeyObject).map().size();
                    return new IntegerObject(mapSize);
                }
                throw new IllegalStateException(String.format(
                        "Invalid monkey object[type=%s] in `len`", monkeyObject.getClass().getName()));
            }
        });

        DICTIONARY.put("first", new BuiltinFunction() {
            @Override
            public MonkeyObject process(List<MonkeyObject> objects) {
                if (objects.size() != 1){
                    throw new IllegalArgumentException(String.format("Invalid arguments count(%d)", objects.size()));
                }
                MonkeyObject monkeyObject = objects.get(0);
                if (monkeyObject instanceof ArrayObject){
                    List<MonkeyObject> values = ((ArrayObject)monkeyObject).values();
                    if (values.isEmpty()){
                        /* 要求返回NullObject.NULL
                         * throw new IndexOutOfBoundsException("Call first(), but array length is 0.");
                         */
                        return NullObject.NULL;
                    }
                    return values.get(0);
                }
                throw new IllegalStateException(String.format(
                        "Invalid monkey object[type=%s] in `first`", monkeyObject.getClass().getName()));
            }

            @Override
            public ObjectType type() {
                return ObjectType.BUILTIN;
            }

            @Override
            public String inspect() {
                return "first";
            }
        });

        DICTIONARY.put("last", new BuiltinFunction() {
            @Override
            public MonkeyObject process(List<MonkeyObject> objects) {
                if (objects.size() != 1){
                    throw new IllegalArgumentException(String.format("Invalid arguments count(%d)", objects.size()));
                }
                MonkeyObject monkeyObject = objects.get(0);
                if (monkeyObject instanceof ArrayObject){
                    List<MonkeyObject> values = ((ArrayObject)monkeyObject).values();
                    if (values.isEmpty()){
                        /* 要求返回NullObject.NULL
                         * throw new IndexOutOfBoundsException("Call last(), but array length is 0.");
                         */
                        return NullObject.NULL;
                    }
                    return values.get(values.size() - 1);
                }
                throw new IllegalStateException(String.format(
                        "Invalid monkey object[type=%s] in `last`", monkeyObject.getClass().getName()));
            }

            @Override
            public ObjectType type() {
                return ObjectType.BUILTIN;
            }

            @Override
            public String inspect() {
                return "last";
            }
        });

        DICTIONARY.put("push", new BuiltinFunction() {
            @Override
            public MonkeyObject process(List<MonkeyObject> objects) {
                if (objects.size() != 2){
                    throw new IllegalArgumentException(String.format("Invalid arguments count(%d)", objects.size()));
                }
                MonkeyObject target = objects.get(0);
                if (target instanceof ArrayObject) {
                    List<MonkeyObject> values = ((ArrayObject) target).values();

                    /* 浅拷贝 */
                    List<MonkeyObject> copy = new ArrayList<>(values.size());
                    for(MonkeyObject value : values){
                        copy.add(value);
                    }
                    copy.add(objects.get(1));
                    /* 要求返回的是改变后的数组, 不是添加是否成功
                     * boolean result = values.add(objects.get(1));
                     * return result ? BooleanObject.TRUE : BooleanObject.FALSE;
                     */
                    return new ArrayObject(copy);
                }
                throw new IllegalStateException(String.format(
                        "Invalid monkey object[type=%s] in `push`", target.getClass().getName()));
            }

            @Override
            public ObjectType type() {
                return ObjectType.BUILTIN;
            }

            @Override
            public String inspect() {
                return "push";
            }
        });

        DICTIONARY.put("pop", new BuiltinFunction() {
            @Override
            public MonkeyObject process(List<MonkeyObject> objects) {
                if (objects.size() != 1){
                    throw new IllegalArgumentException(String.format("Invalid arguments count(%d)", objects.size()));
                }
                MonkeyObject target = objects.get(0);
                if (target instanceof ArrayObject) {
                    List<MonkeyObject> values = ((ArrayObject) target).values();
                    /* 自带异常处理, 应该不需要我 */
                    return values.remove(values.size() - 1);
                }
                throw new IllegalStateException(String.format(
                        "Invalid monkey object[type=%s] in `pop`", target.getClass().getName()));
            }

            @Override
            public ObjectType type() {
                return ObjectType.BUILTIN;
            }

            @Override
            public String inspect() {
                return "pop";
            }
        });

        DICTIONARY.put("rest", new BuiltinFunction() {
            @Override
            public MonkeyObject process(List<MonkeyObject> objects) {
                if (objects.size() != 1){
                    throw new IllegalArgumentException(String.format("Invalid arguments count(%d)", objects.size()));
                }
                MonkeyObject monkeyObject = objects.get(0);
                if (monkeyObject instanceof ArrayObject){
                    List<MonkeyObject> values = ((ArrayObject)monkeyObject).values();
                    if (values.isEmpty()){
                        return NullObject.NULL;
                    }
                    /* 浅拷贝 */
                    List<MonkeyObject> copy = new ArrayList<>();
                    for(int i = 1; i < values.size(); i++){
                        copy.add(values.get(i));
                    }
                    return new ArrayObject(copy);
                }
                throw new IllegalStateException(String.format(
                        "Invalid monkey object[type=%s] in `rest`", monkeyObject.getClass().getName()));
            }

            @Override
            public ObjectType type() {
                return ObjectType.BUILTIN;
            }

            @Override
            public String inspect() {
                return "rest";
            }
        });

        DICTIONARY.put("puts", new BuiltinFunction() {
            @Override
            public MonkeyObject process(List<MonkeyObject> objects) {
                OutputStreamWriter writer = new OutputStreamWriter(System.out);
                for(MonkeyObject monkeyObject : objects){
                    try{
                        writer.write(monkeyObject.inspect());
                        writer.write("\n");
                        writer.flush();
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
                return NullObject.NULL;
            }

            @Override
            public ObjectType type() {
                return ObjectType.BUILTIN;
            }

            @Override
            public String inspect() {
                return "puts";
            }
        });
    }

    public static boolean contains(String name){
        return DICTIONARY.containsKey(name);
    }

    public static BuiltinFunction get(String name){
        return DICTIONARY.get(name);
    }

}
