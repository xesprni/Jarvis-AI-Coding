package com.qihoo.finance.lowcode.design.constant;

import com.google.common.collect.Lists;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbFieldTypeConfig;

import java.util.*;

/**
 * TypeMatch
 *
 * @author fengjinfu-jk
 * date 2023/12/12
 * @version 1.0.0
 * @apiNote TypeMatch
 */
public class FieldTypeMatch {
    public static final Map<String, RdbFieldTypeConfig> FIELD_TYPE_CONFIG;
    public static final List<String> DATE_TYPE;

    static {
        FIELD_TYPE_CONFIG = new LinkedHashMap<>();
        FIELD_TYPE_CONFIG.put("bigint", RdbFieldTypeConfig.builder().fieldTypeName("bigint").needFieldLength(true).needFieldPrecision(false).defaultJdbcType("BIGINT").defaultJavaType("java.lang.Long").build());
        FIELD_TYPE_CONFIG.put("binary", RdbFieldTypeConfig.builder().fieldTypeName("binary").needFieldLength(true).needFieldPrecision(false).defaultJdbcType("BINARY").defaultJavaType("java.lang.Byte[]").build());
        FIELD_TYPE_CONFIG.put("bit", RdbFieldTypeConfig.builder().fieldTypeName("bit").needFieldLength(true).needFieldPrecision(false).defaultJdbcType("BIT").defaultJavaType("java.lang.Boolean").build());
        FIELD_TYPE_CONFIG.put("blob", RdbFieldTypeConfig.builder().fieldTypeName("blob").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("BLOB").defaultJavaType("java.lang.Object").build());
        FIELD_TYPE_CONFIG.put("char", RdbFieldTypeConfig.builder().fieldTypeName("char").needFieldLength(true).needFieldPrecision(false).defaultJdbcType("CHAR").defaultJavaType("java.lang.String").build());
        FIELD_TYPE_CONFIG.put("date", RdbFieldTypeConfig.builder().fieldTypeName("date").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("DATE").defaultJavaType("java.util.Date").build());
        FIELD_TYPE_CONFIG.put("datetime", RdbFieldTypeConfig.builder().fieldTypeName("datetime").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("TIMESTAMP").defaultJavaType("java.util.Date").build());
        FIELD_TYPE_CONFIG.put("decimal", RdbFieldTypeConfig.builder().fieldTypeName("decimal").needFieldLength(true).needFieldPrecision(true).defaultJdbcType("DECIMAL").defaultJavaType("java.math.BigDecimal").build());
        FIELD_TYPE_CONFIG.put("double", RdbFieldTypeConfig.builder().fieldTypeName("double").needFieldLength(true).needFieldPrecision(true).defaultJdbcType("DOUBLE").defaultJavaType("java.lang.Double").build());
        FIELD_TYPE_CONFIG.put("enum", RdbFieldTypeConfig.builder().fieldTypeName("enum").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("VARCHAR").defaultJavaType("java.lang.String").build());
        FIELD_TYPE_CONFIG.put("float", RdbFieldTypeConfig.builder().fieldTypeName("float").needFieldLength(true).needFieldPrecision(true).defaultJdbcType("FLOAT").defaultJavaType("java.lang.Float").build());
        FIELD_TYPE_CONFIG.put("geometry", RdbFieldTypeConfig.builder().fieldTypeName("geometry").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("VARCHAR").defaultJavaType("java.lang.Object").build());
        FIELD_TYPE_CONFIG.put("geometrycollection", RdbFieldTypeConfig.builder().fieldTypeName("geometrycollection").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("VARCHAR").defaultJavaType("java.lang.Object").build());
        FIELD_TYPE_CONFIG.put("int", RdbFieldTypeConfig.builder().fieldTypeName("int").needFieldLength(true).needFieldPrecision(false).defaultJdbcType("INTEGER").defaultJavaType("java.lang.Integer").build());
        FIELD_TYPE_CONFIG.put("integer", RdbFieldTypeConfig.builder().fieldTypeName("integer").needFieldLength(true).needFieldPrecision(false).defaultJdbcType("INTEGER").defaultJavaType("java.lang.Integer").build());
        FIELD_TYPE_CONFIG.put("longtext", RdbFieldTypeConfig.builder().fieldTypeName("longtext").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("LONGVARCHAR").defaultJavaType("java.lang.String").build());
        FIELD_TYPE_CONFIG.put("mediumblob", RdbFieldTypeConfig.builder().fieldTypeName("mediumblob").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("BLOB").defaultJavaType("java.lang.Object").build());
        FIELD_TYPE_CONFIG.put("mediumint", RdbFieldTypeConfig.builder().fieldTypeName("mediumint").needFieldLength(true).needFieldPrecision(false).defaultJdbcType("INTEGER").defaultJavaType("java.lang.Integer").build());
        FIELD_TYPE_CONFIG.put("mediumtext", RdbFieldTypeConfig.builder().fieldTypeName("mediumtext").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("VARCHAR").defaultJavaType("java.lang.String").build());
        FIELD_TYPE_CONFIG.put("numeric", RdbFieldTypeConfig.builder().fieldTypeName("numeric").needFieldLength(true).needFieldPrecision(true).defaultJdbcType("NUMERIC").defaultJavaType("java.math.BigDecimal").build());
        FIELD_TYPE_CONFIG.put("real", RdbFieldTypeConfig.builder().fieldTypeName("real").needFieldLength(true).needFieldPrecision(true).defaultJdbcType("REAL").defaultJavaType("java.lang.Float").build());
        FIELD_TYPE_CONFIG.put("set", RdbFieldTypeConfig.builder().fieldTypeName("set").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("VARCHAR").defaultJavaType("java.lang.Object").build());
        FIELD_TYPE_CONFIG.put("smallint", RdbFieldTypeConfig.builder().fieldTypeName("smallint").needFieldLength(true).needFieldPrecision(false).defaultJdbcType("SMALLINT").defaultJavaType("java.lang.Short").build());
        FIELD_TYPE_CONFIG.put("text", RdbFieldTypeConfig.builder().fieldTypeName("text").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("VARCHAR").defaultJavaType("java.lang.String").build());
        FIELD_TYPE_CONFIG.put("time", RdbFieldTypeConfig.builder().fieldTypeName("time").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("TIME").defaultJavaType("java.util.Date").build());
        // FIELD_TYPE_CONFIG.put("timestamp", RdbFieldTypeConfig.builder().fieldTypeName("timestamp").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("TIMESTAMP").defaultJavaType("java.util.Date").build());
        FIELD_TYPE_CONFIG.put("tinyint", RdbFieldTypeConfig.builder().fieldTypeName("tinyint").needFieldLength(true).needFieldPrecision(false).defaultJdbcType("INTEGER").defaultJavaType("java.lang.Integer").build());
        FIELD_TYPE_CONFIG.put("varchar", RdbFieldTypeConfig.builder().fieldTypeName("varchar").needFieldLength(true).needFieldPrecision(false).defaultJdbcType("VARCHAR").defaultJavaType("java.lang.String").build());
        FIELD_TYPE_CONFIG.put("tinytext", RdbFieldTypeConfig.builder().fieldTypeName("tinytext").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("VARCHAR").defaultJavaType("java.lang.String").build());
        FIELD_TYPE_CONFIG.put("year", RdbFieldTypeConfig.builder().fieldTypeName("year").needFieldLength(false).needFieldPrecision(false).defaultJdbcType("Date").defaultJavaType("java.util.Date").build());

        DATE_TYPE = new ArrayList<>();
        DATE_TYPE.add("date");
        DATE_TYPE.add("datetime");
        DATE_TYPE.add("time");
        // DATE_TYPE.add("timestamp");
    }

    public static final List<String> WARNING_TYPE = Lists.newArrayList("java.lang.Object");

    public static Collection<String> getFieldTypes() {
        return FIELD_TYPE_CONFIG.keySet();
    }

    public static RdbFieldTypeConfig getFieldTypeConfig(String fieldType) {
        String[] split = fieldType.split("\\(");
        String realFieldType = split[0];
        return FIELD_TYPE_CONFIG.get(realFieldType.toLowerCase());
    }

    /**
     * 默认的Java类型列表
     */
    public static String[] DEFAULT_JAVA_TYPE_LIST = new String[]{
            "java.lang.String",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Boolean",
            "java.util.Date",
            "java.time.LocalDateTime",
            "java.time.LocalDate",
            "java.time.LocalTime",
            "java.lang.Short",
            "java.lang.Byte",
            "java.lang.Byte[]",
            "java.lang.Character",
            "java.lang.Character",
            "java.math.BigDecimal",
            "java.math.BigInteger",
            "java.lang.Double",
            "java.lang.Float",
            "java.lang.String[]",
            "java.util.List",
            "java.util.Set",
            "java.util.Map",
            "java.lang.Object",
    };
    public static String DEFAULT_JAVA_TYPE = "java.lang.Object";

    /**
     * 默认的JDBC类型列表
     */
    public static String[] DEFAULT_JDBC_TYPE_LIST = new String[]{
            "CHAR",
            "VARCHAR",
            "LONGVARCHAR",
            "NUMERIC",
            "DECIMAL",
            "BIT",
            "BOOLEAN",
            "TINYINT",
            "SMALLINT",
            "INTEGER",
            "BIGINT",
            "REAL",
            "FLOAT",
            "DOUBLE",
            "BINARY",
            "VARBINARY",
            "LONGVARBINARY",
            "DATE",
            "TIME",
            // "TIMESTAMP",
            "CLOB",
            "BLOB",
            "ARRAY",
            "DISTINCT",
            "STRUCT",
            "REF",
            "DATALINK",
    };
    public static String DEFAULT_JDBC_TYPE = "VARCHAR";
}
