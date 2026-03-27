package com.qihoo.finance.lowcode.common.util;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.ScalableIcon;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.IconDeferrer;
import com.intellij.ui.JBColor;
import com.intellij.ui.icons.CachedImageIcon;
import com.intellij.ui.scale.ScaleContext;
import com.intellij.util.ImageLoader;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.ui.JBImageIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.qifu.utils.UiUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Slf4j
public class Icons extends AllIcons implements PlatformIcons {
    /*
    LIGHT:      37E544
    DEFAULT:    AFB1B3
    SELECTED:   4B6EAF
    DARKGREEN:  3BB35B
     */
    public static JBColor GREEN = new JBColor(new Color(59, 179, 91), JBColor.GREEN);
    public static Color WARNING_COLOR = new Color(255, 200, 0);
    private static final Map<String, Icon> REGISTERED_ICONS = new HashMap<>();

    public static final Icon ACTION_EXECUTE = load("/img/action/Execute.png");

    public static final Icon STMT_EXEC_RESULT_SET = load("/img/ExecutionResultSet.png");

    public static final Icon PROJECT = load("/img/database/Project.png");
    public static final Icon DBO_DATABASE_LINKS = load("/img/database/DatabaseLinks.png");

    public static final Icon LOGIN_USER = load("/img/toolbar/user.png");
    public static final Icon EMAIL = load("/img/toolbar/email.png");
    public static final Icon REFRESH = load("/img/toolbar/refresh.png");
    public static final Icon REFRESH2 = load("/img/toolbar/refresh2.png");
    public static final Icon REFRESH_LIGHT = load("/img/toolbar/refresh_light.png");
    public static final Icon LOGOUT = load("/img/toolbar/logout.png");
    public static final Icon LOGOUT_LIGHT = load("/img/toolbar/logout_light.png");
    public static final Icon ABOUT = load("/img/toolbar/about.png");
    public static final Icon UPDATE = load("/img/toolbar/update.svg");
    public static final Icon HELP = load("/img/toolbar/help.png");
    public static final Icon HOLDER = load("/img/plugins/holder.png");
    public static final Icon LOADING_ANIMATED = new AnimatedIcon.Default();
    public static final Icon SEARCH = load("/img/toolbar/search.png");
    public static final Icon FILTER = load("/img/toolbar/filter.png");
    public static final Icon MCP = load("/img/toolbar/mcp_dark.svg");
    public static final Icon MCP_MARKET = load("/img/toolbar/market.png");
    public static final Icon FILTER_LIGHT = load("/img/toolbar/filter_light.png");
    public static final Icon AGGREGATION = load("/img/database/aggregation_dark.svg");
    // TODO 自动批准的图标要更新
    public static final Icon AUTO_APPROVAL = load("/img/database/aggregation_dark.svg");

    // ~ Plugins
    //------------------------------------------------------------------------------------------------------------------
    public static final Icon CODE = load("/img/plugins/code.png");
    public static final Icon CODE2 = load("/img/plugins/code2.png");
    public static final Icon COMPUTER = load("/img/plugins/computer.png");
    public static final Icon DB_DESIGN = load("/img/plugins/db_design.png");
    public static final Icon API_DESIGN = load("/img/plugins/api_design.png");
    public static final Icon AI = load("/img/plugins/ai.png");
    public static final Icon AI2 = load("/img/plugins/ai2.png");
    public static final Icon GIFT = load("/img/plugins/gift.png");
    public static final Icon ROCKET = load("/img/plugins/rocket.png");
    public static final ImageIcon LOADING = loadResource("/img/plugins/R-C.gif");
    public static final ImageIcon LOADING2 = loadResource("/img/plugins/loading.gif");
    public static final Icon MORE = load("/img/plugins/more.png");
    public static final Icon MORE_LIGHT = load("/img/plugins/more_light.png");
    public static final Icon WINDOW_EXECUTION_CONSOLE = load("/img/window/ExecutionConsole.png");
    public static final Icon COMBO = load("/img/plugins/combo.svg");
    public static final Icon WARNING = load("/img/plugins/warning.svg");
    public static final Icon SETTING = load("/img/plugins/setting.svg");
    public static final Icon UNINSTALL = load("/img/plugins/delete.png");
    public static final Icon SETTING2 = load("/img/plugins/setting2.svg");
    public static final Icon FOLDER_SETTING = load("/img/plugins/folder_setting.svg");
    public static final Icon TRANSPARENT = load("/img/plugins/transparent.svg");
    public static final Icon OPEN = load("/img/plugins/open.svg");
    public static final Icon CLOSE = load("/img/plugins/close.svg");
    public static final Icon PAGE_FAST_NEXT = load("/img/plugins/page_fast_next.svg");
    public static final Icon PAGE_FAST_PREV = load("/img/plugins/page_fast_prev.svg");
    public static final Icon PAGE_NEXT = load("/img/plugins/page_next.svg");
    public static final Icon PAGE_PREV = load("/img/plugins/page_prev.svg");
    public static final Icon AGENT_TASK = load("/img/plugins/agent_task.svg");
    public static final Icon AGENT_TASK2 = load("/img/plugins/agent_task2.svg");

    // ~ Message
    //------------------------------------------------------------------------------------------------------------------
    public static final Icon ERROR = load("/img/message/error.png");
    public static final Icon SYS_ERROR = load("/img/message/sys_error.png");
    public static final Icon SYS_ERROR2 = load("/img/message/sys_error2.png");
    public static final Icon EMAIL_FAIL = load("/img/message/email_fail.png");
    public static final Icon EMAIL_SUCCESS = load("/img/message/email_success.png");
    public static final Icon SUCCESS_BLUE = load("/img/message/success_blue.png");
    public static final Icon SAD = load("/img/message/sad.png");
    public static final Icon SAD2 = load("/img/message/sad2.png");
    public static final Icon FILE_ERROR = load("/img/message/file_error.png");
    public static final Icon FAIL = load("/img/message/fail.png");
    public static final Icon SUCCESS = load("/img/message/success.png");

    // ~ api
    //------------------------------------------------------------------------------------------------------------------
    public static final Icon API = load("/img/api/api.png");
    public static final Icon API2 = load("/img/api/api2.png");
    public static final Icon API3 = load("/img/api/api3.png");
    public static final Icon API4 = load("/img/api/api4.png");
    public static final Icon API_GROUP = load("/img/api/api_group.png");
    public static final Icon API_GROUP2 = load("/img/api/api_group2.png");
    public static final Icon API_GROUP3 = load("/img/api/api_group3.png");
    public static final Icon APP = load("/img/api/application.png");
    public static final Icon DELETE = load("/img/api/api_delete.png");
    public static final Icon APPLICATION2 = load("/img/api/application2.png");
    public static final Icon API_APPLICATION = load("/img/api/api_application.png");
    public static final Icon CATEGORY = load("/img/api/category.png");
    public static final Icon REMOVE = load("/img/api/remove.png");
    public static final Icon CHILD = load("/img/api/child.png");
    public static final Icon CHILD_LIGHT = load("/img/api/child_light.png");
    public static final Icon API_FOLDER = load("/img/api/folder.png");
    public static final Icon FOLDER_ADD = load("/img/api/folder_add.png");
    public static final Icon FOLDER_EDIT = load("/img/api/folder_edit.png");
    public static final Icon FOLDER_DELETE = load("/img/api/folder_delete.png");
    public static final Icon API_ADD = load("/img/api/api_add.png");
    public static final Icon API_ADD_CHILD = load("/img/api/api_add_child.png");
    public static final Icon API_DELETE = load("/img/api/api_delete.png");
    public static final Icon API_EDIT = load("/img/api/api_edit.png");


    // ~ database
    //------------------------------------------------------------------------------------------------------------------
    public static final Icon COLLECT = load("/img/database/collect.svg");
    public static final Icon DB = load("/img/database/database.png");
    public static final Icon DB_BLOCK = load("/img/database/database_block.png");
    public static final Icon DB_BLOCK2 = load("/img/database/database_block2.png");
    public static final Icon DB_LINE = load("/img/database/database_line.png");
    public static final Icon DB_SELECT = load("/img/database/database_select.png");
    public static final Icon ORG = load("/img/database/org.png");
    public static final Icon USERS = load("/img/database/users.png");
    public static final Icon ORG_SELECT = load("/img/database/org_select.png");
    public static final Icon TABLE = load("/img/database/table.png");
    public static final Icon TABLE2 = load("/img/database/table2.png");
    public static final Icon TABLE_SELECT = load("/img/database/table_select.png");
    public static final Icon PRIMARY_KEY = load("/img/database/primary_key.png");
    public static final Icon FOLDER = load("/img/database/folder.png");
    public static final Icon COLUMN = load("/img/database/column.png");
    public static final Icon TIMESTAMP = load("/img/database/timestamp.svg");
    public static final Icon TABLE_LINE = load("/img/database/table_line.png");
    public static final Icon TABLE_ADD = load("/img/database/table_add.png");
    public static final Icon TABLE_EDIT = load("/img/database/table_edit.png");
    public static final Icon TABLE_REMOVE = load("/img/database/table_remove.png");
    public static final Icon TABLE_GEN_CODE = load("/img/database/table_gen_code.png");
    public static final Icon TABLE_CLEAN = load("/img/database/clean.png");
    public static final Icon SQL_EXECUTE = load("/img/database/sql_execute.png");
    public static final Icon SQL_EXECUTE_LIGHT = AllIcons.Providers.Mysql;
    public static final Icon SQL_CONSOLE = load("/img/database/sql_console.png");
    public static final Icon WARN = load("/img/database/warn.png");
    public static final Icon NEXT_PAGE = load("/img/database/next_page.png");
    public static final Icon PRE_PAGE = load("/img/database/pre_page.png");
    public static final Icon WATCH = load("/img/database/watch.png");
    public static final Icon COPY = load("/img/database/copy.svg");
    public static final Icon COPY_DOC = load("/img/database/copy_doc.svg");
    public static final Icon DDL = load("/img/database/ddl.svg");
    public static final Icon EXPORT_DDL = load("/img/database/ddl_file.svg");
    public static final Icon SORT = load("/img/database/sort.svg");
    public static final Icon SELECTED = load("/img/database/selected.svg");
    public static final Icon RELOAD = load("/img/database/reload.svg");
    public static final Icon ROLLBACK = load("/img/database/rollback.svg");
    public static final Icon SUB = load("/img/database/sub.svg");
    public static final Icon ROLLBACK2 = load("/img/database/rollback2.svg");

    // ~ inner
    //------------------------------------------------------------------------------------------------------------------

    public static final Icon FLASH = load("/img/jira/flash.svg");
    public static final Icon JARVIS = load("/img/inner/jarvis.svg");
    public static final Icon JARVIS2 = load("/img/inner/jarvis.png");
    public static final Icon LOGO_ROUND = load("/img/inner/logo_round32.svg");
    public static final Icon LOGO_ROUND13 = load("/img/inner/logo_round13.svg");
    public static final Icon LOGO_ROUND24 = load("/img/inner/logo_round24.svg");
    public static final Icon LOGO_128PX = load("/img/inner/logo_qihoo_128.svg");
    //public static final Icon LOGO_16PX = load("/img/inner/logo_16px.png");
    public static final Icon SEND = load("/img/inner/send.svg");
    public static final Icon SEND_DISABLED = load("/img/inner/send_disabled.svg");
    public static final Icon LOGIN_LOGO = load("/img/inner/login_logo.png");
    public static final Icon DRAGON = load("/img/window/dragon.png");
    public static final Icon API_GEN_CODE = load("/img/inner/api_gen.svg");
    public static final Icon DB_GEN = load("/img/inner/db_gen.svg");
    public static final Icon DB_GEN_LIGHT = load("/img/inner/db_gen_light.svg");
    public static final Icon API_GEN = load("/img/plugins/api_gen.png");
    public static final Icon API_GEN_LIGHT = load("/img/plugins/api_gen_light.png");
    public static final Icon ASK_AI = load("/img/inner/ask_ai.svg");
    public static final Icon ASK_AI_LIGHT = load("/img/inner/ask_ai_light.svg");

    public static final Icon EXECUTE_HISTORY = load("/img/action/execute_history.svg");
    public static final Icon AI_COPY = load("/img/action/ai_copy.svg");
    public static final Icon AI_DIFF = load("/img/action/diff.svg");
    public static final Icon AI_EXPAND = load("/img/action/expand.svg");
    public static final Icon AI_NEW_FILE = load("img/action/new_file.svg");
    public static final Icon DONE = load("/img/action/done.svg");
    public static final Icon INSERT_CODE = load("/img/action/insert_code.svg");
    public static final Icon TERMINATE = load("/img/action/terminate.svg");
    public static final Icon TERMINATE2 = load("/img/action/terminate2.svg");

    public static final Icon THUMBS_UP = load("/img/action/thumbs-up.svg");
    public static final Icon THUMBS_DOWN = load("/img/action/thumbs-down.svg");

    public static final Icon STATUS_BAR = load("/icons/status/chatx_3.svg");
    public static final Icon STATUS_BAR_DISABLED = load("/icons/status/chatx_disabled_3.svg");
    public static final Icon STATUS_BAR_COMPLETION_IN_PROGRESS = new AnimatedIcon.Default();
    public static final Icon DATASET = load("/img/action/dataset.svg");
    public static final Icon ASSISTANT2 = load("/img/action/assistant.svg");
    public static final Icon ASSISTANT = load("/img/action/assistant.png");
    public static final Icon CONVERSATION_NEW = load("/img/action/conversation_new.svg");
    public static final Icon CONVERSATION_HISTORY = load("/img/action/conversation_history.svg");
    public static final Icon DATASET2 = load("/img/action/dataset2.svg");
    public static final Icon DATASET3 = load("/img/action/dataset2.png");

    // ~ code review
    //------------------------------------------------------------------------------------------------------------------
    public static final Icon GIT_LAB_LIGHT = load("/img/codereview/gitlab_light.svg");
    public static final Icon GIT_LAB = load("/img/codereview/gitlab.svg");
    public static final Icon GIT_LAB2 = load("/img/codereview/gitlab2.svg");
    public static final Icon GIT_LAB3 = load("/img/codereview/gitlab3.svg");
    public static final Icon GIT_LAB4 = load("/img/codereview/gitlab4.svg");
    public static final Icon RELEASE = load("/img/codereview/release.svg");
    public static final Icon TASK = load("/img/codereview/task.svg");
    public static final Icon FINISH_CLOSE = load("/img/codereview/finish_close.svg");
    public static final Icon REOPEN = load("/img/codereview/reopen.svg");
    public static final Icon DATE = load("/img/codereview/date.svg");
    public static final Icon BRANCH = load("/img/codereview/branch.svg");
    public static final Icon COMMENT = load("/img/codereview/comment.svg");
    public static final Icon COMMENT_LIGHT = load("/img/codereview/comment_light.svg");
    public static final Icon LOCATION = load("/img/codereview/location.svg");

    // shortcut
    public static final Icon COMMAND = load("/img/shortcut/command.svg");
    public static final Icon ENTER = load("/img/shortcut/enter.svg");
    public static final Icon VK_1 = load("/img/shortcut/vk_1.svg");
    public static final Icon OPTION = load("/img/shortcut/option.svg");
    public static final Icon RIGHT = load("/img/shortcut/right.svg");
    public static final Icon TAB = load("/img/shortcut/tab.svg");
    public static final Icon CTRL = load("/img/shortcut/ctrl.svg");
    public static final Icon ALT = load("/img/shortcut/alt.svg");
    public static final Icon STARTS = load("/img/plugins/starts.svg");
    public static final Icon INDEX = load("/img/plugins/index.svg");
    public static final Icon QUESTION = load("/img/plugins/question.svg");
    public static final Icon CODING = load("/img/plugins/command.svg");
    public static final Icon KIT = load("/img/kit/kit.svg");
    public static final Icon ADD = load("/img/kit/add.svg");
    public static final Icon ENCODE = load("/img/kit/digest.svg");
    public static final Icon JSON = load("/img/kit/json.svg");
    public static final Icon COMPRESS = load("/img/kit/compress.svg");
    public static final Icon COMPRESS_TRANSFER = load("/img/kit/compress_transfer.svg");

    // ftd
    public static final Icon FTD = load("/img/action/ftd.svg");

    //------------------------------------------------------------------------------------------------------------------
    public static final Icon ACTION_COMMAND = load("/img/action/action_command.svg");
    public static final Icon ACTION_DATASET = load("/img/action/action_dataset.svg");
    public static final Icon ACTION_AT = load("/img/action/action_at.svg");
    public static final Icon INTERNET = load("/img/action/internet.svg");
    public static final Icon NO_INTERNET = load("/img/action/no_internet.svg");
    public static final Icon GIT = load("/img/action/git.svg");
    public static final Icon NO_GIT = load("/img/action/no_git.svg");
    public static final Icon BUILD_GIT_INDEX = load("/img/action/build_index.svg");
    public static final Icon UPLOAD = load("/img/action/upload.svg");
    public static final Icon MY_SQL_DARK = load("/img/action/mysql_dark.svg");

    //------------------------------------------------------------------------------------------------------------------
    public static final Icon CONSOLE_ICON = load("img/action/console_icon.svg");
    public static final Icon Locked = load("/img/plugins/locked.svg");
    public static final Icon User = load("/img/plugins/user.svg");
    public static final Icon OpenAI = load("/img/plugins/openai_dark.svg");
    public static final Icon Qwen = load("/img/plugins/qwen.png");
    public static final Icon InSelection = load("/img/plugins/inSelection.svg");
    public static final Icon ListFiles = load("/img/plugins/listFiles.svg");
    public static final Icon Anthropic = load("/img/plugins/anthropic.svg");
    public static final Icon Mistral = load("/img/plugins/mistral.svg");
    public static final Icon JARVIS_Model = load("/img/plugins/jarvis-model.svg");
    public static final Icon CollapseAll = load("/img/plugins/collapseAll.svg");
    public static final Icon ExpandAll = load("/img/plugins/expandAll.svg");
    public static final Icon SendToTheLeft = load("/img/plugins/sendToTheLeft.svg");
    public static final Icon GreenCheckmark = load("/img/plugins/greenCheckmark.svg");
    public static final Icon Sparkle = load("/img/plugins/sparkle.svg");
    public static final Icon AI_EDIT = load("/img/smartconversation/edit.svg");
    public static final Icon AI_EDIT_LIGHT = load("/img/smartconversation/edit-light.svg");
    public static final Icon AI_CREATE = load("/img/smartconversation/create.svg");
    public static final Icon AI_CREATE_LIGHT = load("/img/smartconversation/create-light.svg");
    public static final Icon SKILLS = load("/img/smartconversation/skills.svg");
    public static final Icon AI_READ = load("/img/smartconversation/read.svg");
    public static final Icon AI_READ_LIGHT = load("/img/smartconversation/read-light.svg");
    public static final Icon AI_FLODER = load("/img/smartconversation/floder.svg");
    public static final Icon AI_FLODER_LIGHT = load("/img/smartconversation/floder-light.svg");
    public static final Icon AI_SEARCH = load("/img/smartconversation/search.svg");
    public static final Icon AI_SEARCH_LIGHT = load("/img/smartconversation/search-light.svg");
    public static final Icon AI_COMMAND = load("/img/smartconversation/command.svg");
    public static final Icon AI_COMMAND_LIGHT = load("/img/smartconversation/command-light.svg");
    public static final Icon AI_GREP = load("/img/smartconversation/filter.svg");
    public static final Icon AI_TODO = load("/img/smartconversation/todo-list.svg");
    public static final Icon AI_TODO_LIGHT = load("/img/smartconversation/todo-list-light.svg");
    public static final Icon AI_OPEN = load("/img/smartconversation/open.svg");
    public static final Icon AI_AGENT = load("/img/smartconversation/agent.svg");
    public static final Icon AI_AGENT_LIGHT = load("/img/smartconversation/agent-light.svg");
    public static final Icon AI_HISTORY = load("/img/smartconversation/chat-history.svg");
    public static final Icon AI_HISTORY_LIGHT = load("/img/smartconversation/chat-history-light.svg");

    public static Icon load(String path) {
        Class<?> callerClass = ReflectionUtil.getGrandCallerClass();
        assert callerClass != null : path;
        return IconLoader.getIcon(path, callerClass);
    }

    private static ImageIcon loadResource(String sourcePath) {
        URL resource = com.qihoo.finance.lowcode.common.util.Icons.class.getResource(sourcePath);
        if (Objects.nonNull(resource)) {
            return new ImageIcon(resource);
        }

        throw new RuntimeException(String.format("file not exist: %s", sourcePath));
    }

    public static ImageIcon scale(ImageIcon icon, int width, int height, int hints) {
        Image scaledInstance = icon.getImage().getScaledInstance(width, height, hints);
        return new ImageIcon(scaledInstance);
    }

    public static ImageIcon scale(ImageIcon icon, int width, int height) {
        Image scaledInstance = icon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT);
        return new ImageIcon(scaledInstance);
    }

    public static ImageIcon scale(ImageIcon icon, int scale) {
        Image scaledInstance = icon.getImage().getScaledInstance(scale, scale, Image.SCALE_DEFAULT);
        return new ImageIcon(scaledInstance);
    }

    public static Icon scaleToWidth(Icon icon, float newWidth) {
        if (icon instanceof ScalableIcon scalableIcon) {
            int iconWidth = scalableIcon.getIconWidth();
            if (newWidth != iconWidth) {
                return scalableIcon.scale(newWidth / iconWidth);
            }
        }/* else {
            com.intellij.util.IconUtil.scale(icon, null, newWidth / icon.getIconWidth());
        }*/
        return icon;
    }

    @RequiresBackgroundThread
    public static Icon loadIconFromUrl(String iconUrl, Icon defaultIcon) {
        if (StringUtils.isEmpty(iconUrl)) return defaultIcon;
        try {
            Icon icon = IconLoader.findIcon(new URL(iconUrl), true);
            if (Objects.nonNull(icon)) {
                if (icon instanceof CachedImageIcon cachedIcon) {
                    cachedIcon.getIconWidth();
                    cachedIcon.getIconHeight();
                }
                return icon;
            }

        } catch (IOException e) {
            // 处理异常，例如记录日志或返回一个默认图标
            log.error("Failed to load icon from URL: {}", iconUrl, e);
        }
        return defaultIcon;
    }


    public static void asyncSetUrlIcon(JLabel component, String iconUrl, Icon defaultIcon, int newSize) {
        component.setIcon(scaleToWidth(LOADING_ANIMATED, newSize));
        new SwingWorker<Icon, Icon>() {
            @Override
            protected javax.swing.Icon doInBackground() {
                return UiUtil.loadIconFromUrl(iconUrl, newSize, 3600);
            }

            @Override
            protected void done() {
                try {
                    Icon icon = get();
                    if (icon != null) {
                        component.setIcon(icon);
                    } else {
                        component.setIcon(defaultIcon);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    // ignore
                }
                super.done();
            }
        }.execute();
    }

}
