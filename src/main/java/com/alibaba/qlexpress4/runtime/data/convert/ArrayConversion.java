package com.alibaba.qlexpress4.runtime.data.convert;

import com.alibaba.qlexpress4.runtime.data.checker.*;
import com.alibaba.qlexpress4.runtime.data.checker.convertchecker.array.QLArrayAssignableConvertChecker;
import com.alibaba.qlexpress4.runtime.data.checker.convertchecker.array.QLArrayDoubleStreamConvertChecker;
import com.alibaba.qlexpress4.runtime.data.checker.convertchecker.array.QLArrayIntStreamConvertChecker;
import com.alibaba.qlexpress4.runtime.data.checker.convertchecker.array.QLArrayLongStreamConvertChecker;
import com.alibaba.qlexpress4.runtime.data.implicit.QLConvertResult;
import com.alibaba.qlexpress4.runtime.data.implicit.QLConvertResultType;
import com.alibaba.qlexpress4.utils.BasicUtil;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.*;

/**
 * @Author TaoKan
 * @Date 2022/6/26 下午3:35
 */
public class ArrayConversion {
    private static final TypeConvertChecker[] typeConvertArrayChecker;

    static {
        typeConvertArrayChecker = new TypeConvertChecker[]{
                new QLArrayAssignableConvertChecker(),
                new QLArrayIntStreamConvertChecker(),
                new QLArrayLongStreamConvertChecker(),
                new QLArrayDoubleStreamConvertChecker()};
    }

    public static QLConvertResult trans(final Object object, final Class type) {
        for(TypeConvertChecker typeConvertChecker: typeConvertArrayChecker){
            if(typeConvertChecker.typeCheck(object,type)){
                QLConvertResult result = (QLConvertResult)typeConvertChecker.typeReturn(object,type);
                if(result != null){
                    return result;
                }
            }
        }
        Class<?> elementType = type.getComponentType();
        Collection<?> collection = toCollection(object);
        Object array = Array.newInstance(elementType, collection.size());

        int i = 0;
        for (Object element : collection) {
            QLConvertResult qlConvertResult = InstanceConversion.castObject(element, elementType);
            //if one element cannot trans
            if(qlConvertResult.getResultType().equals(QLConvertResultType.NOT_TRANS)){
                return qlConvertResult;
            }
            Array.set(array, i++, qlConvertResult.getCastValue());
        }

        return new QLConvertResult(QLConvertResultType.CAN_TRANS, array);
    }

    public static Collection toCollection(final Object value) {
        if (value == null) {
            return Collections.EMPTY_LIST;
        } else if (value instanceof Collection) {
            return (Collection) value;
        } else if (value instanceof Map) {
            return ((Map) value).entrySet();
        } else if (value.getClass().isArray()) {
            return arrayToCollection(value);
        } else if (value instanceof BaseStream) {
            return toList((BaseStream) value);
        } else if (value instanceof String) {
            return toList((CharSequence) value);
        } else {
            return Collections.singletonList(value);
        }
    }

    public static <T> List<T> toList(final Stream<T> stream) {
        return stream.collect(Collectors.toList());
    }

    public static <T> List<T> toList(final BaseStream<T, ? extends BaseStream> baseStream) {
        return SteamConversion.stream(baseStream.iterator()).collect(Collectors.toList());
    }


    public static List<String> toList(final CharSequence charSequence) {
        String s = charSequence.toString();
        final int n = s.length();
        List<String> answer = new ArrayList<>(n);
        for (int i = 0; i < n; i += 1) {
            answer.add(s.substring(i, i + 1));
        }
        return answer;
    }

    public static Collection arrayToCollection(Object value) {
        if (BasicUtil.isPrimitive(value.getClass().getComponentType())) {
            return primitiveArrayToList(value);
        }
        return arrayToCollection((Object[]) value);
    }

    public static List primitiveArrayToList(Object array) {
        int size = Array.getLength(array);
        List list = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            Object item = Array.get(array, i);
            if (item != null && item.getClass().isArray() && BasicUtil.isPrimitive(item.getClass().getComponentType())) {
                item = primitiveArrayToList(item);
            }
            list.add(item);
        }
        return list;
    }

    public static <T> Collection<T> arrayToCollection(T[] value) {
        return Arrays.asList(value);
    }

}
