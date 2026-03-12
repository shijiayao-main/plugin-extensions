package com.jiaoay.plugins.trace

import com.jiaoay.plugins.core.annotation.isQualifiedClass
import com.jiaoay.plugins.core.annotation.logger
import com.jiaoay.plugins.core.transform.TransformContext
import com.jiaoay.plugins.core.transform.Transformer
import com.jiaoay.plugins.trace.config.TraceConfig
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.CheckClassAdapter

class TraceTransform : Transformer {
    companion object {
        private const val TAG = "TraceTransform"
    }

    private var config: TraceConfig? = null

    override fun onPreTransform(context: TransformContext) {
        super.onPreTransform(context)
        logger("$TAG: onPreTransform: ")
    }

    override fun onPostTransform(context: TransformContext) {
        super.onPostTransform(context)
        logger("$TAG: onPostTransform: ")
    }

    override fun transform(
        context: TransformContext,
        bytecode: ByteArray,
    ): ByteArray {
        val traceConfig: TraceConfig = config ?: let {
            val config = TraceConfig.get(context.project)
            this.config = config
            config
        }

        if (traceConfig.isEnable.not()) {
            return bytecode
        }

        val classReader = checkTraceAndGetClassReader(
            whiteSet = traceConfig.traceWhiteSet,
            blackSet = traceConfig.traceBlackSet,
            bytecode = bytecode,
        )

        if (classReader == null) {
            return bytecode
        }

        return ClassWriter(ClassWriter.COMPUTE_MAXS)
            .also { writer ->
                val traceVisitor = TraceClassVisitor(Opcodes.ASM9, writer)
                val classVisitor = CheckClassAdapter(traceVisitor)
                classReader.accept(
                    classVisitor,
                    ClassReader.EXPAND_FRAMES,
                )
            }
            .toByteArray()
    }

    private fun checkTraceAndGetClassReader(
        whiteSet: Set<String>?,
        blackSet: Set<String>?,
        bytecode: ByteArray,
    ): ClassReader? {
        if (whiteSet.isNullOrEmpty()) {
            return null
        }

        val classReader = ClassReader(bytecode)

        if (classReader.isQualifiedClass.not()) {
            return null
        }

        val clazzName: String = classReader.className.replace('/', '.')

        if (whiteSet.checkIsMatch(clazzName).not()) {
            return null
        }

        if (blackSet.checkIsMatch(clazzName)) {
            return null
        }

        return classReader
    }

    private fun Set<String>?.checkIsMatch(str: String): Boolean {
        if (this.isNullOrEmpty()) {
            return false
        }

        forEach {
            if (str.contains(it)) {
                return true
            }
        }

        return false
    }
}
