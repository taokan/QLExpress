package com.alibaba.qlexpress4.runtime.operator.logic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.qlexpress4.QLPrecedences;
import com.alibaba.qlexpress4.exception.ErrorReporter;
import com.alibaba.qlexpress4.runtime.Value;
import com.alibaba.qlexpress4.runtime.operator.base.BaseBinaryOperator;

/**
 * @author 冰够
 */
public class LogicAndOperator extends BaseBinaryOperator {
    private static final Map<String, LogicAndOperator> INSTANCE_CACHE = new ConcurrentHashMap<>(2);

    static {
        INSTANCE_CACHE.put("&&", new LogicAndOperator("&&"));
        INSTANCE_CACHE.put("and", new LogicAndOperator("and"));
    }

    private final String operator;

    private LogicAndOperator(String operator) {
        this.operator = operator;
    }

    public static LogicAndOperator getInstance(String operator) {
        return INSTANCE_CACHE.get(operator);
    }

    @Override
    public Object execute(Value left, Value right, ErrorReporter errorReporter) {
        Object leftValue = left.get();
        Object rightValue = right.get();
        // 抽取至类型转换工具类
        if (leftValue == null) {
            leftValue = false;
        }
        if (rightValue == null) {
            rightValue = false;
        }

        if (!(leftValue instanceof Boolean) || !(rightValue instanceof Boolean)) {
            throw buildInvalidOperandTypeException(left, right, errorReporter);
        }

        return (Boolean)leftValue && (Boolean)rightValue;
    }

    @Override
    public String getOperator() {
        return "&&";
    }

    @Override
    public int getPriority() {
        return QLPrecedences.AND;
    }
}
