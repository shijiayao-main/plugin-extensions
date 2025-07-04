package com.jiaoay.plugins.trace

import com.jiaoay.plugins.core.PluginExtensions

class PluginTraceInfo(
    private val classSource: String?,
    private val methodName: String?,
    private var isStatic: Int = 0,
) {

    private var begin: Long = 0L
    private var end: Long = 0L
    private var result: Any? = null
    private var lineNumber: Int = 0
    private var arguments: Array<Any?>? = null

    init {
        begin = System.currentTimeMillis()
    }

    fun setArguments(arguments: Array<Any?>?) {
        this.arguments = arguments
    }

    fun endContext(r: Any?, line: Int) {
        end = System.currentTimeMillis()
        lineNumber = line

        result = r

        PluginExtensions.outputTrace(this)
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        if (isStatic == 1) {
            stringBuilder
                .append(
                    "static: ",
                )
        }

        stringBuilder
            .append(Thread.currentThread().name)
            .append(" ")
            .append(
                if (lineNumber > 0) {
                    "($classSource:$lineNumber) $methodName"
                } else {
                    "($classSource) $methodName"
                },
            )
            .append(
                "(${parseArguments(arguments)})->",
            )
            .append(
                "[${parseResult(result)}]",
            )
            .append(" ${end - begin}ms")
        return stringBuilder.toString()
    }

    private fun parseResult(result: Any?): String {
        if (result == null || result is Unit) {
            return ""
        }
        return printObj(result)
    }

    private fun parseArguments(arguments: Array<Any?>?): String {
        if (arguments.isNullOrEmpty()) {
            return ""
        }
        val builder = StringBuilder().apply {
            val lastArgIndex = arguments.size - 1
            arguments.forEachIndexed { index, any ->

                val content = printObj(any)
                append(content)
                if (index < lastArgIndex) {
                    append(",")
                }
            }
        }
        return builder.toString()
    }

    private fun printObj(any: Any?): String {
        return when (any) {
            null -> "null"

            is Unit -> ""

            is Enum<*> -> any.javaClass.simpleName + ":" + any.name

            is Iterable<*> -> {
                val cs = StringBuilder()
                cs.append("[")
                any.forEach {
                    cs.append(printObj(it))
                }
                cs.append("]")
                cs.toString()
            }

            is String -> "\"$any\""

            is Boolean -> any.toString()

            is Number -> any.toString()

            else -> any::class.java.simpleName
        }
    }
}
