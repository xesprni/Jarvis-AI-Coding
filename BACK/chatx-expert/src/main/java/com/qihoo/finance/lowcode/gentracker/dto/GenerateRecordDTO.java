package com.qihoo.finance.lowcode.gentracker.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
public class GenerateRecordDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String batchNo;
    private String date;
    private String project;
    private String operator;
    private List<String> tableNames;
    private List<String> templateNames;
    private GenerateVelocityOptions generateOptions;
    private Map<String, Object> otherParam;
    private List<GenerateTableDTO> selectTables;

    public static GenerateRecordDTO newInstance(String date, String project, String operator, List<String> tableNames, List<String> templates, GenerateVelocityOptions generateOptions, Map<String, Object> otherParam) {
        GenerateRecordDTO generateRecord = new GenerateRecordDTO();
        generateRecord.date = date;
        generateRecord.project = project;
        generateRecord.operator = operator;
        generateRecord.tableNames = tableNames;
        generateRecord.templateNames = templates;
        generateRecord.generateOptions = generateOptions;
        generateRecord.otherParam = Objects.isNull(otherParam) ? new HashMap<>() : otherParam;

        return generateRecord;
    }
}
