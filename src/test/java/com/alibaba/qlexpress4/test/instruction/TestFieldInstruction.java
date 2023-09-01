package com.alibaba.qlexpress4.test.instruction;

import com.alibaba.qlexpress4.QLOptions;
import com.alibaba.qlexpress4.exception.ErrorReporter;
import com.alibaba.qlexpress4.exception.MockErrorReporter;
import com.alibaba.qlexpress4.exception.QLRuntimeException;
import com.alibaba.qlexpress4.runtime.LeftValue;
import com.alibaba.qlexpress4.runtime.MetaClass;
import com.alibaba.qlexpress4.runtime.data.DataValue;
import com.alibaba.qlexpress4.runtime.instruction.GetFieldInstruction;
import com.alibaba.qlexpress4.test.property.Child;
import com.alibaba.qlexpress4.test.property.Parent;
import com.alibaba.qlexpress4.test.property.TestEnum;
import org.junit.Assert;
import org.junit.Test;

/**
 * @Author TaoKan
 * @Date 2022/5/4 上午8:36
 */
public class TestFieldInstruction {
    /**
     * error case(Static method of Parent)
     * Parent::getStaticGet()  setMethod is null and fieldNotAccess
     *
     * @throws Exception
     */
    @Test
    public void case1() throws Exception{
        ErrorReporter errorReporter = new MockErrorReporter();
        GetFieldInstruction getFieldInstruction = new GetFieldInstruction(errorReporter, "staticGet", false);
        TestQContextParent testQContextParent = new TestQContextParent(false);
        testQContextParent.push(new DataValue(new MetaClass(Parent.class)));
        getFieldInstruction.execute(testQContextParent, QLOptions.DEFAULT_OPTIONS);
        Assert.assertEquals((testQContextParent.getValue()).get(),"staticGet1");
     }


    /**
     * public static case(Static field of Parent)
     * Parent.staticSet setAble
     *
     * @throws Exception
     */
    @Test
    public void case2() throws Exception{
        ErrorReporter errorReporter = new MockErrorReporter();
        GetFieldInstruction getFieldInstruction = new GetFieldInstruction(errorReporter, "staticSet", false);
        TestQContextParent testQContextParent = new TestQContextParent(false);
        testQContextParent.push(new DataValue(new MetaClass(Parent.class)));
        getFieldInstruction.execute(testQContextParent, QLOptions.DEFAULT_OPTIONS);
        ((LeftValue) testQContextParent.getValue()).set("staticSet1", errorReporter);
        Assert.assertEquals((testQContextParent.getValue()).get(),"staticSet1");
    }

    /**
     * private static case(Static field of Parent)
     * Parent.staticSetPrivate notSetAble
     *
     * @throws Exception
     */
    @Test
    public void case3() throws Exception{
        ErrorReporter errorReporter = new MockErrorReporter();
        GetFieldInstruction getFieldInstruction = new GetFieldInstruction(errorReporter, "staticSetPrivate", false);
        TestQContextParent testQContextParent = new TestQContextParent(false);
        testQContextParent.push(new DataValue(new MetaClass(Parent.class)));
        try {
            getFieldInstruction.execute(testQContextParent, QLOptions.DEFAULT_OPTIONS);
        }catch (Exception e){
            Assert.assertTrue(e != null);
            return;
        }
        Assert.assertTrue(false);
    }

    /**
     * private static case(Static field of Parent)
     * Parent.staticSetPrivate allowSetAble
     *
     * @throws Exception
     */
    @Test
    public void case4() throws Exception{
        ErrorReporter errorReporter = new MockErrorReporter();
        GetFieldInstruction getFieldInstruction = new GetFieldInstruction(errorReporter, "staticSetPrivate", false);
        TestQContextParent testQContextParent = new TestQContextParent(true);
        testQContextParent.push(new DataValue(new MetaClass(Parent.class)));
        getFieldInstruction.execute(testQContextParent, QLOptions.DEFAULT_OPTIONS);
        Assert.assertEquals((testQContextParent.getValue()).get(),"staticSetPrivate");
    }

    /**
     * private static case(Static field of Parent)
     * Parent.staticSetPrivate allowSetAble
     *
     * @throws Exception
     */
    @Test
    public void case5() throws Exception{
        ErrorReporter errorReporter = new MockErrorReporter();
        GetFieldInstruction getFieldInstruction = new GetFieldInstruction(errorReporter, "staticSetPrivate", false);
        TestQContextParent testQContextParent = new TestQContextParent(true);
        testQContextParent.push(new DataValue(new MetaClass(Parent.class)));
        getFieldInstruction.execute(testQContextParent, QLOptions.DEFAULT_OPTIONS);
        Assert.assertEquals((testQContextParent.getValue()).get(),"staticSetPrivate");
    }


    /**
     * static case(Static method of Parent instance)
     * parent.getStaticGet()
     *
     * @throws Exception
     */
    @Test
    public void case7() throws Exception{
        ErrorReporter errorReporter = new MockErrorReporter();
        GetFieldInstruction getFieldInstruction = new GetFieldInstruction(errorReporter, "staticGet", false);
        TestQContextParent testQContextParent = new TestQContextParent(true);
        testQContextParent.push(new Parent());
        getFieldInstruction.execute(testQContextParent, QLOptions.DEFAULT_OPTIONS);
        Assert.assertEquals((testQContextParent.getValue()).get(),"staticGet1");
    }


    /**
     * normal case(normal method of Parent instance)
     * parent.getAge()
     *
     * @throws Exception
     */
    @Test
    public void case8() throws Exception{
        ErrorReporter errorReporter = new MockErrorReporter();
        GetFieldInstruction getFieldInstruction = new GetFieldInstruction(errorReporter, "age", false);
        TestQContextParent testQContextParent = new TestQContextParent(false);
        testQContextParent.push(new Parent());
        getFieldInstruction.execute(testQContextParent, QLOptions.DEFAULT_OPTIONS);
        ((LeftValue) testQContextParent.getValue()).set(35,errorReporter);
        Assert.assertEquals((testQContextParent.getValue()).get(),35);
    }

    /**
     * normal case(normal field of Parent instance)
     * parent.name accessible
     *
     * @throws Exception
     */
    @Test
    public void case9() throws Exception{
        ErrorReporter errorReporter = new MockErrorReporter();
        GetFieldInstruction getFieldInstruction = new GetFieldInstruction(errorReporter, "name", false);
        TestQContextParent testQContextParent = new TestQContextParent(true);
        testQContextParent.push(new Parent());
        getFieldInstruction.execute(testQContextParent, QLOptions.DEFAULT_OPTIONS);
        ((LeftValue) testQContextParent.getValue()).set("name1",errorReporter);
        Assert.assertEquals((testQContextParent.getValue()).get(),"name1");
    }


    /**
     * error case(normal field of Parent instance)
     * parent.name with not accessible
     *
     * @throws Exception
     */
    @Test
    public void case10() throws Exception{
        ErrorReporter errorReporter = new MockErrorReporter();
        GetFieldInstruction getFieldInstruction = new GetFieldInstruction(errorReporter, "name", false);
        TestQContextParent testQContextParent = new TestQContextParent(false);
        testQContextParent.push(new Parent());
        try {
            getFieldInstruction.execute(testQContextParent, QLOptions.DEFAULT_OPTIONS);
            ((LeftValue) testQContextParent.getValue()).set("name1",errorReporter);
            Assert.assertEquals((testQContextParent.getValue()).get(),"name1");
            Assert.fail();
        } catch (QLRuntimeException e){
            Assert.assertNotNull(e);
        }
    }

    /**
     * normal case(normal method of Parent,Child instance)
     * child.getAge() instead of parent.getAge()
     *
     * @throws Exception
     */
    @Test
    public void case11() throws Exception{
        ErrorReporter errorReporter = new MockErrorReporter();
        GetFieldInstruction getFieldInstruction = new GetFieldInstruction(errorReporter, "age", false);
        TestQContextParent testQContextParent = new TestQContextParent(true);
        testQContextParent.push(new Child());
        getFieldInstruction.execute(testQContextParent, QLOptions.DEFAULT_OPTIONS);
        Assert.assertEquals((testQContextParent.getValue()).get(),11);
    }



    /**
     * normal case(normal method of Parent,Child instance)
     * parent.getBirth() public instead of child.birth(not allow)
     *
     * @throws Exception
     */
    @Test
    public void case12() throws Exception{
        ErrorReporter errorReporter = new MockErrorReporter();
        GetFieldInstruction getFieldInstruction = new GetFieldInstruction(errorReporter, "birth", false);
        TestQContextParent testQContextParent = new TestQContextParent(false);
        testQContextParent.push(new Child());
        getFieldInstruction.execute(testQContextParent, QLOptions.DEFAULT_OPTIONS);
        Assert.assertEquals((testQContextParent.getValue()).get(),"2022-01-01");
    }



    /**
     * error case(normal method of Parent instance)
     * parent.getMethod1() not findl
     *
     * @throws Exception
     */
    @Test
    public void case13() throws Exception{
        ErrorReporter errorReporter = new MockErrorReporter();
        GetFieldInstruction getFieldInstruction = new GetFieldInstruction(errorReporter, "method1", false);
        TestQContextParent testQContextParent = new TestQContextParent(true);
        testQContextParent.push(new Child());
        try {
            getFieldInstruction.execute(testQContextParent, QLOptions.DEFAULT_OPTIONS);
            Assert.fail();
        } catch (QLRuntimeException e){
            Assert.assertNotNull(e);
        }
    }


    /**
     * enum attr get
     * TestEnum.skt.getValue()
     *
     * @throws Exception
     */
    @Test
    public void case14() throws Exception{
        ErrorReporter errorReporter = new MockErrorReporter();
        GetFieldInstruction getFieldInstruction = new GetFieldInstruction(errorReporter, "SKT", false);
        TestQContextParent testQContextParent = new TestQContextParent(true);
        testQContextParent.push(new DataValue(new MetaClass(TestEnum.class)));
        getFieldInstruction.execute(testQContextParent, QLOptions.DEFAULT_OPTIONS);
        GetFieldInstruction getFieldInstruction1 = new GetFieldInstruction(errorReporter, "value", false);
        getFieldInstruction1.execute(testQContextParent,QLOptions.DEFAULT_OPTIONS);
        Assert.assertEquals((testQContextParent.getValue()).get(),-1);
    }

}
