package com.ql.util.express.test.issue;

import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;

/**
 * @Author TaoKan
 * @Date 2023/4/14 下午2:07
 */
public class Issue249 {
    public static void main(String[] args) throws Exception {
        ExpressRunner runner = new ExpressRunner(true, false);
        String[] a = runner.getOutVarNames("a = 3; a > b;");
        for(int i = 0; i < a.length; i++){
            System.out.println(a[i]);
        }

        a = runner.getOutVarNames("a = 3;c = a; a > b;");
        for(int i = 0; i < a.length; i++){
            System.out.println(a[i]);
        }

        String express = "int 平均分 = (语文 + 数学 + 英语 + 综合考试.科目2) / 4.0; return 平均分";
        String[] names = runner.getOutVarNames(express);
        for(String s:names){
            System.out.println("var : " + s);
        }

    }
}
