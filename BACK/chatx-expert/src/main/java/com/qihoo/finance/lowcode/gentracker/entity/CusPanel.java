package com.qihoo.finance.lowcode.gentracker.entity;

import lombok.Data;

import java.util.List;

/**
 * CustSettingPanel
 *
 * @author fengjinfu-jk
 * date 2023/10/19
 * @version 1.0.0
 * @apiNote CustSettingPanel
 */
@Data
public class CusPanel {
    private String labelTxt;
    private String labelKey;
    private ComponentType componentType;
    private List<String> values;

    public static enum ComponentType {
        LABEL, TEXT_FIELD, COMBO_BOX, CHECK_BOX
    }
}
