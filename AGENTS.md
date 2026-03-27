# AGENTS.md

## 📦 项目描述

本项目是一个 IntelliJ 插件，用于提供 AI 编程能力，支持以下两种核心功能：

- ✨ **智能代码补全**（自动提示代码）
- 💬 **Agent 模式开发**：通过工具面板与 AI 聊天，对代码进行生成、修改与调试。

旧代码使用 Java 编写，但推荐在新开发中使用 **Kotlin**。本项目已部分迁移至 Kotlin。

## 项目规范
- 所有LOG不得打印ERROR级别（会导致用户弹窗）

---

## 🔧 项目结构说明

```text
src/
├── main/
│   ├── java/              # 历史 Java 代码（建议迁移）
│   └── kotlin/            # 推荐使用的 Kotlin 代码目录（主入口）
│       └── com.miracle/      # 项目主包路径
│           ├── agent/     # AI Agent 逻辑相关（如补全、聊天、上下文分析等）
│           ├── config/    # 插件配置项（如 plugin.xml、注册项、常量等）
│           ├── external/  # 外部依赖接口封装（如 API 请求、服务调用）
│           ├── ui/        # 插件 UI 组件（如 ToolPanel、Popup、通知等）
│           └── utils/     # 通用工具类（字符串处理、文件处理、日志等）
│
├── test/
│   └── kotlin/            # 单元测试代码（建议包路径与主代码保持一致）
│       └── com.miracle/...
```

---

## ⚙️ 构建与运行
- 插件项目使用 Gradle 进行构建。
- 使用 IntelliJ Platform Plugin SDK。
- 编译指令：
```bash
./gradlew build -x test
```
- 构建插件命令：
```bash
./gradlew buildPlugin
```
- 本地运行（会启动一个沙箱版本的IDE）：
```bash
./gradlew runIde
```

---

## 单元测试说明
- 测试代码位于：src/test/kotlin
- 测试框架：kotlin.test.*（标准库）
- 测试命令（Gradle）：
```bash
# 执行所有单元测试
./gradlew test

# 执行某个类
./gradlew test --tests com.miracle.utils.FileUtilsTest
```

### 协程测试建议（使用 kotlinx-coroutines-test）
如需测试 suspend 函数或使用协程的逻辑，推荐使用如下方式：
```kotlin
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import io.mockk.*

class SomeTest {

    @Test
    fun testSomething() = runTest {
        // mock 单例对象
        mockkObject(SomeSingleton)

        every { SomeSingleton.getToken() } returns "fake-token"

        val result = SomeService.getSomething()
        assertTrue(result.contains("expected"))
    }
}
```
- 使用 runTest {} 来执行协程代码。
- 使用 mockkObject 来模拟 Kotlin 单例对象（object）。
- 搭配 every { ... } returns ... 控制返回值。