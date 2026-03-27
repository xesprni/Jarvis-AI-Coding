package com.qihoo.finance.lowcode.gentracker.dto;

import com.qihoo.finance.lowcode.gentracker.tool.Md5Utils;
import lombok.Data;

import java.io.Serializable;

@Data
public class GenerateFileCodeDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String batchNo;
    private String groupName;
    private String templateName;
    private String fileName;
    private String savePath;

    private String saveModelName;
    private String tableName;
    private String operator;
    private String project;
    private String code;
    private String codeMd5;

    public static GenerateFileCodeDTO newInstance(String batchNo, String tableName, String groupName, String templateName, String fileName, String savePath, String operator, String project, String saveModelName, String code) {
        GenerateFileCodeDTO generateCodeRecord = new GenerateFileCodeDTO();
        generateCodeRecord.batchNo = batchNo;
        generateCodeRecord.groupName = groupName;
        generateCodeRecord.templateName = templateName;
        generateCodeRecord.fileName = fileName;
        generateCodeRecord.savePath = savePath;
        generateCodeRecord.tableName = tableName;
        generateCodeRecord.operator = operator;
        generateCodeRecord.project = project;
        generateCodeRecord.code = code;
        generateCodeRecord.saveModelName = saveModelName;
        generateCodeRecord.codeMd5 = Md5Utils.md5Digest(code);

        return generateCodeRecord;
    }
}
