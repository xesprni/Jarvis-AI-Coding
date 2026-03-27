package com.qihoo.finance.lowcode.common.constants;

import com.google.common.collect.Lists;
import com.intellij.ui.JBColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 全局常量配置
 *
 * @author fengjinfu-jk
 * date 2023/8/7
 * @version 1.0.0
 * @apiNote Constants
 */
public interface Constants {

    String PLUGIN_ID = "com.qihoo.finance.lowcode.chatx-expert";
    String PLUGIN_SLOGAN = "让软件研发更高效、更可靠、更智能";
    String DEFAULT_ASSISTANT_NAME = "JARVIS";
    String PLUGIN_TOOL_WINDOW_ID = "Jarvis";
    String DEFAULT_ASSISTANT = "BASIC_ASSISTANT";
    String ALL_DATASET = "#ALL";

    static boolean isDebugMode() {
        String devMode = System.getenv("DEV_MODE");
        return "true".equalsIgnoreCase(devMode);
    }

    // fixme: URL后续需提取到配置文件或从远程配置中获取
    interface Url {

        static String getHost() {
            String devMode = System.getenv("DEV_MODE");
            if ("true".equalsIgnoreCase(devMode)) {
//                return "http://127.0.0.1:8080";
                return "http://10.236.24.114";
            }
            return "http://lowcode-app.daikuan.qihoo.net";
        }

        static String getRagHost() {
            if (isDebugMode()) {
//                return "http://127.0.0.1:8080";
                return "http://git-rag-dev.daikuan.qihoo.net";
            }
            return "http://git-rag.daikuan.qihoo.net";
        }

        static String getLowcodeDomainHost(){
            if (isDebugMode()) {
                return "http://10.236.25.63";
            }
            return "https://lowcode.daikuan.qihoo.net";
        }

        String FROM = "/from-idea";

        String USER = "https://lingxi.daikuan.qihoo.net/";
        String TOOL_WEB = "https://lowcode.daikuan.qihoo.net/";
        String TOOL_HELP = "https://docs.daikuan.qihoo.net/lowcode/";

        // time track
        String TIME_TRACK = "http://lingxi-tracker.daikuan.qihoo.net/openapi/v1/develop";

        // generate track
        String POST_ADD_RECORD = getHost() + "/record/add";
        String POST_ADD_FILE_CODE = getHost() + "/record/add-file";
        String POST_QUERY_FILE_CODE = getHost() + "/record/query-file";
        String GET_DB_GENERATE_OTHER_SETTING = getHost() + "/plugin-config/db-generate-other-setting";
        String GET_PLUGIN_CONFIG = getHost() + "/plugin-config/configs";

        // user database
        String GET_QUERY_PERMISSION_TREE = getHost() + FROM + "/metadata/{datasourceType}/permission-tree";
        String GET_QUERY_SQL_HISTORY = getHost() + FROM + "/metadata/{datasourceType}/sql-history";
        String POST_DELETE_SQL_HISTORY = getHost() + FROM + "/metadata/{datasourceType}/delete-sql-history";
        String POST_SAVE_SQL_HISTORY = getHost() + FROM + "/metadata/{datasourceType}/save-sql-history";
        String GET_QUERY_PERMISSION_TABLES = getHost() + FROM + "/metadata/{datasourceType}/tables";
        String GET_QUERY_TABLE_COLUMNS = getHost() + FROM + "/metadata/mysql/columns";
        String GET_QUERY_TABLE_INDEXES = getHost() + FROM + "/metadata/mysql/indexes";

        // load source
        String GET_LOAD_TEMPLATE = getHost() + "/resources/templates";
        String GET_LOAD_TYPE_MAPPER = getHost() + "/resources/type-mapper";
        String GET_LOAD_COLUMNS_CONFIG = getHost() + "/resources/columns-config";
        String GET_LOAD_GLOBAL_CONFIG = getHost() + "/resources/global-config";
        // application info
        String GET_MODULE_APPLICATION_CODE = getHost() + FROM + "/app-base-info/module";

        // database design
        String GET_DDL_RECORD = getHost() + FROM + "/metadata/mysql/tables/change-record";
        String POST_EXECUTE_SQL = getHost() + FROM + "/metadata/mysql/execute";
        String POST_BATCH_EXECUTE_CONSOLE_SQL = getHost() + FROM + "/metadata/{datasourceType}/batch/check-and-execute";
        String POST_EXECUTE_CONSOLE_SQL = getHost() + FROM + "/metadata/mysql/check-and-execute";
        String POST_VALIDATE_SQL = getHost() + FROM + "/metadata/mysql/batch/ddl-check";
        String GET_CREATE_TABLE_DDL = getHost() + FROM + "/metadata/mysql/tables/create-ddl";
        String POST_SAVE_TABLE_GEN_CODE_LOG = getHost() + FROM + "/lowcode/addTableGenCodeLog";

        String POST_CREATE_MONGO_COLLECTION = getHost() + FROM + "/metadata/{datasourceType}/create-collection";
        String POST_DROP_MONGO_COLLECTION = getHost() + FROM + "/metadata/{datasourceType}/drop-collection";
        String GET_MONGO_COLLECTION_PAGE = getHost() + FROM + "/metadata/{datasourceType}/collection-page";
        String GET_MONGO_COLLECTION_BSON = getHost() + FROM + "/metadata/{datasourceType}/collection-query-bson";
        String POST_MONGO_FIND_DOCUMENTS = getHost() + FROM + "/metadata/{datasourceType}/find-documents";
        String POST_MONGO_FIND_DOCUMENT = getHost() + FROM + "/metadata/{datasourceType}/find-document";
        String POST_MONGO_DELETE_DOCUMENT = getHost() + FROM + "/metadata/{datasourceType}/delete-document";
        String POST_MONGO_UPDATE_DOCUMENT = getHost() + FROM + "/metadata/{datasourceType}/update-document";

        // login
        String POST_SEND_VERIFICATION_CODE = getHost() + FROM + "/send-verification-code";
        String POST_SEND_LOGIN = getHost() + FROM + "/login";
        String GET_AGREEMENT_STATEMENT_TIPS = getHost() + "/plugin-config/import-agreement-statement";

        // chat
        String CHAT_SYNC_SEND_MSG = getHost() + FROM + "/chat/send";
        String CHAT_SEND_MSG = getHost() + FROM + "/chat/conversation";
        String CHAT_SEND_MSG_V2 = getHost() + FROM + "/chat/conversationV2";
        String CHAT_SEND_MSG_V3 = getHost() + FROM + "/chat/conversationV3";
        String CHAT_HELLO_TEXT = getHost() + FROM + "/chat/helloText";
        String CHAT_FEEDBACK = getHost() + FROM + "/chat/feedback";
        String CHAT_CHAT_COMPLETION_POST = getHost() + FROM + "/chat/chatCompletion";
        String CHAT_CODE_COMPLETION = getHost() + FROM + "/chat/codeCompletion";
        String CHAT_CODE_COMPLETION_V2 = getHost() + FROM + "/chat/codeCompletionV2";
        String CHAT_CODE_COMPLETION_LOG = getHost() + FROM + "/chat/codeCompletionLog";
        String CHAT_UPDATE_CODE_COMPLETION_LOG_STATUS = getHost() + FROM + "/chat/updateCodeCompletionLogStatus";
        String CHAT_GET_MESSAGE_SUGGESTED = getHost() + FROM + "/chat/message-suggested";
        String CHAT_GET_DATASETS = getHost() + FROM + "/chat/datasets";
        String CHAT_GET_CONVERSATIONS = getHost() + FROM + "/chat/conversations";
        String CHAT_GET_CONVERSATION_MESSAGES = getHost() + FROM + "/chat/conversation-messages";
        String CHAT_POST_CONVERSATION_DELETE = getHost() + FROM + "/chat/delete-conversation";
        String CHAT_POST_MESSAGE_FEEDBACKS = getHost() + FROM + "/chat/message-feedbacks";
        String CHAT_GET_LIST_DATASET = getHost() + FROM + "/chat/list-dataset";
        String CHAT_GET_LIST_ASSISTANT = getHost() + FROM + "/chat/list-assistant";
        String CHAT_GET_LIST_INSTRUCTION = getHost() + FROM + "/chat/list-instruction";

        // API Design
        String GET_API_PRJ_LIST = getHost() + FROM + "/lowcode/listProject";
        String GET_API_GROUP_PRJ_LIST = getHost() + FROM + "/lowcode/listProjectWithGroup";
        String GET_API_CATEGORY_LIST = getHost() + FROM + "/lowcode/listCategory";
        String GET_API_INTERFACE_LIST = getHost() + FROM + "/lowcode/listInterfaceByCategory";
        String GET_API_INTERFACE_DETAIL = getHost() + FROM + "/lowcode/interfaceDetail";
        String POST_API_INTERFACE_ADD = getHost() + FROM + "/lowcode/addInterface";
        String POST_API_INTERFACE_UPDATE = getHost() + FROM + "/lowcode/updateInterface";
        String POST_API_INTERFACE_DELETE = getHost() + FROM + "/lowcode/deleteInterface";
        String POST_API_INTERFACE_GENERATE = getHost() + FROM + "/lowcode/generateInterface";
        String POST_API_CATEGORY_ADD = getHost() + FROM + "/lowcode/addCategory";
        String POST_API_CATEGORY_UPDATE = getHost() + FROM + "/lowcode/updateCategory";
        String POST_API_CATEGORY_DELETE = getHost() + FROM + "/lowcode/deleteCategory";

        String YAPI_PROJECTS = getHost() + FROM + "/yapi/projects";
        String YAPI_MENUS = getHost() + FROM + "/yapi/menus";
        String YAPI_INTERFACES = getHost() + FROM + "/yapi/interfaces";

        // jira commit
        String GET_COMMIT_TIPS = getHost() + "/plugin-config/commit-jira-tips";
        String GET_PIPELINE_AUTHORIZATION = getHost() + "/plugin-config/pipeline-authorization";
        String GET_USER_PIPELINE_AUTHORIZATION = getHost() + "/plugin-config/user-pipeline-config";
        String POST_PIPELINE_CONFIG = "http://lingxi-server.daikuan.qihoo.net:8080/openapi/v1/pipeline/configName";
        String GET_USER_ACTIVE_JIRA_ISSUE = getHost() + FROM + "/jira/issues";
        String GET_USER_ACTIVE_JIRA_ISSUE_DETAIL = getHost() + FROM + "/jira/issue-detail";
        String GET_USER_ISSUE_TRANSITION_STATUS = getHost() + FROM + "/jira/transition-status";
        String POST_DEVELOP_TIME_COUNT = getHost() + FROM + "/develop-time-count";
        String POST_READ_WRITE_COUNT = getHost() + FROM + "/dev-inspect/event/batch-save";

        // code review
        String GET_CRV_VERIFY_TOKEN = getHost() + FROM + "/code-review/verify-token";
        String GET_CRV_PERMISSION_TREE = getHost() + FROM + "/code-review/permission-tree";
        String GET_CRV_REVIEW_TASKS = getHost() + FROM + "/code-review/review-tasks";
        String POST_CRV_REVIEW_TASK_ADD = getHost() + FROM + "/code-review/review-task/add";
        String POST_CRV_REVIEW_TASK_UPDATE = getHost() + FROM + "/code-review/review-task/update";
        String POST_CRV_REVIEW_TASK_DEL = getHost() + FROM + "/code-review/review-task/del";
        String POST_CRV_REVIEW_TASK_FINISH = getHost() + FROM + "/code-review/review-task/finish";
        String POST_CRV_REVIEW_TASK_REOPEN = getHost() + FROM + "/code-review/review-task/reopen";
        String POST_CRV_TEMP_BRANCH_DEL = getHost() + FROM + "/code-review/review-task/temp-branches/del";
        String GET_CRV_REVIEW_TASK_DISCUSSIONS = getHost() + FROM + "/code-review/review-task/discussions";
        String GET_CRV_REVIEW_TASK_COMMENTS = getHost() + FROM + "/code-review/review-task/comments";
        String GET_CRV_EMPLOYEES = getHost() + FROM + "/code-review/employees";
        String GET_CRV_REMOTE_BRANCHES = getHost() + FROM + "/code-review/remote-branches";
        String GET_CRV_COMMITS = getHost() + FROM + "/code-review/commits";
        String GET_CRV_TIPS = getHost() + "/plugin-config/code-review/tips";
        String GET_CRV_TOKEN_TIPS = getHost() + "/plugin-config/code-review/token-tips";

        String GET_DDL_EXPORT_PATH = getHost() + "/plugin-config/ddl-export-path";
        String POST_CRV_DISCUSSION_MARK = getHost() + FROM + "/code-review/review-task/discussion/mark";

        // generate track
        String GET_QUERY_GENERATE_OPTIONS = getHost() + FROM + "/generate-options/query-generate-options";
        String POST_SAVE_GENERATE_OPTIONS = getHost() + FROM + "/generate-options/save-generate-options";

        // generate example
        String POST_GENERATE_EXAMPLE = getHost() + "/generate/generate-example";
        String GET_GENERATE_EXT_FILE_SUFFIX = getHost() + "/generate/ext-file-suffix";
        // generate json
        String POST_GENERATE_JSON_ENTITY = getHost() + "/generate/json-entity";

        // agent task
        String GET_USER_RECENT_AGENT_TASK = getHost() + FROM + "/agent-task/user-recent-task";
        String GET_USER_AGENT_TASK_SIGNAL = getHost() + FROM + "/agent-task/user-task-signal";
        String GET_CLOSE_USER_AGENT_TASK = getHost() + FROM + "/agent-task/close-agent-task";
        String GET_CLOSE_ALL_USER_AGENT_TASK = getHost() + FROM + "/agent-task/close-all-agent-task";

        // declarative
        String POST_DECLARATIVE_ANALYZE_DIFF = getHost() + FROM + "/declarative-sql/analyze-diff";
        String POST_DECLARATIVE_ANALYZE_DIFF_TABLE = getHost() + FROM + "/declarative-sql/analyze-diff-table";

        // git index
        String POST_BUILD_INDEX = getRagHost() + "/v1/api/repo/summary";
        String POST_INDEX_STATUS = getRagHost() + "/v1/api/repo/summary/status";

        // file upload
        String POST_FILE_UPLOAD = getHost() + FROM + "/minio/upload";

        String GET_JARVIS_MODEL = getHost() + "/jarvis/v2/models";
        String GET_MCP_MARKET_SERVERS = getHost() + "/mcp-market/mcp-servers/queryAll";
        String GET_MCP_SERVER_BY_ID = getHost() + "/mcp-market/mcp-servers/";

        // OAuth2 url
        String OAUTH2_AUTH = getLowcodeDomainHost() + "/?#/login/oauth";
        String OAUTH2_TOKEN_ENDPOINT = getHost() + "/oauth2/token";
        String OAUTH2_REDIRECT_URI = getHost() + "/login/oauth2/code";
        String OAUTH2_USERINFO = getHost() + "/openapi/v1/oauth2/userinfo";

        // trace url
        String CONVERSATION_TRACE_URL = getHost() + "/jarvis/trace/conversation";
        String MESSAGE_TRACE_URL = getHost() + "/jarvis/trace/message";
        String BUSINESS_TRACE_URL = getHost() + "/jarvis/trace/business";
        String AGENT_CONFIG_TRACE_URL = getHost() + "/jarvis/agent/saveConfig";
        String AGENT_CONFIG_FILE_TRACE_URL = getHost() + "/jarvis/agent/saveConfigFile";
        String AGENT_CONFIG_FILE_CLEAR_URL = getHost() + "/jarvis/agent/clearOtherFile";
        String AGENT_CONVERSATION_ADD_URL = getHost() + "/jarvis/conversation/add";
    }

    interface ResponseCode {
        String TOKEN_INVALID = "401";
    }

    interface DataSource {
        String MySQL = "mysql";
        String Oracle = "oracle";
        String Postgresql = "postgresql";
        String MongoDB = "mongoDB";
    }

    interface Headers {
        String MAC = "X-Low-Code-Mac";
        String EMAIL = "X-Low-Code-Email";
        String TOKEN = "X-Low-Code-Token";
        String VERSION = "X-Low-Code-Version";
        String IDE_VERSION = "X-Low-Code-IDE-Version";
        String USERNAME = "X-Lingxi-Username";
        String GITLAB_TOKEN = "X-Gitlab-Token";
        String WITH_DATA_SET = "X-Low-Code-AI-WITH-DATASET";
        String WITH_DATA_SET_ID = "X-Low-Code-AI-WITH-DATASET_ID";
        String OPERATE_PATH = "X-Low-Code-OPERATE_PATH";
        String MODEL_ID = "X-Jarvis-Model-Id";
    }

//    interface Email {
//        String ADDRESS = "@360shuke.com";
//    }

    interface Package {
        String AUTOGEN = "autogen";

        String EXTENSION = "extension";

        String ROOT_PACKAGE = "tech.qifu.lowcode";

        /**
         * 部分无需用户选择的控件是否隐藏
         */
        boolean HIDDEN_OPTIONS = true;
    }

    interface DB_COLUMN {
        String DEFAULT_ZERO = "0";
        String DEFAULT_NULL = "";

        String EMPTY_STRING = "EMPTY STRING";

        String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

        String UPDATE_TIMESTAMP = "CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP";

        List<String> DEFAULT_VALUES = Lists.newArrayList(DEFAULT_ZERO, DEFAULT_NULL, EMPTY_STRING, CURRENT_TIMESTAMP, UPDATE_TIMESTAMP);
        Map<String, String> BASE_ENTITY = new HashMap<>() {{
            this.put("id", "bigint|int");
            this.put("date_created", "time|timestamp|datetime|date");
            this.put("created_by", "varchar");
            this.put("date_updated", "time|timestamp|datetime|date");
            this.put("updated_by", "varchar");

            // fixme: deleted_at 暂时不作为基础字段
//            this.put("deleted_at", "bigint|int");
        }};

        Map<String, String> MIN_BASE_ENTITY = new HashMap<>() {{
            this.put("id", "bigint|int");
            this.put("date_created", "time|timestamp|datetime|date");
            this.put("created_by", "varchar");
            this.put("date_updated", "time|timestamp|datetime|date");
            this.put("updated_by", "varchar");
        }};

        Map<String, String> DELETED_AT = new HashMap<>() {{
            this.put("deleted_at", "bigint");
        }};
    }

    interface Plugins {
        String PLUGIN_XML_URL = "http://artifacts.daikuan.qihoo.net/artifacts/public/plugins/chatx-expert/updatePlugins.xml";
    }

    interface Api {
        String[] FORM_FIELD_TYPE = new String[]{"text", "file"};
        String[] JSON_FIELD_TYPE = new String[]{"string", "number", "array", "object", "boolean", "integer"};
    }

    interface REGEX {
        String ENG_NUM_UNDER_LINE = "^[0-9a-zA-Z_]{1,}$";
        String BIT_REGEX = "^b'[0,1]+'$";
        Pattern BIT = Pattern.compile(BIT_REGEX);

        String ENG_NUM_UNDER_LINE_OR = "[a-z|A-Z0-9_$.]+";
        Pattern KEY_WORD = Pattern.compile(ENG_NUM_UNDER_LINE_OR);
    }

    interface Log {
        String USER_ACTION = "USER_ACTION: {}";
    }

    interface Color {
        JBColor USER_MESSAGE_BACKGROUND = new JBColor(new java.awt.Color(252, 253, 255), new java.awt.Color(52, 54, 58));
        JBColor PANEL_BACKGROUND = new JBColor(new java.awt.Color(247, 248, 250), new java.awt.Color(43, 45, 48));
        JBColor SPLIT_LINE_COLOR = new JBColor(new java.awt.Color(230, 238, 240), new java.awt.Color(30, 31, 34));
        JBColor TOOL_CONTENT_BACKGROUND = new JBColor(new java.awt.Color(245, 245, 245), new java.awt.Color(45, 45, 45));
    }

    interface Encrypt {
        String ENCRYPT = "Encryptx";
        String MD5X = "Md5x";
        String ENCRYPT_FIELD_SUFFIX = "_encryptx";
        String MD5X_FIELD_SUFFIX = "_md5x";
    }

    interface TemplateTag {
        List<String> SERVICE = Lists.newArrayList("service", "serviceImpl", "extendService", "extendServiceImpl");
        List<String> CONTROLLER = Lists.newArrayList("controller");
        List<String> FACADE = Lists.newArrayList("facade", "facadeService", "facadeServiceImpl", "facadeInput", "facadeOutput", "facadeProvider");
    }

    interface GenerateSetter {
        public static final String GENERATE_SETTER_METHOD = "「Jarvis」Generate all setter with default value";
        public static final String GENERATE_BUILDER_METHOD = "Generate builder chain call";
        public static final String GENERATE_ACCESSORS_METHOD = "Generate accessors chain call";
        public static final String GENERATE_SETTER_METHOD_NO_DEFAULT_VALUE = "「Jarvis」Generate all setter no default value";
        public static final String ASSERT_ALL_PROPS = "Assert all getters";
        public static final String ASSERT_NOT_NULL = "Assert is not null";
        public static final String GENERATE_CONVERTER_FROM_METHOD = "「Jarvis」Generate setter getter converter";
        public static final String BUILDER_CONVERTER_FROM_METHOD = "Generate builder getter converter";
        public static final String BUILDER_METHOD_NAME = "builder";
        public static final String GENERATE_GETTER_METHOD = "Generate all getter";

        public static final String GENERATE_SETTER_METHOD_NO_DEAULT_VALUE = "";

        public static final String IS = "is";
        public static final String GET = "get";
        public static final String SET_SETTER_PREFIX = "set";
        public static final String WITH_SETTER_PREFIX = "with";
        public static final String STATIC = "static";
    }

    interface Editor {
        String GENERATE_CODE_BY_COMMENT = "「Jarvis」注释生成代码";
    }

    interface BusinessConstant {
        String SOURCE = "JET_BRAIN_PLUGIN";
        List<String> STATUS = Lists.newArrayList("NORMAL", "ERROR", "CANCELLED");
        List<String> DATA_TYPE = Lists.newArrayList("USER_MESSAGE", "AI_MESSAGE", "TOOL_CALL_REQUEST", "TOOL_RESPONSE");

    }
}
