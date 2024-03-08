package com.jiaoay.plugins.trace

import com.android.SdkConstants
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

        val traceClassSet = traceConfig.traceClassSet ?: return bytecode

        if (traceClassSet.isEmpty()) {
            return bytecode
        }

        if (traceConfig.isEnable.not()) {
            return bytecode
        }

        val classReader = ClassReader(bytecode)

        if (classReader.isQualifiedClass.not()) {
            return bytecode
        }

        val clazzName: String = classReader.className.plus(SdkConstants.DOT_CLASS) ?: ""
        if (traceClassSet.contains(clazzName).not()) {
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
}
