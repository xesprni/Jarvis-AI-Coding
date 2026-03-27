package com.qihoo.finance.lowcode.convertor.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MethodDTO {

    private String methodRet;
    private String methodName;
    private String paramDefine;
    private boolean extractDataField;
    private String callRetType;
    private String callRetVar;
    private String interfaceVar;
    private String callParam;
    private String returnStr;
    private String checkResultMethod;
    private String throwStatement;
}
