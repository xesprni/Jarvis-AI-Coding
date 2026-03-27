
import org.gradle.plugins.ide.eclipse.model.Classpath
import org.gradle.plugins.ide.eclipse.model.Library
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")
    id("eclipse")
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("org.jetbrains.intellij.platform") version "2.9.0"
    id("org.jetbrains.changelog") version "2.0.0"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure project's dependencies
repositories {
    maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        create(properties("platformType"), properties("platformVersion"))
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("Git4Idea")
    }
    implementation("org.json:json:20240303")
    implementation("com.google.guava:guava:29.0-jre")
    implementation("org.dom4j:dom4j:2.1.4")
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("org.apache.poi:poi-ooxml-schemas:4.1.2")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.commons:commons-text:1.14.0")
    implementation("commons-lang:commons-lang:2.6")
    implementation("commons-io:commons-io:2.13.0")
    implementation("com.jcraft:jsch:0.1.55")
    implementation("com.fifesoft:rsyntaxtextarea:3.3.1")
    implementation("org.swinglabs:swingx:1.6.1")
    implementation("org.jetbrains:annotations:24.0.0")
    compileOnly("com.intellij:forms_rt:7.0.3")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    implementation("com.alibaba:fastjson:1.2.10")
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("ch.qos.logback:logback-classic:1.2.6")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.12.0.202106070339-r")
    implementation("org.mongodb:mongo-java-driver:3.10.2")
    implementation("com.github.vertical-blank:sql-formatter:2.0.4")
    implementation("com.vladsch.flexmark:flexmark:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-ext-tables:0.64.8")

    val langchain4jVersion = project.property("langchain4j.version") as String
    val langchain4jKotlinVersion = project.property("langchain4j-kotlin.version") as String
    implementation("dev.langchain4j:langchain4j-core:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-open-ai:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-kotlin:$langchain4jKotlinVersion") {
        exclude("org.jetbrains.kotlinx")
    }

    implementation("io.github.java-diff-utils:java-diff-utils:4.15")

    val ktor_version: String by project
    implementation("io.ktor:ktor-client-core:${ktor_version}") {
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
    }
    implementation("io.ktor:ktor-client-cio:${ktor_version}"){
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
    }
    implementation("io.ktor:ktor-client-content-negotiation:${ktor_version}"){
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
    }
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktor_version}"){
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0"){
        exclude("org.jetbrains.kotlinx", "kotlinx-serialization-json")
    }

    implementation("com.fasterxml.jackson:jackson-bom:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    implementation("org.yaml:snakeyaml:2.5")

    implementation("com.github.albfernandez:juniversalchardet:2.5.0")
    implementation("io.modelcontextprotocol:kotlin-sdk-client:0.7.4"){
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
    }
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.6")
    
    compileOnly("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    compileOnly("com.fasterxml.jackson.core:jackson-core:2.17.2")
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
    compileOnly("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
}

configurations.all {
    exclude("com.fasterxml.jackson.core")
    resolutionStrategy {
        force(
            "com.fasterxml.jackson.core:jackson-databind:2.17.2",
            "com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2",
            "com.fasterxml.jackson.core:jackson-core:2.17.2",
            "com.fasterxml.jackson.core:jackson-annotations:2.17.2"
        )
    }
}

changelog {
    groups.set(emptyList())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

val jvmSupport = properties("jvmSupport")
tasks {
    withType<JavaCompile> {
        sourceCompatibility = jvmSupport
        targetCompatibility = jvmSupport
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:-deprecation")
    }

    withType<Test> {
        testLogging {
            // 在控制台显示 "PASSED", "SKIPPED", "FAILED" 等事件
            events("passed", "skipped", "failed")
            // 将标准输出和标准错误流实时打印到控制台
            showStandardStreams = true
        }
    }
}

kotlin {
    jvmToolchain(jvmSupport.toInt())
    compilerOptions {
        val targetVersion = properties("jvmSupport")
        jvmTarget.set(JvmTarget.fromTarget(jvmSupport))
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

eclipse {
    classpath {
        plusConfigurations.add(configurations["intellijPlatformClasspath"])
        plusConfigurations.add(configurations["intellijPlatformBundledPlugins"])
        file {
            whenMerged {
                val classpath = this as Classpath
                val kotlinOutput = classpath.fileReference(file("build/classes/kotlin/main"))
                if (classpath.entries.none { it is Library && it.path == kotlinOutput.path }) {
                    classpath.entries.add(Library(kotlinOutput))
                }
            }
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        name = properties("pluginName")
        version = properties("pluginVersion")
        description =
            providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                with(it.lines()) {
                    if (!containsAll(listOf(start, end))) {
                        throw GradleException("Plugin description section not found in DESCRIPTION.md:\n$start ... $end")
                    }
                    subList(indexOf(start) + 1, indexOf(end)).joinToString("\n")
                        .let(::markdownToHTML)
                }
            }
        // Get the latest available change notes from the changelog file
        changeNotes = providers.fileContents(layout.projectDirectory.file("CHANGELOG.md")).asText.map {
            val start = "<!-- Plugin changelog -->"
            val end = "<!-- Plugin changelog end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    subList(0, 0)
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n")
                    .let(::markdownToHTML)
            }
        }

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    signing {
        certificateChain = System.getenv("CERTIFICATE_CHAIN")
        privateKey = System.getenv("PRIVATE_KEY")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = System.getenv("PUBLISH_TOKEN")
        channels = listOf("stable")
    }

    instrumentCode.set(false)
}
