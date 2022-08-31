package com.jiaoay.plugins.core

//import com.android.dex.DexFormat
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status.NOTCHANGED
import com.android.build.api.transform.Status.REMOVED
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.BaseExtension
import com.didiglobal.booster.gradle.ResolvedArtifactResults
import com.didiglobal.booster.gradle.applicationId
import com.didiglobal.booster.gradle.bootClasspath
import com.didiglobal.booster.gradle.compileClasspath
import com.didiglobal.booster.gradle.extension
import com.didiglobal.booster.gradle.getAndroid
import com.didiglobal.booster.gradle.getProperty
import com.didiglobal.booster.gradle.isDataBindingEnabled
import com.didiglobal.booster.gradle.originalApplicationId
import com.didiglobal.booster.gradle.project
import com.didiglobal.booster.gradle.runtimeClasspath
import com.didiglobal.booster.gradle.variant
import com.didiglobal.booster.kotlinx.NCPU
import com.didiglobal.booster.kotlinx.file
import com.didiglobal.booster.kotlinx.green
import com.didiglobal.booster.kotlinx.red
import com.jiaoay.plugins.core.config.ExtensionsPluginConfig
import com.jiaoay.plugins.core.transform.AbstractKlassPool
import com.jiaoay.plugins.core.transform.ArtifactManager
import com.jiaoay.plugins.core.transform.Collector
import com.jiaoay.plugins.core.transform.TransformContext
import com.jiaoay.plugins.core.transform.Transformer
import com.jiaoay.plugins.core.transform.artifacts
import com.jiaoay.plugins.core.util.CompositeCollector
import com.jiaoay.plugins.core.util.collect
import com.jiaoay.plugins.core.util.dex
import com.jiaoay.plugins.core.util.transform
import java.io.File
import java.net.URI
import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import org.apache.commons.codec.digest.DigestUtils.md5Hex
import org.gradle.api.logging.LogLevel

internal class PluginTransformInvocation(
    private val delegate: TransformInvocation,
    private val transform: ExtensionsPluginTransform,
    private val config: ExtensionsPluginConfig
) : TransformInvocation by delegate, TransformContext, ArtifactManager {

    private val outputs = CopyOnWriteArrayList<File>()

    private val collectors = CopyOnWriteArrayList<Collector<*>>()

    /*
     * Preload transformers as List to fix NoSuchElementException caused by ServiceLoader in parallel mode
     */
    private val transformers: List<Transformer> = transform.parameter.transformers.map {
        try {
            it.getConstructor(ClassLoader::class.java).newInstance(transform.parameter.buildscript.classLoader)
        } catch (e1: Throwable) {
            try {
                it.getConstructor().newInstance()
            } catch (e2: Throwable) {
                throw e2.apply {
                    addSuppressed(e1)
                }
            }
        }
    }

    override val name: String = delegate.context.variantName

    override val projectDir: File = project.projectDir

    override val buildDir: File = project.buildDir

    override val temporaryDir: File = delegate.context.temporaryDir

    override val reportsDir: File = File(buildDir, "reports").also { it.mkdirs() }

    override val bootClasspath = delegate.bootClasspath

    override val compileClasspath = delegate.compileClasspath

    override val runtimeClasspath = delegate.runtimeClasspath

    override val artifacts = this

    override val dependencies: Collection<String> by lazy {
        ResolvedArtifactResults(variant).map {
            it.id.displayName
        }
    }

    private val bootKlassPool by lazy {
        object : AbstractKlassPool(project.getAndroid<BaseExtension>().bootClasspath) {}
    }

    override val klassPool by lazy {
        object : AbstractKlassPool(compileClasspath, bootKlassPool) {}
    }

    override val applicationId = delegate.applicationId

    override val originalApplicationId = delegate.originalApplicationId

    override val isDebuggable = variant.buildType.isDebuggable

    override val isDataBindingEnabled = delegate.isDataBindingEnabled

    override fun hasProperty(name: String) = project.hasProperty(name)

    override fun <T> getProperty(name: String, default: T): T = project.getProperty(name, default)

    override fun get(type: String) = variant.artifacts.get(type)

    override fun <R> registerCollector(collector: Collector<R>) {
        this.collectors += collector
    }

    override fun <R> unregisterCollector(collector: Collector<R>) {
        this.collectors -= collector
    }

    internal fun doFullTransform() = doTransform(this::transformFully)

    internal fun doIncrementalTransform() = doTransform(this::transformIncrementally)

    private fun lookAhead(executor: ExecutorService): Set<File> {
        return this.inputs.asSequence().map {
            it.jarInputs + it.directoryInputs
        }.flatten().map { input ->
            executor.submit(Callable {
                input.file.takeIf { file ->
                    file.collect(CompositeCollector(collectors)).isNotEmpty()
                }
            })
        }.mapNotNull {
            it.get()
        }.toSet()
    }

    private fun onPreTransform() {
        transformers.forEach {
            it.onPreTransform(this)
        }
    }

    private fun onPostTransform() {
        transformers.forEach {
            it.onPostTransform(this)
        }
    }

    private fun doTransform(block: (ExecutorService, Set<File>) -> Iterable<Future<*>>) {
        this.outputs.clear()
        this.collectors.clear()

        val executor = Executors.newFixedThreadPool(NCPU)

        this.onPreTransform()

        // Look ahead to determine which input need to be transformed even incremental build
        val outOfDate = this.lookAhead(executor).onEach {
            project.logger.info("✨ ${it.canonicalPath} OUT-OF-DATE ")
        }

        try {
            block(executor, outOfDate).forEach {
                it.get()
            }
        } finally {
            executor.shutdown()
            executor.awaitTermination(1, TimeUnit.HOURS)
        }

        this.onPostTransform()

        if (transform.verifyEnabled) {
            this.doVerify()
        }
    }

    private fun transformFully(
        executor: ExecutorService,
        @Suppress("UNUSED_PARAMETER")
        outOfDate: Set<File>
    ) = this.inputs.map {
        it.jarInputs + it.directoryInputs
    }.flatten().map { input ->
        executor.submit {
            val format = if (input is DirectoryInput) {
                Format.DIRECTORY
            } else {
                Format.JAR
            }
            outputProvider?.let { provider ->
                input.transform(
                    provider.getContentLocation(
                        input.id, input.contentTypes, input.scopes, format
                    )
                )
            }
        }
    }

    private fun transformIncrementally(executor: ExecutorService, outOfDate: Set<File>) = this.inputs.map { input ->
        input.jarInputs.filter {
            it.status != NOTCHANGED || outOfDate.contains(it.file)
        }.map { jarInput ->
            executor.submit {
                doIncrementalTransform(jarInput)
            }
        } + input.directoryInputs.filter {
            it.changedFiles.isNotEmpty() || outOfDate.contains(it.file)
        }.map { dirInput ->
            executor.submit {
                doIncrementalTransform(dirInput, dirInput.file.toURI())
            }
        }
    }.flatten()

    @Suppress("NON_EXHAUSTIVE_WHEN")
    private fun doIncrementalTransform(jarInput: JarInput) {
        when (jarInput.status) {
            REMOVED -> jarInput.file.delete()
            else -> {
                outputProvider?.let { provider ->
                    jarInput.transform(provider.getContentLocation(jarInput.id, jarInput.contentTypes, jarInput.scopes, Format.JAR))
                }
            }
        }
    }

    @Suppress("NON_EXHAUSTIVE_WHEN")
    private fun doIncrementalTransform(dirInput: DirectoryInput, base: URI) {
        dirInput.changedFiles.forEach { (file, status) ->
            when (status) {
                REMOVED -> {
                    outputProvider?.let { provider ->
                        provider.getContentLocation(dirInput.id, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY).parentFile.listFiles()?.asSequence()
                            ?.filter { it.isDirectory }
                            ?.map { File(it, dirInput.file.toURI().relativize(file.toURI()).path) }
                            ?.filter { it.exists() }
                            ?.forEach { it.delete() }
                    }
                    file.delete()
                }
                else -> {
                    outputProvider?.let { provider ->
                        val root = provider.getContentLocation(dirInput.id, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                        val output = File(root, base.relativize(file.toURI()).path)
                        file.transform(output)
                    }
                }
            }
        }
    }

    private fun doVerify() {
        outputs.sortedBy(File::nameWithoutExtension).forEach { input ->
            val output = temporaryDir.file(input.name)
//            DexFormat.API_NO_EXTENDED_OPCODES = 13
            val rc = input.dex(output, variant.extension.defaultConfig.targetSdkVersion?.apiLevel ?: 13)
            logger("${if (rc != 0) red("✗") else green("✓")} $input")
            output.deleteRecursively()
        }
    }

    private val QualifiedContent.id: String
        get() = md5Hex(file.absolutePath)

    private fun QualifiedContent.transform(output: File) = this.file.transform(output)

    private fun File.transform(output: File) {
        outputs += output
        project.logger.info("Booster transforming $this => $output")
        this.transform(output = output, config = config) { bytecode ->
            bytecode.transform()
        }
    }

    private fun ByteArray.transform(): ByteArray {
        return transformers.fold(this) { bytes, transformer ->
            transformer.transform(this@PluginTransformInvocation, bytes)
        }
    }

    private fun logger(message: String) {
        logger(level = LogLevel.ERROR, message = message)
    }
}
