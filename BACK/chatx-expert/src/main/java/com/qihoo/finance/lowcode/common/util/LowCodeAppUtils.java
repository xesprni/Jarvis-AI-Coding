package com.qihoo.finance.lowcode.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.qihoo.finance.lowcode.common.configuration.UserContextPersistent;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.constants.ServiceErrorCode;
import com.qihoo.finance.lowcode.common.entity.CustomResult;
import com.qihoo.finance.lowcode.common.entity.FileUpload;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.AppBaseInfo;
import com.qihoo.finance.lowcode.common.entity.dto.LoginResultDTO;
import com.qihoo.finance.lowcode.common.entity.dto.plugins.PluginConfig;
import com.qihoo.finance.lowcode.common.exception.ServiceException;
import com.qihoo.finance.lowcode.common.oauth2.dto.OAuth2ResponseDTO;
import com.qihoo.finance.lowcode.common.oauth2.dto.OAuth2UserInfoDTO;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateFileCodeDTO;
import com.qihoo.finance.lowcode.gentracker.entity.ColumnConfigGroup;
import com.qihoo.finance.lowcode.gentracker.entity.GlobalConfigGroup;
import com.qihoo.finance.lowcode.gentracker.entity.TemplateGroup;
import com.qihoo.finance.lowcode.gentracker.entity.TypeMapperGroup;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * App接口请求内部工具类
 *
 * @author fengjinfu-jk
 * date 2023/8/7
 * @version 1.0.0
 * @apiNote LowCodeAppService
 */
@Slf4j
public class LowCodeAppUtils {
    public static final String ADD_NOTIFY = ", 请确保使用公司内网或登录VPN";
    protected static final Map<String, String> APPLICATION_JSON_HEADERS = new HashMap<>() {{
        put("Content-Type", "application/json");
    }};

    public static UserInfoPersistentState.UserInfo getUserInfo() {
        UserInfoPersistentState state = UserInfoPersistentState.getInstance();
        return state != null ? state.getState() : new UserInfoPersistentState.UserInfo();
    }

    public static UserContextPersistent.UserContext getUserContext() {
        UserContextPersistent context = UserContextPersistent.getInstance();
        return context != null ? context.getState() : new UserContextPersistent.UserContext();
    }


    // ~ load source type
    private static final TypeReference<Result<Map<String, TemplateGroup>>> TEMPLATE_GROUP_MAP = new TypeReference<>() {
    };
    private static final TypeReference<Result<Map<String, TypeMapperGroup>>> TYPE_MAPPER_MAP = new TypeReference<>() {
    };
    private static final TypeReference<Result<Map<String, ColumnConfigGroup>>> COLUMN_CONFIG_MAP = new TypeReference<>() {
    };
    private static final TypeReference<Result<Map<String, GlobalConfigGroup>>> GLOBAL_CONFIG_MAP = new TypeReference<>() {
    };

    // ~ user login
    private static final TypeReference<Result<Object>> SEND_VERIFY_CODE = new TypeReference<>() {
    };
    private static final TypeReference<Result<LoginResultDTO>> LOGIN_VERIFY_CODE = new TypeReference<>() {
    };

    // ~ project info
    //------------------------------------------------------------------------------------------------------------------
    private static final TypeReference<Result<AppBaseInfo>> APPLICATION_PATH = new TypeReference<>() {
    };
    static final TypeReference<Result<GenerateFileCodeDTO>> FILE_CODE = new TypeReference<>() {
    };
    static final TypeReference<Result<PluginConfig>> PLUGIN_CONFIG = new TypeReference<>() {
    };
    protected static final TypeReference<Result<String>> STRING = new TypeReference<>() {
    };
    protected static final TypeReference<Result<Object>> OBJECT = new TypeReference<>() {
    };
    protected static final TypeReference<Result<FileUpload>> FILE_UPLOAD = new TypeReference<>() {
    };

    // ~ project info
    //------------------------------------------------------------------------------------------------------------------

    public static boolean isLogin() {
        return StringUtils.isNotEmpty(getUserInfo().getUserNo());
    }

    public static boolean isLastFocusProject(Project project) {
        IdeFrame lastFocusedFrame = IdeFocusManager.getGlobalInstance().getLastFocusedFrame();
        if (Objects.isNull(lastFocusedFrame)) return false;
        Project focusProject = lastFocusedFrame.getProject();
        return !Objects.isNull(focusProject) && focusProject.equals(project);
    }

    public static boolean isFocusingProject(Project project) {
        return isLastFocusProject(project) && isFocusIde();
    }

    public static boolean isFocusIde() {
        return IdeFocusManager.getGlobalInstance().isFocusTransferEnabled();
    }

    public static PluginConfig getPluginConfig() {
        return getPluginConfig(true);
    }

    public static PluginConfig getPluginConfig(boolean useCache) {
        String url = Constants.Url.GET_PLUGIN_CONFIG;
        String cacheKey = "@getPluginConfigs";
        if (useCache) {
            Result<PluginConfig> cache = InnerCacheUtils.getCache(cacheKey, PLUGIN_CONFIG);
            if (Objects.nonNull(cache)) {
                return resultData(cache);
            }
        }

        Result<PluginConfig> result = catchException(url, () -> RestTemplateUtil.get(url, new HashMap<>(), new HashMap<>(), PLUGIN_CONFIG), "获取插件全局配置信息失败" + ADD_NOTIFY, false);
        if (result.isSuccess() && Objects.nonNull(result.getData()))
            InnerCacheUtils.setCache(cacheKey, JSON.toJson(result), 60 * 30);
        return resultData(result, new PluginConfig());
    }

    public static AppBaseInfo queryApplicationCode(String project, String module) {
        String url = Constants.Url.GET_MODULE_APPLICATION_CODE + "/" + module;
        String cacheKey = "@AppBaseInfo_" + module;
        Result<AppBaseInfo> cache = InnerCacheUtils.getCache(cacheKey, APPLICATION_PATH);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        Result<AppBaseInfo> result = catchException(url, () -> RestTemplateUtil.get(url, new HashMap<>(), new HashMap<>(), APPLICATION_PATH), "获取应用信息失败" + ADD_NOTIFY, false);
        if (result.isSuccess() && Objects.nonNull(result.getData()))
            InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));
        return resultData(result, new AppBaseInfo());
    }

    // ~ user login
    //------------------------------------------------------------------------------------------------------------------

    public static Boolean sendLoginVerifyCode(String email) {
        Map<String, String> param = new HashMap<>();
        param.put("email", email);
        param.put("mac", RestTemplateUtil.getLocalMac());

        String url = Constants.Url.POST_SEND_VERIFICATION_CODE;
        Result<Object> result = catchException(url, () -> RestTemplateUtil.post(url, param, new HashMap<>(), SEND_VERIFY_CODE), true);

        return result.isSuccess();
    }

    public static String loginWithVerifyCode(String email, String verificationCode) {
        Map<String, String> param = new HashMap<>();
        param.put("email", email);
        param.put("verificationCode", verificationCode);
        param.put("mac", RestTemplateUtil.getLocalMac());

        String url = Constants.Url.POST_SEND_LOGIN;
        Result<LoginResultDTO> result = catchException(url, () -> RestTemplateUtil.post(url, param, new HashMap<>(), LOGIN_VERIFY_CODE), "登录请求失败" + ADD_NOTIFY);

        return result.isSuccess() && Objects.nonNull(result.getData()) ? result.getData().getToken() : null;
    }

    // ~ resource
    //------------------------------------------------------------------------------------------------------------------
    public static Map<String, TemplateGroup> queryTemplateGroup() {
        return queryTemplateGroup(null);
    }

    public static Map<String, TemplateGroup> queryTemplateGroup(String datasource) {
        HashMap<String, Object> param = new HashMap<>();
        if (StringUtils.isNotEmpty(datasource)) {
            param.put("datasource", datasource);
        }

        TypeReference<Result<Map<String, TemplateGroup>>> returnType = TEMPLATE_GROUP_MAP;
        String cacheKey = "@queryTemplateGroup_" + returnType.getType().getTypeName() + JSON.toJson(param);
        Result<Map<String, TemplateGroup>> cache = InnerCacheUtils.getCache(cacheKey, returnType);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        String url = Constants.Url.GET_LOAD_TEMPLATE;
        Result<Map<String, TemplateGroup>> result = catchException(url, () -> RestTemplateUtil.get(url, param, new HashMap<>(), returnType), "初始化模板信息失败" + ADD_NOTIFY);

        if (result.isSuccess()) InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));
        return resultData(result);
    }

    public static Map<String, TypeMapperGroup> queryTypeMapperGroupMap() {
        TypeReference<Result<Map<String, TypeMapperGroup>>> returnType = TYPE_MAPPER_MAP;
        String cacheKey = returnType.getType().getTypeName();
        Result<Map<String, TypeMapperGroup>> cache = InnerCacheUtils.getCache(cacheKey, returnType);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        String url = Constants.Url.GET_LOAD_TYPE_MAPPER;
        Result<Map<String, TypeMapperGroup>> result = catchException(url, () -> RestTemplateUtil.get(url, new HashMap<>(), new HashMap<>(), returnType), "初始化类型映射信息失败" + ADD_NOTIFY);

        if (result.isSuccess()) InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));
        return resultData(result);
    }

    public static Map<String, ColumnConfigGroup> queryColumnConfigGroup() {
        TypeReference<Result<Map<String, ColumnConfigGroup>>> returnType = COLUMN_CONFIG_MAP;
        String cacheKey = returnType.getType().getTypeName();
        Result<Map<String, ColumnConfigGroup>> cache = InnerCacheUtils.getCache(cacheKey, returnType);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        String url = Constants.Url.GET_LOAD_COLUMNS_CONFIG;
        Result<Map<String, ColumnConfigGroup>> result = catchException(url, () -> RestTemplateUtil.get(url, new HashMap<>(), new HashMap<>(), returnType), "初始化字段映射信息失败" + ADD_NOTIFY);

        if (result.isSuccess()) InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));
        return resultData(result);
    }

    public static Map<String, GlobalConfigGroup> queryGlobalConfigGroup() {
        TypeReference<Result<Map<String, GlobalConfigGroup>>> returnType = GLOBAL_CONFIG_MAP;
        String cacheKey = returnType.getType().getTypeName();
        Result<Map<String, GlobalConfigGroup>> cache = InnerCacheUtils.getCache(cacheKey, returnType);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        String url = Constants.Url.GET_LOAD_GLOBAL_CONFIG;
        Result<Map<String, GlobalConfigGroup>> result = catchException(url, () -> RestTemplateUtil.get(url, new HashMap<>(), new HashMap<>(), returnType), "初始化配置信息失败" + ADD_NOTIFY);
        return resultData(result);
    }

    // ~ readOly listener
    //------------------------------------------------------------------------------------------------------------------

    public static GenerateFileCodeDTO getEventFileNewestCode(VirtualFile file) {
        String projectName = ProjectUtils.getCurrProjectName();
        GenerateFileCodeDTO fileCodeDTO = new GenerateFileCodeDTO();
        fileCodeDTO.setFileName(file.getName());
        fileCodeDTO.setProject(projectName);

        String canonicalPath = file.getCanonicalPath();
        if (StringUtils.isNotBlank(canonicalPath)) {
            String[] projectPath = canonicalPath.split(projectName);
            String projectDirPath;
            if (projectPath.length > 1) {
                projectDirPath = StringUtils.remove(projectPath[1], ("/" + file.getName()));
            } else {
                projectDirPath = StringUtils.remove(canonicalPath, ("/" + file.getName()));
            }

            fileCodeDTO.setSavePath(projectName + projectDirPath);
        }

        String cacheKey = "@GenerateFileCodeDTO_" + JSON.toJson(fileCodeDTO);
        String url = Constants.Url.POST_QUERY_FILE_CODE;
        TypeReference<Result<GenerateFileCodeDTO>> returnType = FILE_CODE;

        Result<GenerateFileCodeDTO> cache = InnerCacheUtils.getCache(cacheKey, returnType);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        Result<GenerateFileCodeDTO> result = catchException(url, () -> RestTemplateUtil.post(url, fileCodeDTO, new HashMap<>(), returnType), false);
        if (result.isSuccess() && Objects.nonNull(result.getData()))
            InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));

        return resultData(result);
    }

    public static String queryAgreementStatement() {
        String cacheKey = "@LowCodeAppUtils_queryAgreementStatement" + STRING.getType().getTypeName();
        Result<String> cache = InnerCacheUtils.getCache(cacheKey, STRING);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        String url = Constants.Url.GET_AGREEMENT_STATEMENT_TIPS;
        Result<String> result = catchException(url, () -> RestTemplateUtil.get(url, new HashMap<>(), new HashMap<>(), STRING), "查询重要声明失败" + ADD_NOTIFY, false);
        if (result.isSuccess()) InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));

        return resultData(result, "<html><body>"
                + "<h1 style='font-size: 16px;'>重要声明</h1>"
                + "<p style='margin-left: 10px;'>使用大模型进行代码补全时，我们需要获取你的代码上下文信息以完成补全，但上下文信息不会被存储或用于其他任何目的，该等数据完全由你所有及控制。</p>"
                + "<p style='margin-left: 10px;'>使用大模型生成的所有内容均由人工智能模型生成，其生成内容的准确性和完整性无法保证，不代表我们的态度或观点。</p>"
                + "</body></html>"
        );
    }

    public static FileUpload uploadFile(File file) {
        String url = Constants.Url.POST_FILE_UPLOAD;
        Result<FileUpload> fileUploadResult = catchException(url, () -> {
                    try {
                        return RestTemplateUtil.uploadFile(url, new HashMap<>(), new FileInputStream(file), file.getName(), FILE_UPLOAD);
                    } catch (FileNotFoundException e) {
                        log.error("uploadFile error", e);
                        return null;
                    }
                }
        );

        FileUpload data = fileUploadResult.getData();
        if (Objects.nonNull(data)) {
            data.setFullPath(file.getPath());
            data.setName(file.getName());
        }
        return data;
    }

    public static FileUpload uploadFile(VirtualFile file) {
        String url = Constants.Url.POST_FILE_UPLOAD;
        Result<FileUpload> fileUploadResult = catchException(url, () -> {
                    try {
                        return RestTemplateUtil.uploadFile(url, new HashMap<>(), file.getInputStream(), file.getName(), FILE_UPLOAD);
                    } catch (IOException e) {
                        log.error("uploadFile error", e);
                        return null;
                    }
                }
        );

        FileUpload data = fileUploadResult.getData();
        if (Objects.nonNull(data)) {
            data.setFullPath(file.getPath());
            data.setName(file.getName());
        }
        return data;
    }

    // ~ inner
    //------------------------------------------------------------------------------------------------------------------

    protected static <T> T resultData(Result<T> result, T defaultData) {
        if (!result.isSuccess()) {
            log.warn("request result is failure, return default result, original result: {}, default result: {}", JSON.toJson(result), JSON.toJson(defaultData));
            return defaultData;
        }

        return ObjectUtils.defaultIfNull(result.getData(), defaultData);
    }

    protected static <T> T resultData(Result<T> result) {
        return result.getData();
    }

    protected static <R extends Result<T>, T> R catchException(String url, Supplier<R> supplier) {
        return handleException(url, supplier, (e, r) -> log.warn("{} fail, errMsg: {}", url, e.getMessage()), null, true);
    }

    protected static <R extends Result<T>, T> R catchException(String url, Supplier<R> supplier, boolean notifyErr) {
        return handleException(url, supplier, (e, r) -> log.warn("{} fail, errMsg: {}", url, e.getMessage()), null, notifyErr);
    }

    protected static <R extends Result<T>, T> R catchException(String url, Supplier<R> supplier, String notifyIfFail) {
        return handleException(url, supplier, (e, r) -> log.warn("{} fail, errMsg: {}", url, e.getMessage()), notifyIfFail, true);
    }

    protected static <R extends Result<T>, T> R catchException(String url, Supplier<R> supplier, String notifyIfFail, boolean notifyErr) {
        return handleException(url, supplier, (e, r) -> log.warn("{} fail, errMsg: {}", url, e.getMessage()), notifyIfFail, notifyErr);
    }

    @SuppressWarnings("all")
    protected static <R extends Result<T>, T> R handleException(String url, Supplier<R> supplier, BiConsumer<Exception, Result<T>> handler, String notifyIfFail, boolean notifyErr) {
        long time1 = System.currentTimeMillis();
        R result;
        try {
            result = supplier.get();
        } catch (Exception e) {
            result = (R) new CustomResult<>();
            result.setSuccess(false);
            result.setErrorMsg(e.getMessage());
            result.setErrorCode((e instanceof ServiceException) ? ((ServiceException) e).getErrorCode() : "500");
            log.error("LowCodeAppUtils request fail, url: {}, errMsg: {}", url, e.getMessage());

            if (Objects.nonNull(handler)) {
                handler.accept(e, result);
            }
        }

        // 结果处理
        ResultHelper.handleResult(result, url, notifyIfFail, notifyErr);
        // 耗时统计
        statisticsElapsedTime(url, time1, System.currentTimeMillis());

        return result;
    }

    private static void statisticsElapsedTime(String url, long time1, long time2) {
        long time = time2 - time1;
        if (time > 1000) {
            log.warn("接口 {} 耗时 {}ms", url, time);
        } else {
            log.debug("接口 {} 耗时 {}ms", url, time);
        }
    }

    public static String getErrMsg(Result<?> result) {
        ServiceErrorCode errorCode = ServiceErrorCode.getByCode(result.getErrorCode());
        return Objects.nonNull(errorCode) ? errorCode.getMessage() : result.getErrorMsg();
    }

    public static String setUrlPath(String url, String path, String pathValue) {
        return StringUtils.replace(url, String.format("{%s}", path), pathValue);
    }


    public static OAuth2ResponseDTO loginWithOAuth2(String url, Map<String, String> param, Map<String, String> headers) {
        try {
            String result = RestTemplateUtil.postForm(url, param, headers, 10000);
            return JSON.parse(result, OAuth2ResponseDTO.class);
        }
        catch (Exception e) {
            log.error("loginWithOAuth2 error:{}", e.getMessage(), e);
            if (e instanceof ServiceException) {
                throw new ServiceException(((ServiceException) e).getErrorCode(), e.getMessage());
            }
            throw new ServiceException("500", e.getMessage() + ADD_NOTIFY);
        }
    }

    public static OAuth2UserInfoDTO getUserInfoWithOAuth2(String url, Map<String,Object> param, Map<String, String> headers) {
        TypeReference<Result<OAuth2UserInfoDTO>> oauth2TypeReference = new TypeReference<>() {
        };
        Result<OAuth2UserInfoDTO> result = catchException(url, () -> RestTemplateUtil.get(url, param, headers, oauth2TypeReference), "获取用户信息失败" + ADD_NOTIFY);
        return result.isSuccess() && Objects.nonNull(result.getData()) ? result.getData() : null;
    }
}
