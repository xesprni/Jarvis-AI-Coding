package com.qihoo.finance.lowcode.design.dto;

import lombok.Data;

import javax.swing.*;

/**
 * SimpleDatasource
 *
 * @author fengjinfu-jk
 * date 2023/12/21
 * @version 1.0.0
 * @apiNote SimpleDatasource
 */
@Data
public class SimpleDatasource {
    private String datasource;
    private Icon icon;

    public SimpleDatasource(String datasource, Icon icon) {
        this.datasource = datasource;
        this.icon = icon;
    }
}
