package com.qihoo.finance.lowcode.common.entity;

import com.qihoo.finance.lowcode.common.entity.base.Result;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * CustResult
 *
 * @author fengjinfu-jk
 * date 2023/9/14
 * @version 1.0.0
 * @apiNote CustResult
 */
@Getter
@Setter
public class CustomResult<T> extends Result<T> {
    private Date start;
    private Date end;
}
