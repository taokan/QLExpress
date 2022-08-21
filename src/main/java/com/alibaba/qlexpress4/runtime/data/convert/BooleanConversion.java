package com.alibaba.qlexpress4.runtime.data.convert;

import com.alibaba.qlexpress4.runtime.data.implicit.QLConvertResult;
import com.alibaba.qlexpress4.runtime.data.implicit.QLConvertResultType;

/**
 * @Author TaoKan
 * @Date 2022/6/26 下午3:40
 */
public class BooleanConversion {
    public static QLConvertResult trans(Object object) {
        //delete null to boolean
        if (object instanceof Boolean) {
            return new QLConvertResult(QLConvertResultType.CAN_TRANS, object);
        }
        return new QLConvertResult(QLConvertResultType.NOT_TRANS, null);
    }

}
