package com.qihoo.finance.lowcode.declarative.action.editor;

import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider;

/**
 * DeclarativeFloatingToolbarProvider
 *
 * @author fengjinfu-jk
 * date 2024/5/20
 * @version 1.0.0
 * @apiNote DeclarativeFloatingToolbarProvider
 */
public class DeclarativeFloatingToolbarProvider extends AbstractFloatingToolbarProvider {
    public DeclarativeFloatingToolbarProvider() {
        super("Chatx.declarative.floating");
    }

    @Override
    public boolean getAutoHideable() {
        return true;
    }
}
