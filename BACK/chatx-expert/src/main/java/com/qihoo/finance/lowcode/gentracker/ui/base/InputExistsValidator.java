package com.qihoo.finance.lowcode.gentracker.ui.base;

import com.intellij.openapi.ui.InputValidator;
import com.qihoo.finance.lowcode.gentracker.tool.StringUtils;

import java.util.Collection;

/**
 * 输入存在验证器
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class InputExistsValidator implements InputValidator {

    private final Collection<String> itemList;

    public InputExistsValidator(Collection<String> itemList) {
        this.itemList = itemList;
    }

    @Override
    public boolean checkInput(String inputString) {
        return !StringUtils.isEmpty(inputString) && !itemList.contains(inputString);
    }

    @Override
    public boolean canClose(String inputString) {
        return this.checkInput(inputString);
    }
}
