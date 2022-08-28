package com.alibaba.qlexpress4.runtime.data.checker.paramchecker;

import com.alibaba.qlexpress4.runtime.data.checker.MatchChecker;
import com.alibaba.qlexpress4.runtime.data.convert.ParametersConversion;
import com.alibaba.qlexpress4.utils.BasicUtil;

/**
 * @Author TaoKan
 * @Date 2022/7/20 下午10:38
 */
public class QLPrimitiveParametersChecker implements MatchChecker {


    @Override
    public boolean typeMatch(Class<?> source, Class<?> target) {
        Class<?> sourcePrimitive = BasicUtil.isPrimitive(source) ? source : BasicUtil.transToPrimitive(source);
        Class<?> targetPrimitive = BasicUtil.isPrimitive(target) ? target : BasicUtil.transToPrimitive(target);
        return sourcePrimitive != null && targetPrimitive != null && BasicUtil.classMatchImplicit(targetPrimitive, sourcePrimitive);
    }

    @Override
    public ParametersConversion.QLMatchConverter typeReturn(Class<?> source, Class<?> target) {
        return ParametersConversion.QLMatchConverter.IMPLICIT;
    }
}
