/*
 *  Copyright (c) 2017-2019, bruce.ge.
 *    This program is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU General Public License
 *    as published by the Free Software Foundation; version 2 of
 *    the License.
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU General Public License for more details.
 *    You should have received a copy of the GNU General Public License
 *    along with this program;
 */

package com.qihoo.finance.lowcode.convertor.action;

import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.convertor.GenerateAllHandlerAdapter;
import org.jetbrains.annotations.NotNull;

/**
 * Created by bruce.ge on 2016/12/23.
 */
public class GenerateAllSetterAction extends GenerateAllSetterBase {


    public GenerateAllSetterAction() {
        super(new GenerateAllHandlerAdapter() {
            @Override
            public boolean shouldAddDefaultValue() {
                return true;
            }
        });
    }

    @NotNull
    @Override
    public String getText() {
        return Constants.GenerateSetter.GENERATE_SETTER_METHOD;
    }
}
