package com.jiaoay.plugins.core.util

import com.didiglobal.booster.build.AndroidSdk
import com.didiglobal.booster.kotlinx.NCPU
import com.didiglobal.booster.kotlinx.file
import com.jiaoay.plugins.core.config.ExtensionsPluginConfig
import com.jiaoay.plugins.core.transform.AbstractTransformContext
import com.jiaoay.plugins.core.transform.ArtifactManager
import com.jiaoay.plugins.core.transform.TransformContext
import com.jiaoay.plugins.core.transform.Transformer
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val TMPDIR = File(System.getProperty("java.io.tmpdir"))

/**
 * Utility class for JAR or class file transforming
 *
 * @param input The files to transform
 * @param platform The specific android platform location, such as ${ANDROID_HOME}/platforms/android-28
 * @param artifacts The artifact manager
 * @param applicationId An identifier for transform output
 * @param variant The variant name
 *
 * @author johnsonlee
 */
open class TransformHelper(
    val input: File,
    val platform: File = AndroidSdk.getAndroidJar().parentFile,
    val artifacts: ArtifactManager = object : ArtifactManager {},
    val applicationId: String = UUID.randomUUID().toString(),
    val variant: String = "debug",
    val config: ExtensionsPluginConfig
) {

    fun transform(output: File = TMPDIR, transformer: (TransformContext, ByteArray) -> ByteArray = { _, it -> it }) = transform(output, object : Transformer {
        override fun transform(context: TransformContext, bytecode: ByteArray) = transformer(context, bytecode)
    })

    fun transform(transformer: (TransformContext, ByteArray) -> ByteArray = { _, it -> it }, output: File = TMPDIR) = transform(output, transformer)

    fun transform(output: File = TMPDIR, vararg transformers: Transformer) {
        val inputs = if (this.input.isDirectory) this.input.listFiles()?.toList() ?: emptyList() else listOf(this.input)
        val classpath = inputs.filter {
            it.isDirectory || it.extension.run {
                equals("class", true) || equals("jar", true)
            }
        }
        val context = object : AbstractTransformContext(
            applicationId,
            variant,
            platform.resolve("android.jar").takeIf { it.exists() }?.let { listOf(it) } ?: emptyList(),
            classpath,
            classpath
        ) {
            override val projectDir = output
            override val artifacts = this@TransformHelper.artifacts
        }
        val executor = Executors.newFixedThreadPool(NCPU)

        try {
            transformers.map {
                executor.submit {
                    it.onPreTransform(context)
                }
            }.forEach {
                it.get()
            }

            inputs.map { input ->
                executor.submit {
                    input.takeIf {
                        it.collect(CompositeCollector(context.collectors)).isNotEmpty()
                    }
                }
            }.forEach {
                it.get()
            }

            inputs.map {
                executor.submit {
                    it.transform(
                        output = context.buildDir.file("transforms", it.name),
                        config = config
                    ) { bytecode ->
                        transformers.fold(bytecode) { bytes, transformer ->
                            transformer.transform(context, bytes)
                        }
                    }
                }
            }.forEach {
                it.get()
            }

            transformers.map {
                executor.submit {
                    it.onPostTransform(context)
                }
            }.forEach {
                it.get()
            }
        } finally {
            executor.shutdown()
            executor.awaitTermination(1L, TimeUnit.HOURS)
        }
    }

    fun transform(vararg transformers: Transformer, output: File = TMPDIR) = transform(output, *transformers)

}
