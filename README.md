# Monkey —— 用 Java 实现的 Monkey 语言解释器

一个用纯 Java 编写的 [Monkey](https://monkeylang.org) 编程语言解释器，完整实现了词法分析、语法分析、抽象语法树与树遍历求值器，并带有一个交互式 REPL。

本项目是 [Thorsten Ball《Writing An Interpreter In Go》](https://interpreterbook.com/) 的 Java 移植版本，同时在原书基础上做了一些扩展。

## ✨ 特性

- **词法分析器（Lexer）**：将源代码切分为 Token，支持标识符、整数、字符串、运算符、分隔符与关键字。
- **语法分析器（Parser）**：基于 Pratt（自顶向下运算符优先级）解析技术，支持完整的运算符优先级。
- **抽象语法树（AST）**：结构化的 Node / Expression / Statement 体系。
- **树遍历求值器（Evaluator）**：支持词法作用域、闭包、一等函数。
- **丰富的数据类型**：整数（64 位）、布尔值、字符串、数组、哈希表（Map）、函数、内置函数。
- **语言特性**：let 变量绑定、if/else 条件表达式、while 循环、return、函数与闭包、字符串拼接、索引表达式（数组 / Map / 字符串）。
- **内置函数**：len、first、last、rest、push、pop、puts。
- **交互式 REPL**：逐行输入、即时求值，环境跨行共享。
- **完整测试**：基于 TestNG 的词法、语法、求值器单元测试。

## 📁 项目结构

```text
src/main/java/priv/zhaoyang/monkey/
├── token/      # Token 与 TokenType、关键字定义
├── lexer/      # 词法分析器
├── ast/        # 抽象语法树节点定义
├── parser/     # Pratt 语法分析器（含运算符优先级）
├── object/     # 运行时对象系统（整数/布尔/字符串/数组/Map/函数/内置函数等）
├── eval/       # 求值器与环境（作用域）
└── repl/       # 交互式解释器入口（main 方法）
src/test/java/priv/zhaoyang/test/   # TestNG 测试
```

## 🚀 快速开始

### 环境要求

- JDK 12 及以上
- Maven 3.x

### 构建

```bash
mvn clean package
```

### 运行 REPL

```bash
mvn compile
java -cp target/classes priv.zhaoyang.monkey.repl.Repl
```

进入 REPL 后可逐行输入 Monkey 代码，输入 `quit` 或 `exit` 退出：

```text
>> let x = 5;
5
>> let y = 10;
10
>> x + y;
15
>> if (x > y) { "big" } else { "small" };
small
>> let arr = [1, 2, 3];
[1, 2, 3]
>> arr[1];
2
>> len(arr);
3
```

### 运行测试

```bash
mvn test
```

## 🧪 语言特性示例

### 变量与算术

```text
let a = 5;
let b = a * 2 + 10;
-b;              // 取负
!true;           // 逻辑取反
```

### 条件表达式

```text
if (10 > 1) { "yes" } else { "no" };
```

### 函数与闭包

```text
let add = fn(a, b) { a + b; };
add(2, 3);       // 5

// 闭包：内层函数捕获外层作用域变量
let newAdder = fn(x) { fn(y) { x + y; }; };
let addTwo = newAdder(2);
addTwo(2);       // 4
```

### while 循环（本项目的扩展）

```text
let i = 0;
while (i < 3) { let i = i + 1; };
```

### 数组与哈希表

```text
let arr = [1, 2, 3];
let map = {"one": 1, "two": 2};

arr[0];          // 1
map["one"];      // 1
"hello"[1];      // "e"（字符串索引）
```

## 🔧 内置函数

| 函数    | 说明                                        |
| ------- | ------------------------------------------- |
| `len`   | 字符串 / 数组 / Map 的长度                  |
| `first` | 返回数组第一个元素，空数组返回 null          |
| `last`  | 返回数组最后一个元素，空数组返回 null        |
| `rest`  | 返回去掉第一个元素后的新数组                |
| `push`  | 返回在数组末尾追加元素后的新数组（不修改原数组） |
| `pop`   | 移除并返回数组最后一个元素（本项目扩展）    |
| `puts`  | 打印传入的每个值                            |

## 📝 与原书《Writing An Interpreter In Go》的差异

在原书实现的基础上，本项目做了如下增强：

1. **新增 while 循环**：原书没有循环结构，这里补充了 while 表达式。
2. **新增内置函数 pop**：原书内置函数为 len/first/last/rest/push/puts，此处额外提供 pop。
3. **len 支持 Map**：原书 len 仅支持字符串与数组，这里额外支持 Map 的键值对数量。
4. **新增字符串索引取值**：支持 `"hello"[1]` 形式的按索引取字符。
5. **整数类型采用 64 位 long**：与 Go 原版的 int64 语义一致，支持到 Long.MAX_VALUE。

此外，代码中保留了大量实现过程中的思考与踩坑记录（以中文注释形式），例如前缀/中缀表达式按「对象类型 + 运算符」分派而非按 AST 节点类型分派、闭包中「实参在调用方环境求值、局部变量表 outer 指向定义时环境」等，便于对照学习。

## 📚 参考

- [Thorsten Ball — Writing An Interpreter In Go](https://interpreterbook.com/)
- [Monkey Programming Language](https://monkeylang.org/)
