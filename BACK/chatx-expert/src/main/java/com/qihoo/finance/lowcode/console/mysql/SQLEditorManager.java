package com.qihoo.finance.lowcode.console.mysql;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteHistory;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLDataEditor;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLFileSystem;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLVirtualFile;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.Icon;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SQLConsoleEditor
 *
 * @author fengjinfu-jk
 * date 2023/9/12
 * @version 1.0.0
 * @apiNote SQLConsoleEditor
 */
@Slf4j
public class SQLEditorManager {
    public static final String SQL_SUFFIX = ".sql";
    public static final String JS_SUFFIX = ".js";
    public static final FileType SQL;
    public static final FileType MONGODB;
    public static Map<String, VirtualFile> consoles = new HashMap<>();
    public static final String FILE_NAME = "SQL Query";
    private static final String SQL_CONSOLE_PATH = Paths.get(PathManager.getConfigPath(), "EXTENSIONS", "chatx-expert", "console").toString();
    public static final Key<String> SQL_CONSOLE_DATASOURCE = Key.create("SQL_CONSOLE_DATASOURCE");
    public static final Key<Boolean> SQL_CONSOLE = Key.create("SQL_CONSOLE");
    public static final Key<SQLExecuteHistory> SQL_HISTORY = Key.create("SQL_HISTORY");
    public static final Key<SQLDataEditor> SQL_DATA_EDITOR = Key.create("SQL_DATA_EDITOR");

    static {
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        SQL = fileTypeManager.getFileTypeByFileName(SQLEditorManager.FILE_NAME + SQL_SUFFIX);
        MONGODB = fileTypeManager.getFileTypeByFileName(SQLEditorManager.FILE_NAME + SQL_SUFFIX);
    }

    public static boolean isMySQLConsole(VirtualFile file) {
        return Constants.DataSource.MySQL.equals(file.getUserData(SQL_CONSOLE_DATASOURCE));
    }

    public static void openTempSQLConsole(Project project, DatabaseNode dbNode) {
        openTempSQLConsole(project, dbNode, null);
    }

    public static void openTempSQLConsole(Project project, DatabaseNode dbNode, SQLExecuteHistory sqlHistory) {
        String consoleName = Objects.nonNull(sqlHistory) ? sqlHistory.getShowSQLConsoleName() : genConsoleName();
        SQLVirtualFile virtualFile = new SQLVirtualFile(project, consoleName, SQL);
        // 绑定数据库
        DataContext.getInstance(project).getConsoleDatabase().put(virtualFile.getName(), dbNode);
        // 打开文件
        String datasourceType = StringUtils.defaultString(DataContext.getInstance(project).getDatasourceType(), Constants.DataSource.MySQL);
        virtualFile.putUserData(SQL_CONSOLE_DATASOURCE, datasourceType);
        virtualFile.putUserData(SQL_CONSOLE, true);
        if (Objects.nonNull(sqlHistory)) {
            virtualFile.putUserData(SQL_HISTORY, sqlHistory);
        }

        SQLFileSystem.getInstance().openEditor(virtualFile);
        log.info("fileEditorManager.openEditor");
    }

    private static FileType getFileType(String datasourceType) {
        if (Constants.DataSource.MongoDB.equals(datasourceType)) {
            return MONGODB;
        }

        return SQL;
    }

    private static String genConsoleName() {
        int consoleCount;

        try {
            consoleCount = consoles.keySet().stream().filter(name -> name.startsWith(FILE_NAME)).map(name -> {
                String fileName = name.substring(name.lastIndexOf(FILE_NAME));
                return fileName.replace(FILE_NAME, StringUtils.EMPTY);
            }).map(Integer::parseInt).max(Integer::compareTo).orElse(0);
        } catch (Exception e) {
            consoleCount = 0;
        }

        consoleCount++;
        return FILE_NAME + consoleCount;
    }

    public static boolean isSQLConsole(VirtualFile file) {
        if (Boolean.TRUE.equals(file.getUserData(SQLEditorManager.SQL_CONSOLE))) {
            return true;
        }

        String fieldName = file.getName();
        if (StringUtils.isEmpty(fieldName)) {
            return false;
        }

        return fieldName.startsWith(FILE_NAME);
    }

    public static Icon datasourceIcon(String datasourceType) {
        switch (datasourceType) {
            case Constants.DataSource.MySQL:
                return Icons.scaleToWidth(Icons.Providers.Mysql, 13);
            case Constants.DataSource.Oracle:
                return Icons.scaleToWidth(Icons.Providers.Oracle, 13);
            case Constants.DataSource.Postgresql:
                return Icons.scaleToWidth(Icons.Providers.Postgresql, 13);
            case Constants.DataSource.MongoDB:
                return Icons.scaleToWidth(Icons.Providers.MongoDB, 13);
            default:
                return Icons.scaleToWidth(Icons.DB_SELECT, 13);
        }
    }
}
