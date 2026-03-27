package com.qihoo.finance.lowcode.convertor.util;

import com.qihoo.finance.lowcode.common.util.UserUtils;
import com.qihoo.finance.lowcode.common.utils.CopyrightUtils;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TemplateUtil {

    public final static String TEMPLATE_CONVERT_CLASS = "velocity/convertor/class.vm";
    public final static String TEMPLATE_CONVERT_METHOD = "velocity/convertor/method.vm";
    public final static String TEMPLATE_CONVERT_METHOD_BLOCK = "velocity/convertor/method_block.vm";
    public final static String TEMPLATE_MEDIATOR_CLASS = "velocity/mediator/class.vm";
    public final static String TEMPLATE_MEDIATOR_METHOD = "velocity/mediator/method.vm";

    @SneakyThrows
    public static String getTemplateContent(String templatePath) {
        try (InputStream is = ConvertCodeGenUtil.class.getClassLoader().getResourceAsStream(templatePath)) {
            byte[] bytes = IOUtils.toByteArray(is);
            return new String(bytes, StandardCharsets.UTF_8).replaceAll("\r\n", "\n");
        }
    }

    static Map<String, Object> buildBaseParamMap(String batchNo, String module) {
        Map<String, Object> paramMap = buildBaseParamMapForMethod();
        String copyright = CopyrightUtils.genCopyright("JARVIS", module, (String)paramMap.get("author"), batchNo);
        paramMap.put("copyright", copyright);
        paramMap.put("tool", new ConvertorVelocityUtil());
        return paramMap;
    }

    static Map<String, Object> buildBaseParamMapForMethod() {
        Map<String, Object> paramMap = new HashMap<>();
        String author = StringUtils.defaultIfEmpty(UserUtils.getUserEmail(), "LowCodeGenerate");
        paramMap.put("author", author);
        paramMap.put("tool", new ConvertorVelocityUtil());
        return paramMap;
    }
}
