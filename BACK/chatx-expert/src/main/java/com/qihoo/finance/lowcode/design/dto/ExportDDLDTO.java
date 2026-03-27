package com.qihoo.finance.lowcode.design.dto;

import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ExportDDLDTO
 *
 * @author fengjinfu-jk
 * date 2023/12/28
 * @version 1.0.0
 * @apiNote ExportDDL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportDDLDTO {
    private DatabaseNode database;
    private MySQLTableNode table;
}
