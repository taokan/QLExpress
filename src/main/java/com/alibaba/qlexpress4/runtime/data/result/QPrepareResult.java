package com.alibaba.qlexpress4.runtime.data.result;

/**
 * @Author TaoKan
 * @Date 2022/12/25 下午7:11
 */
public class QPrepareResult {
    private boolean result;
    private String errorMsg;

    public QPrepareResult(boolean result){
        this.result = result;
    }

    public QPrepareResult(boolean result, String errorMsg){
        this.result = result;
        this.errorMsg = errorMsg;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
