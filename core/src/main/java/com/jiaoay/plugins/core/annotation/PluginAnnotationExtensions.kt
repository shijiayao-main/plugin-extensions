package com.jiaoay.plugins.core.annotation

import com.android.SdkConstants
import com.jiaoay.plugins.core.config.ExtensionsPluginConfig
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.FileInputStream

private const val TAG = "ExtensionsPlugin"

internal var pluginProject: Project? = null

fun logger(message: String) {
    logger(tag = TAG, level = LogLevel.ERROR, message = message)
}

fun logger(tag: String, message: String) {
    logger(tag = tag, level = LogLevel.ERROR, message = message)
}

fun logger(tag: String, level: LogLevel, message: String) {
    pluginProject?.logger?.log(level, "$tag: $message") ?: println("$tag: $message")
}

val ClassReader.isQualifiedClass: Boolean
    get() {
        return access.isQualified
    }

val Int.isQualified: Boolean
    get() {
        val access = this
        return (
            access.and(Opcodes.ACC_INTERFACE) != 0 ||
                access.and(Opcodes.ACC_ABSTRACT) != 0 ||
                access.and(Opcodes.ACC_ENUM) != 0 ||
                access.and(Opcodes.ACC_ANNOTATION) != 0 ||
                access.and(Opcodes.ACC_MODULE) != 0 ||
                access.and(Opcodes.ACC_SYNTHETIC) != 0
            ).not()
    }

/**
 * 提前将file遍历一遍, 找出需要的annotation
 */
internal fun autoSearchAnnotation(
    annotations: Collection<PluginAnnotation>,
    compileClasspath: Collection<File>,
    config: ExtensionsPluginConfig,
) {
    if (annotations.isEmpty()) {
        return
    }

    compileClasspath.forEach { file ->
        if (file.isDirectory.not()) {
            return@forEach
        }
        handleFile(
            inputFile = file,
            config = config,
            annotations = annotations,
        )
    }
}

private fun handleFile(
    inputFile: File,
    config: ExtensionsPluginConfig,
    annotations: Collection<PluginAnnotation>,
) {
    val files: Array<File> = inputFile.listFiles() ?: arrayOf()
    for (file in files) {
        if (file.isDirectory) {
            handleFile(
                inputFile = file,
                config = config,
                annotations = annotations,
            )
        } else if (file.isFile && file.name.endsWith(
                suffix = SdkConstants.DOT_CLASS,
                ignoreCase = true,
            )
        ) {
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
                        if (annotationNode == null) {
                            return@forEach
                        }
                        annotations.forEach {
                            it.checkAnnotationNode(
                                className = className,
                                classNode = classNode,
                                annotationNode = annotationNode,
                            )
                        }
                        return@forEach
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    logger("error: file: ${file.name}")
                }
            }
        }
    }
}
