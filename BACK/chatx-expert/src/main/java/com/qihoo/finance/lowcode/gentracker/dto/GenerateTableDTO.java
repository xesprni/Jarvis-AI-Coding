package com.qihoo.finance.lowcode.gentracker.dto;

import java.io.Serializable;

public class GenerateTableDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String tableName;
    private String saveModelName;
    private String savePackageName;
    private String savePath;
    private String templateGroupName;
    private String batchNo;
    private String operator;
    private String project;

    public static GenerateTableDTO newInstance(String tableName, String saveModelName, String savePackageName, String savePath, String templateGroupName, String batchNo, String operator, String project) {
        GenerateTableDTO generateTable = new GenerateTableDTO();
        generateTable.tableName = tableName;
        generateTable.saveModelName = saveModelName;
        generateTable.savePackageName = savePackageName;
        generateTable.savePath = savePath;
        generateTable.templateGroupName = templateGroupName;
        generateTable.batchNo = batchNo;
        generateTable.operator = operator;
        generateTable.project = project;

        return generateTable;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSaveModelName() {
        return saveModelName;
    }

    public void setSaveModelName(String saveModelName) {
        this.saveModelName = saveModelName;
    }

    public String getSavePackageName() {
        return savePackageName;
    }

    public void setSavePackageName(String savePackageName) {
        this.savePackageName = savePackageName;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getTemplateGroupName() {
        return templateGroupName;
    }

    public void setTemplateGroupName(String templateGroupName) {
        this.templateGroupName = templateGroupName;
    }
}
