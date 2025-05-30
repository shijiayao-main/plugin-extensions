package com.jiaoay.plugins.core.util

import com.didiglobal.booster.kotlinx.search
import com.jiaoay.plugins.core.transform.AbstractTransformContext
import com.jiaoay.plugins.core.transform.TransformContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

typealias Collector<T> = com.jiaoay.plugins.core.transform.Collector<T>

internal const val PATTERN_FQN = "(([a-zA-Z_\$][a-zA-Z\\d_\$]*\\.)*[a-zA-Z_\$][a-zA-Z\\d_\$]*)"

internal val REGEX_FQN = Regex("^$PATTERN_FQN$")

internal val REGEX_SPI = Regex("^META-INF/services/$PATTERN_FQN$")

sealed class Collectors {

    /**
     * A collector for class name collecting
     */
    object ClassNameCollector : Collector<String> {
        override fun accept(name: String): Boolean {
            return name.endsWith(".class", true)
        }

        override fun collect(name: String, data: () -> ByteArray): String {
            return name.substringBeforeLast('.').replace('/', '.')
        }
    }

    /**
     * A collector for service (SPI) collecting
     */
    object ServiceCollector : Collector<Pair<String, Collection<String>>> {

        override fun accept(name: String): Boolean {
            return name matches REGEX_SPI
        }

        override fun collect(
            name: String,
            data: () -> ByteArray,
        ): Pair<String, Collection<String>> {
            return REGEX_SPI.matchEntire(name)!!.groupValues[1] to data().inputStream()
                .bufferedReader().lineSequence().filterNot {
                    it.isBlank() || it.startsWith('#')
                }.toSet()
        }
    }
}

class NameCollector(private val names: Set<String>) : Collector<String> {

    constructor(names: Iterable<String>) : this(names.toSet())

    constructor(vararg names: String) : this(names.toSet())

    override fun accept(name: String) = name in names

    override fun collect(name: String, data: () -> ByteArray) = name
}

class RegexCollector(private val regex: Regex) : Collector<String> {

    constructor(regex: String) : this(Regex(regex))

    constructor(pattern: Pattern) : this(pattern.toRegex())

    override fun accept(name: String): Boolean = name matches regex

    override fun collect(name: String, data: () -> ByteArray): String = name
}

/**
 * Collecting information from file with [collector], the supported file types are as follows:
 *
 * - directories
 * - archive files
 */
fun <R> File.collect(collector: Collector<R>): List<R> = when {
    this.isDirectory -> {
        val base = this.toURI()
        this.search { f ->
            f.isFile && collector.accept(base.relativize(f.toURI()).normalize().path)
        }.map { f ->
            collector.collect(base.relativize(f.toURI()).normalize().path, f::readBytes)
        }
    }

    this.isFile -> {
        this.inputStream().buffered().use {
            ArchiveStreamFactory().createArchiveInputStream(it).let { archive ->
                generateSequence {
                    try {
                        archive.nextEntry
                    } catch (e: IOException) {
                        null
                    }
                }.filterNot(ArchiveEntry::isDirectory).filter { entry ->
                    collector.accept(entry.name)
                }.map { entry ->
                    collector.collect(entry.name, archive::readBytes)
                }.toList()
            }
        }
    }

    else -> emptyList()
}

fun <R> TransformContext.collect(collector: Collector<R>): List<R> {
    return compileClasspath.map { file ->
        file.collect(collector)
    }.flatten()
}

fun AbstractTransformContext.collect(): List<*> = collect(CompositeCollector(collectors)).flatten()
