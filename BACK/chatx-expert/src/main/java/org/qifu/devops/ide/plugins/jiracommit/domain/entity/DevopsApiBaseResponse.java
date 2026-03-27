package org.qifu.devops.ide.plugins.jiracommit.domain.entity;

public class DevopsApiBaseResponse {
    private String flag;

    private String code;

    private String msg;


    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
