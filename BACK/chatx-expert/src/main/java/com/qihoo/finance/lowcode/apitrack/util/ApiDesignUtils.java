package com.qihoo.finance.lowcode.apitrack.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qihoo.finance.lowcode.apitrack.entity.*;
import com.qihoo.finance.lowcode.apitrack.entity.ai.AiApiNode;
import com.qihoo.finance.lowcode.apitrack.entity.ai.AiMenuNode;
import com.qihoo.finance.lowcode.apitrack.entity.ai.AiProjectNode;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.interfacegen.InterfaceGenRequest;
import com.qihoo.finance.lowcode.common.entity.dto.interfacegen.InterfaceGenResult;
import com.qihoo.finance.lowcode.common.entity.dto.yapi.*;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * App接口请求内部工具类
 *
 * @author fengjinfu-jk
 * date 2023/8/7
 * @version 1.0.0
 * @apiNote LowCodeAppService
 */
public class ApiDesignUtils extends LowCodeAppUtils {
    private static final TypeReference<Result<List<YapiProjectDTO>>> API_PRJ_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Result<List<YapiProjectGroupDTO>>> API_GROUP_PRJ_LIST = new TypeReference<>() {
    };

    private static final TypeReference<Result<List<CategoryDetailDTO>>> API_CATEGORY_LIST = new TypeReference<>() {
    };

    private static final TypeReference<Result<List<InterfaceDTO>>> API_INTERFACE_LIST = new TypeReference<>() {
    };

    private static final TypeReference<Result<InterfaceDetailDTO>> API_INTERFACE_DETAIL = new TypeReference<>() {
    };

    private static final TypeReference<Result<InterfaceDetailDTO>> API_INTERFACE_ADD = new TypeReference<>() {
    };

    private static final TypeReference<Result<Object>> API_INTERFACE_UPDATE = new TypeReference<>() {
    };

    private static final TypeReference<Result<Object>> API_INTERFACE_DELETE = new TypeReference<>() {
    };

    private static final TypeReference<Result<Object>> API_CATEGORY_ADD = new TypeReference<>() {
    };

    private static final TypeReference<Result<Object>> API_CATEGORY_UPDATE = new TypeReference<>() {
    };

    private static final TypeReference<Result<Object>> API_CATEGORY_DELETE = new TypeReference<>() {
    };

    private static final TypeReference<Result<InterfaceGenResult>> API_INTERFACE_GENERATE = new TypeReference<>() {
    };

    public static List<YapiProjectDTO> apiPrjList() {
        Map<String, Object> param = new HashMap<>();
        String url = Constants.Url.GET_API_PRJ_LIST;
        Result<List<YapiProjectDTO>> result = catchException(url, () -> RestTemplateUtil.get(url, param, APPLICATION_JSON_HEADERS, API_PRJ_LIST), "接口设计项目列表请求失败" + ADD_NOTIFY, false);

        return resultData(result);
    }

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static List<AiProjectNode> yapiProjects(String branch) {
        String url = Constants.Url.YAPI_PROJECTS;
        Map<String, Object> query = new HashMap<>();
        query.put("branch", branch);
        Result<List<AiProjectNode>> result = catchException(
            url,
            () -> RestTemplateUtil.get(url, query, APPLICATION_JSON_HEADERS, new TypeReference<Result<List<AiProjectNode>>>() {}),
            "获取接口列表失败" + ADD_NOTIFY,
            false
        );
        Optional.ofNullable(result.getData()).orElseGet(List::of)
                .forEach(project -> {
                    String name = project.getName();
                    name = name.replaceFirst("_", "/");
                    name = name.replaceFirst("feature_", "feature/");
                    name = name.replaceFirst("release_", "release/");
                    name = name.replaceFirst("hotfix_", "hotfix/");
                    project.setName(name);
                    LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(project.getAddTime()), ZoneId.systemDefault());
                    String addTimeStr = dateTime.format(formatter);
                    int index = name.indexOf("/");
                    if (index > 0) {
                        project.setGroupName(name.substring(0, index));
                        project.setName(name.substring(index + 1));
                        project.setToolTipText(String.format("分组: %s<br/>生成时间: %s", project.getGroupName(), addTimeStr));
                    } else {
                        project.setToolTipText(String.format("生成时间: %s", addTimeStr));
                    }
                    if (StringUtils.isNotBlank(project.getDesc())) {
                        project.setToolTipText(project.getToolTipText() + "<br/>" + project.getDesc());
                    }
                });
        return result.getData();
    }

    public static List<AiMenuNode> yapiMenus(Long projectId) {
        String url = Constants.Url.YAPI_MENUS;
        Map<String, Object> query = new HashMap<>();
        query.put("projectId", projectId);
        Result<List<AiMenuNode>> result = catchException(
            url,
            () -> RestTemplateUtil.get(url, query, APPLICATION_JSON_HEADERS, new TypeReference<Result<List<AiMenuNode>>>() {}),
            "获取接口菜单列表失败" + ADD_NOTIFY,
            false
        );
        List<AiMenuNode> data = result.getData();
        Optional.ofNullable(data).ifPresent(x -> x.forEach(item -> {
            List<AiApiNode> apiNodes = item.getList();
            Optional.ofNullable(apiNodes).ifPresent(y -> y.forEach(item::add));
        }));
        return result.getData();
    }

    public static InterfaceDetailDTO yapiInterfaceDetail(Long interfaceId) {
        Map<String, Object> query = new HashMap<>();
        query.put("interfaceId", interfaceId);

        String url = Constants.Url.YAPI_INTERFACES;
        Result<InterfaceDetailDTO> result = catchException(
            url,
            () -> RestTemplateUtil.get(url, query, APPLICATION_JSON_HEADERS, new TypeReference<Result<InterfaceDetailDTO>>() {}),
            "获取接口详情失败" + ADD_NOTIFY,
            false
        );
        return result.getData();
    }

    public static List<ApplicationNode> apiPrjNodeList() {
        return convert(apiPrjList());
    }

    private static List<ApplicationNode> convert(List<YapiProjectDTO> prjList) {
        List<ApplicationNode> applicationNodes = new ArrayList<>();
        for (YapiProjectDTO prj : prjList) {
            ApplicationNode node = new ApplicationNode();
            node.setProjectName(prj.getProjectName());

            node.setToken(prj.getToken());
            node.setProjectId(prj.getProjectId());
            node.setAppCode(prj.getAppCode());

            node.setCode(String.valueOf(prj.getProjectId()));
            node.setName(prj.getProjectName());
            node.setEditable(prj.isEditable());

            applicationNodes.add(node);
        }

        return applicationNodes;
    }

    public static List<YapiProjectGroupDTO> apiGroupPrjList() {
        Map<String, Object> param = new HashMap<>();
        String url = Constants.Url.GET_API_GROUP_PRJ_LIST;
        Result<List<YapiProjectGroupDTO>> result = catchException(url, () -> RestTemplateUtil.get(url, param, APPLICATION_JSON_HEADERS, API_GROUP_PRJ_LIST), "接口设计用户权限组项目列表请求失败" + ADD_NOTIFY, false);

        return resultData(result);
    }

    public static List<ApiDepartmentNode> apiGroupPrjNodeList() {
        List<YapiProjectGroupDTO> depGroups = apiGroupPrjList();

        List<ApiDepartmentNode> groups = new ArrayList<>();
        for (YapiProjectGroupDTO depGroup : depGroups) {
            ApiDepartmentNode departmentNode = new ApiDepartmentNode();
            groups.add(departmentNode);

            departmentNode.setCode(depGroup.getCode());
            departmentNode.setName(depGroup.getName());

            List<ApplicationNode> applicationNodes = convert(depGroup.getProjects());
            applicationNodes.forEach(departmentNode::add);
        }
        return groups;
    }

    public static List<CategoryDetailDTO> apiCategoryList(String appCode, String projectId) {
        Map<String, Object> param = new HashMap<>();
        param.put("appCode", appCode);
        param.put("projectId", projectId);

        String url = Constants.Url.GET_API_CATEGORY_LIST;
        Result<List<CategoryDetailDTO>> result = catchException(url, () -> RestTemplateUtil.get(url, param, APPLICATION_JSON_HEADERS, API_CATEGORY_LIST), "接口设计项目分类列表请求失败" + ADD_NOTIFY);

        return resultData(result);
    }

    public static List<ApiGroupNode> apiCategoryList(ApplicationNode applicationNode) {
        List<CategoryDetailDTO> categoryList = apiCategoryList(applicationNode.getAppCode(), String.valueOf(applicationNode.getProjectId()));
        List<ApiGroupNode> nodes = new ArrayList<>();
        for (CategoryDetailDTO category : categoryList) {
            ApiGroupNode node = new ApiGroupNode();
            nodes.add(node);

            node.setId(String.valueOf(category.getId()));
            node.setClassName(category.getClassName());
            node.setProjectId(category.getProjectId());
            node.setProjectToken(applicationNode.getToken());

            node.setName(category.getName());
            node.setCode(String.valueOf(applicationNode.getCode()));
            node.setAppCode(applicationNode.getAppCode());

            node.setClassDesc(category.getClassDesc());
            node.setAutoGenPackage(category.getAutoGenPackage());
            node.setExtensionPackage(category.getExtensionPackage());
            node.setEditable(category.isEditable());

            node.setApplicationNode(applicationNode);
        }

        return nodes;
    }

    public static List<InterfaceDTO> apiInterfaceList(String appCode, String projectId, String categoryId) {
        Map<String, Object> param = new HashMap<>();
        param.put("appCode", appCode);
        param.put("projectId", projectId);
        param.put("categoryId", categoryId);

        String url = Constants.Url.GET_API_INTERFACE_LIST;
        Result<List<InterfaceDTO>> result = catchException(url, () -> RestTemplateUtil.get(url, param, APPLICATION_JSON_HEADERS, API_INTERFACE_LIST), "接口设计分类接口列表请求失败" + ADD_NOTIFY, false);

        return resultData(result);
    }

    public static List<ApiNode> apiInterfaceList(ApiGroupNode groupNode) {
        List<InterfaceDTO> apiInterfaceList = apiInterfaceList(groupNode.getAppCode(), String.valueOf(groupNode.getProjectId()), groupNode.getId());
        List<ApiNode> nodes = new ArrayList<>();
        for (InterfaceDTO api : apiInterfaceList) {
            ApiNode node = new ApiNode();
            nodes.add(node);

            node.setId(api.getId());
            node.setTitle(api.getTitle());
            node.setCategoryId(api.getCategoryId());
            node.setProjectToken(groupNode.getProjectToken());

            node.setMethodName(api.getMethodName());
            node.setMethod(RequestMethod.valueOf(api.getMethod().name()));
            node.setName(api.getTitle());
            node.setCode(String.valueOf(api.getId()));
            node.setUrl(api.getPath());
            node.setEditable(api.isEditable());

            node.setApiGroupNode(groupNode);
        }

        return nodes;
    }

    public static InterfaceDetailDTO apiInterfaceDetail(ApiNode apiNode) {
        Map<String, Object> param = new HashMap<>(getProjectInfo(apiNode));
        param.put("id", String.valueOf(apiNode.getId()));

        String url = Constants.Url.GET_API_INTERFACE_DETAIL;
        Result<InterfaceDetailDTO> result = catchException(url, () -> RestTemplateUtil.get(url, param, APPLICATION_JSON_HEADERS, API_INTERFACE_DETAIL), "接口设计分类接口详情请求失败" + ADD_NOTIFY);

        return resultData(result);
    }

    public static Result<InterfaceDetailDTO> apiInterfaceAdd(InterfaceAddDTO apiNode) {
        String url = Constants.Url.POST_API_INTERFACE_ADD;
        return catchException(url, () -> RestTemplateUtil.post(url, apiNode, APPLICATION_JSON_HEADERS, API_INTERFACE_ADD), "新增接口请求失败" + ADD_NOTIFY + "入参: " + JSON.toJson(apiNode), false);
    }

    public static Result<Object> apiInterfaceUpdate(InterfaceUpdateDTO apiNode) {
        String url = Constants.Url.POST_API_INTERFACE_UPDATE;
        return catchException(url, () -> RestTemplateUtil.post(url, apiNode, APPLICATION_JSON_HEADERS, API_INTERFACE_UPDATE), "编辑接口请求失败" + ADD_NOTIFY + "入参: " + JSON.toJson(apiNode), false);
    }

    public static Result<Object> apiInterfaceDelete(ApiNode apiNode) {
        Map<String, Object> param = new HashMap<>(getProjectInfo(apiNode));
        param.put("id", String.valueOf(apiNode.getId()));

        String url = Constants.Url.POST_API_INTERFACE_DELETE;
        return catchException(url, () -> RestTemplateUtil.post(url, param, APPLICATION_JSON_HEADERS, API_INTERFACE_DELETE), "删除接口请求失败" + ADD_NOTIFY, false);
    }

    public static Result<InterfaceGenResult> apiInterfaceGenerate(InterfaceGenRequest request) {
        String url = Constants.Url.POST_API_INTERFACE_GENERATE;
        return catchException(url, () -> RestTemplateUtil.post(url, request, APPLICATION_JSON_HEADERS, API_INTERFACE_GENERATE), "接口生成代码失败" + ADD_NOTIFY, false);
    }


    public static Result<Object> apiCategoryAdd(ApplicationNode applicationNode, ApiGroupNode groupNode) {
        Map<String, Object> param = new HashMap<>();
        param.put("appCode", applicationNode.getAppCode());
        param.put("projectId", String.valueOf(groupNode.getProjectId()));

        param.put("name", groupNode.getName());
        param.put("className", groupNode.getClassName());
        param.put("classDesc", groupNode.getClassDesc());

        String url = Constants.Url.POST_API_CATEGORY_ADD;
        return catchException(url, () -> RestTemplateUtil.post(url, param, APPLICATION_JSON_HEADERS, API_CATEGORY_ADD), "添加接口分类请求失败" + ADD_NOTIFY, false);
    }

    public static Result<Object> apiCategoryUpdate(ApplicationNode applicationNode, ApiGroupNode groupNode) {
        Map<String, Object> param = new HashMap<>();
        param.put("appCode", applicationNode.getAppCode());
        param.put("projectId", String.valueOf(groupNode.getProjectId()));

        param.put("id", groupNode.getId());
        param.put("name", groupNode.getName());
        param.put("className", groupNode.getClassName());
        param.put("autoGenPackage", groupNode.getAutoGenPackage());
        param.put("extensionPackage", groupNode.getExtensionPackage());
        param.put("classDesc", groupNode.getClassDesc());

        String url = Constants.Url.POST_API_CATEGORY_UPDATE;
        return catchException(url, () -> RestTemplateUtil.post(url, param, APPLICATION_JSON_HEADERS, API_CATEGORY_UPDATE), "编辑接口分类请求失败" + ADD_NOTIFY, false);
    }

    public static Result<Object> apiCategoryDelete(ApiGroupNode groupNode) {
        Map<String, Object> param = new HashMap<>();
        param.put("id", groupNode.getId());

        ApplicationNode applicationNode = groupNode.getApplicationNode();
        if (Objects.nonNull(applicationNode)) {
            param.put("appCode", applicationNode.getAppCode());
            param.put("projectId", String.valueOf(applicationNode.getProjectId()));
        }

        String url = Constants.Url.POST_API_CATEGORY_DELETE;
        return catchException(url, () -> RestTemplateUtil.post(url, param, APPLICATION_JSON_HEADERS, API_CATEGORY_DELETE), "删除接口分类请求失败" + ADD_NOTIFY, false);
    }

    // ~ project token
    //------------------------------------------------------------------------------------------------------------------

    private static Map<String, Object> getProjectInfo(ApiNode apiNode) {
        Map<String, Object> param = new HashMap<>();

        // token
        ApiGroupNode apiGroupNode = apiNode.getApiGroupNode();
        if (Objects.nonNull(apiGroupNode)) {
            ApplicationNode applicationNode = apiGroupNode.getApplicationNode();
            if (Objects.nonNull(applicationNode)) {
                param.put("appCode", applicationNode.getAppCode());
                param.put("projectId", String.valueOf(applicationNode.getProjectId()));
            }
        }

        return param;
    }
}
