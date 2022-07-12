package com.alibaba.qlexpress4.runtime;

import java.util.Map;

/**
 * Author: DQinYuan
 */
public interface QRuntime {

    /**
     * get assignable symbol variable by name
     * @param varName variable name
     * @return value, null if not exist
     */
    Value getSymbol(String varName);

    /**
     * get symbol variable value by name
     * @param varName variable name
     * @return inner value, null if not exist
     */
    Object getSymbolValue(String varName);

    /**
     * define a symbol in global scope
     * for example, `Number a = 10`
     *              define("a", Number.class, new Value(10))
     * @param varName symbol name
     * @param varClz symbol clz, declare clz, not real clz
     */
    LeftValue defineSymbol(String varName, Class<?> varClz);

    /**
     * define a symbol in local scope
     * @param varName
     * @param varClz
     * @param value init value
     */
    void defineLocalSymbol(String varName, Class<?> varClz, Object value);

    /**
     * define local function in scope
     * @param functionName
     * @param function
     */
    void defineFunction(String functionName, QFunction function);

    /**
     * get function or lambda define
     * @param functionName
     * @return null if not exist
     */
    QFunction getFunction(String functionName);

    /**
     * push value on the top of stack
     * @param value pushed element
     */
    void push(Value value);

    /**
     * pop number elements on top of stack
     * @param number pop elements' number
     * @return popped elements
     */
    Parameters pop(int number);

    /**
     * pop one element on top of stack
     * @return popped element
     */
    Value pop();

    /**
     * get script start time
     * @return start time
     */
    long scriptStartTimeStamp();

    /**
     * populate define global symbol
     * @return
     */
    boolean isPopulate();

    Map<String, Object> attachment();
}
