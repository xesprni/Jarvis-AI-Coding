package com.qihoo.finance.lowcode.gentracker.tool;

import com.qihoo.finance.lowcode.common.utils.LocalDateUtils;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateVelocityOptions;
import com.qihoo.finance.lowcode.gentracker.entity.TableInfo;
import com.qihoo.finance.lowcode.gentracker.service.impl.MySQLGenerateServiceImpl;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * 额外的代码生成工具
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class ExtraCodeGenerateUtils {
    /**
     * 代码生成服务
     */
    private final MySQLGenerateServiceImpl codeGenerateService;
    /**
     * 表信息对象
     */
    private final TableInfo tableInfo;
    /**
     * 生成配置
     */
    private final GenerateVelocityOptions generateOptions;

    public ExtraCodeGenerateUtils(MySQLGenerateServiceImpl codeGenerateService, TableInfo tableInfo, GenerateVelocityOptions generateOptions) {
        this.codeGenerateService = codeGenerateService;
        this.tableInfo = tableInfo;
        this.generateOptions = generateOptions;
    }

    public static String genCopyright(String company, String project, String author, String batchNo, String... metaInfos) {
        StringBuilder right = new StringBuilder("/*\n" + " *   @copyright      Copyright %s . [%s] All rights reserved.\n"
                + " *   @batchNo        %s\n");

        for (String metaInfo : metaInfos) {
            right.append(metaInfo);
        }

        right.append(" *   重要提示：此处为代码水印，不要删除！\n");
        String end = " */";
        right.append(end);

        right = new StringBuilder(String.format(right.toString(), LocalDateUtils.convertToPatternString(new Date(), "yyyy"), company, batchNo));
        return right.toString();
    }

    public static String copyrightMeta(String key, String value) {
        String keyStr = String.format(" *   @%s", key);
        return StringUtils.rightPad(keyStr, 21) + value + "\n";
    }
}
