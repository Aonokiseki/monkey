package priv.zhaoyang.monkey.object;

import java.io.IOException;
import java.util.List;

public interface BuiltinFunction extends MonkeyObject{
    public MonkeyObject process(List<MonkeyObject> objects);
}
