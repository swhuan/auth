package com.huan.auth.common.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author swuhuan
 * @desc 返回实体类
 */
public class ResultEntity {
    /**
     *
     * 数据
     */
    private Object data;
    /**
     * 状态码
     */
    @JsonIgnore
    private ResultStatus status = ResultStatus.OK;
    /**
     * 返回消息
     */
    private String msg = ResultStatus.OK.getMsg();

    public ResultEntity(Object data) {
        this(ResultStatus.OK, data, ResultStatus.OK.getMsg());
    }

    public ResultEntity(ResultStatus status) {
        this(status, "", status.getMsg());
    }

    public ResultEntity(ResultStatus status, Object data) {
        this(status, status == ResultStatus.OK ? data : null,
                status == ResultStatus.OK ? ResultStatus.OK.getMsg() : (data == null ? "" : data.toString()));
    }

    public ResultEntity() {
    }

    public ResultEntity(ResultStatus status, Object data, String msg) {
        this.status = status;
        this.data = data;
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public ResultEntity setData(Object data) {
        this.data = data;
        return this;
    }

    public int getCode() {
        return status.getCode();
    }
    
    public String getMsg() {
        return msg;
    }

    public enum ResultStatus {
        OK(1000, "OK"),
        FAILURE(1001, "failure"),
        ERROR(1002, "error"),
        SENSITIVE_WORD(1003,"内容包含敏感词，请重新提交！"),
        ERROR_PARAMETER(407, "参数错误"),
        REPEAT_SUBMIT(1010,"操作太频繁啦，请5秒后重试！");

        private int code;
        private String msg;

        private ResultStatus(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}
