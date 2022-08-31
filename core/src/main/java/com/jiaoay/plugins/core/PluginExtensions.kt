package com.jiaoay.plugins.core

import com.android.SdkConstants
import com.android.build.api.transform.TransformInvocation
import com.jiaoay.plugins.core.asm.getValue
import com.jiaoay.plugins.core.config.ExtensionsPluginConfig
import java.io.File
import java.io.FileInputStream
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

private const val TAG = "ExtensionsPlugin"

internal var pluginProject: Project? = null

fun logger(message: String) {
    logger(level = LogLevel.ERROR, message = message)
}

fun logger(level: LogLevel, message: String) {
    pluginProject?.logger?.log(level, "$TAG: $message") ?: println("$TAG: $message")
}


val ClassReader.isQualifiedClass: Boolean
    get() {
        return !(access.and(Opcodes.ACC_INTERFACE) != 0 ||
                access.and(Opcodes.ACC_ABSTRACT) != 0 ||
                access.and(Opcodes.ACC_ENUM) != 0 ||
                access.and(Opcodes.ACC_ANNOTATION) != 0 ||
                access.and(Opcodes.ACC_MODULE) != 0 ||
                access.and(Opcodes.ACC_SYNTHETIC) != 0)
    }

/**
 * 获取需要替换的class
 */
internal fun autoSearchAnnotation(
    invocation: TransformInvocation,
    config: ExtensionsPluginConfig
) {
    // 不支持增量编译
    val outputProvider = invocation.outputProvider
    if (invocation.isIncremental.not()) {
        outputProvider.deleteAll()
    }
    invocation.inputs.forEach { input ->
        // 当前仅处理目录中的，如果之后jar中也有，再单独处理
        input.directoryInputs.forEach { directoryInput ->
            val file = directoryInput.file
            if (file.isDirectory.not()) {
                return
            }
            handleFile(inputFile = file, config = config)
        }
    }
}

private val replaceAnnotationType: Type by lazy {
    Type.getType(Replace::class.java)
}

private fun handleFile(
    inputFile: File, config: ExtensionsPluginConfig
) {
    val files: Array<File> = inputFile.listFiles() ?: arrayOf()
    for (file in files) {
        if (file.isDirectory) {
            handleFile(inputFile = file, config = config)
        } else if (file.isFile && file.name.endsWith(SdkConstants.DOT_CLASS, ignoreCase = true)) {
            val fileInputStream = FileInputStream(file)
            fileInputStream.use { inputStream ->
                try {
                    val classReader = ClassReader(inputStream)
                    if (classReader.isQualifiedClass.not()) {
                        return@use
                    }
                    val classNode = ClassNode()
                    classReader.accept(classNode, 0)
                    val className = classNode.name.plus(SdkConstants.DOT_CLASS)
                    classNode.invisibleAnnotations?.forEach { annotationNode ->
                        // TODO: 支持其他
                        annotationNode.checkSdkPatcherAnnotation(className = className, config = config)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    logger("error: file: ${file.name}")
                }
            }
        }
    }
}

private fun AnnotationNode.checkSdkPatcherAnnotation(
    className: String, config: ExtensionsPluginConfig
) {
    if (desc == replaceAnnotationType.descriptor) {
        val packageName = getValue<String>("name")?.replace(":", "-")

        logger("package name: $packageName, className: $className")
        if (packageName != null && packageName.isNotEmpty()) {
            config.replaceClassMap?.let { map ->
                map[packageName]?.let {
                    if (it.contains(className).not()) {
                        it.add(className)
                    }
                } ?: let {
                    map[packageName] = mutableListOf(className)
                }
            } ?: let {
                val replaceClassMap: MutableMap<String, MutableList<String>> = HashMap()
                replaceClassMap[packageName] = mutableListOf(className)
                config.replaceClassMap = replaceClassMap
            }
        }
    }
}
