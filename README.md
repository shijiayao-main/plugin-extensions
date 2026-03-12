# plugin-extensions

一个基于 Android Gradle Plugin 的字节码转换框架，核心代码修改自 [booster](https://github.com/didi/booster)，将常用功能整合到一起，并额外支持通过 SPI（Service Provider Interface）机制配置插件和替换 JAR 中的类。

## 项目结构

```
plugin-extensions/
├── core/                    # 核心模块，包含插件主体逻辑
├── core-extensions/         # 核心扩展模块，包含注解定义
├── plugin-demo/             # 示例插件模块
└── plugin-trace/            # 方法耗时统计插件模块
```

## 核心特性

- **字节码转换**：基于 ASM 实现的字节码转换能力
- **SPI 机制**：通过 `META-INF/services` 自动发现和加载插件组件
- **配置扩展**：支持自定义 Gradle DSL 配置
- **JAR 类替换**：支持替换第三方 JAR 包中的类
- **Variant 处理**：支持 Android Variant 级别的处理

## 快速开始

### 1. 引入插件

在项目的 `build.gradle.kts` 中应用插件：

```kotlin
plugins {
    id("com.jiaoay.plugins")
}
```

### 2. 配置插件

```kotlin
extensionsPlugin {
    isEnableSdkPatcher = true
    replaceClassMap = mutableMapOf(
        "some-library.jar" to mutableListOf(
            "com/example/TargetClass.class"
        )
    )
}
```

## 自定义 Transformer

### 1. 实现 Transformer 接口

创建一个实现 `Transformer` 接口的类：

```kotlin
class MyTransformer : Transformer {

    override fun onPreTransform(context: TransformContext) {
        // 转换前的准备工作
    }

    override fun transform(context: TransformContext, bytecode: ByteArray): ByteArray {
        // 使用 ASM 进行字节码转换
        val classReader = ClassReader(bytecode)
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        // ... 转换逻辑
        return classWriter.toByteArray()
    }

    override fun onPostTransform(context: TransformContext) {
        // 转换后的清理工作
    }
}
```

### 2. 注册 Transformer

在 `src/main/resources/META-INF/services/com.jiaoay.plugins.core.transform.Transformer` 文件中注册：

```
com.example.MyTransformer
```

## 自定义配置

### 1. 定义配置类

```kotlin
open class MyConfig {
    companion object {
        const val NAME = "myPlugin"

        fun get(project: Project): MyConfig {
            return project.extensions.getByName(NAME) as? MyConfig ?: MyConfig()
        }
    }

    var isEnable: Boolean = false
}
```

### 2. 实现 PluginConfig 接口

```kotlin
class MyConfigImpl : PluginConfig {
    override fun getConfigClass(): Class<*> = MyConfig::class.java
    override fun getConfigName(): String = MyConfig.NAME
}
```

### 3. 注册配置

在 `src/main/resources/META-INF/services/com.jiaoay.plugins.core.config.PluginConfig` 文件中注册：

```
com.example.MyConfigImpl
```

### 4. 使用配置

在 `build.gradle.kts` 中即可使用自定义 DSL：

```kotlin
myPlugin {
    isEnable = true
}
```

## 内置插件示例

### plugin-trace

方法耗时统计插件，用于在方法前后插入耗时统计代码。

配置示例：

```kotlin
pluginTrace {
    isEnable = true
    traceWhiteSet = mutableSetOf("com.example.app")  // 白名单，匹配的包名会被插桩
    traceBlackSet = mutableSetOf("com.example.app.excluded")  // 黑名单，匹配的包名会被过滤
}
```

在代码中使用 `@Trace` 注解可以标记需要统计耗时的类。

### plugin-demo

演示插件，展示如何实现一个基本的 Transformer。

## 核心注解

### @SdkPatcher

用于标记需要替换的类：

```kotlin
@SdkPatcher(name = "TargetClassName")
class MyPatchClass {
    // 替换目标类的实现
}
```

### @Trace

用于标记需要进行方法耗时统计的类：

```kotlin
@Trace
class MyActivity : Activity() {
    // 该类的方法会被自动插入耗时统计代码
}
```

## 技术依赖

- [Booster](https://github.com/didi/booster) - Android 应用优化框架
- [ASM](https://asm.ow2.io/) - Java 字节码操作框架
- Kotlin 2.3.10
- Android Gradle Plugin 9.1.0

## License

Apache License 2.0
