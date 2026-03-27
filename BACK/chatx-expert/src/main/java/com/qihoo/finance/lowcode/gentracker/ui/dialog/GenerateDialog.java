package com.qihoo.finance.lowcode.gentracker.ui.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.qihoo.finance.lowcode.common.configuration.UserContextPersistent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * GenerateDialog
 *
 * @author fengjinfu-jk
 * date 2023/12/25
 * @version 1.0.0
 * @apiNote GenerateDialog
 */
public abstract class GenerateDialog extends DialogWrapper {

    protected static final UserContextPersistent userContextPersistent = UserContextPersistent.getInstance();
    protected static final UserContextPersistent.UserContext userContext = UserContextPersistent.getUserContext();
    protected static final List<String> CLASS_NAME_SUFFIX = new ArrayList<>();
    /**
     * 项目对象
     */
    protected final Project project;

    static {
        CLASS_NAME_SUFFIX.add("Entity");
        CLASS_NAME_SUFFIX.add("DTO");
        CLASS_NAME_SUFFIX.add("DAO");
        CLASS_NAME_SUFFIX.add("Mapper");
        CLASS_NAME_SUFFIX.add("Service");
        CLASS_NAME_SUFFIX.add("Controller");
        CLASS_NAME_SUFFIX.add("Facade");
        CLASS_NAME_SUFFIX.add("FacadeImpl");
    }

    public GenerateDialog(@Nullable Project project) {
        super(project);
        this.project = project;
    }
}
