/*
 * Copyright (c) 2018 David Boissier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qihoo.finance.lowcode.console.mongo.view.renderer;


import com.intellij.ui.ColoredTableCellRenderer;
import com.qihoo.finance.lowcode.common.utils.LocalDateUtils;
import com.qihoo.finance.lowcode.console.mongo.view.style.StyleAttributesProvider;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Date;

public class MongoTableCellRenderer extends ColoredTableCellRenderer {

    @Override
    protected void customizeCellRenderer(@NotNull JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {

        if (value == null) {
            append("null", StyleAttributesProvider.getNullAttribute());
        } else {
            if (value instanceof Number) {
                append(String.valueOf(value), StyleAttributesProvider.getNumberAttribute());
            } else if (value instanceof Boolean) {
                append(String.valueOf(value), StyleAttributesProvider.getBooleanAttribute());
            } else if (value instanceof Document) {
                append(((Document) value).toJson(), StyleAttributesProvider.getDocumentAttribute());
            } else if (value instanceof ObjectId) {
                append(String.valueOf(value), StyleAttributesProvider.getObjectIdAttribute());
            } else if (value instanceof Date) {
                append(LocalDateUtils.convertToPatternString((Date) value, LocalDateUtils.FORMAT_DATE_TIME), StyleAttributesProvider.getDateAttribute());
            } else {
                append(String.valueOf(value), StyleAttributesProvider.getStringAttribute());
            }
        }
    }
}
