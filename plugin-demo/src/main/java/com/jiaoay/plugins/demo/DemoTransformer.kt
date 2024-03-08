package com.jiaoay.plugins.demo

import com.jiaoay.plugins.core.annotation.isQualifiedClass
import com.jiaoay.plugins.core.transform.TransformContext
import com.jiaoay.plugins.core.transform.Transformer
import com.jiaoay.plugins.demo.config.DemoConfig
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

class DemoTransformer : Transformer {

    companion object {
        private const val TAG = "DemoTransformer"
    }

    private var config: DemoConfig? = null

    override fun onPreTransform(context: TransformContext) {
        super.onPreTransform(context)
        logger("onPreTransform: ")
    }

    override fun onPostTransform(context: TransformContext) {
        super.onPostTransform(context)
        logger("onPostTransform: ")
    }

    override fun transform(context: TransformContext, bytecode: ByteArray): ByteArray {
        val sdkPatcherConfig: DemoConfig = this.config ?: let {
            val config = DemoConfig.get(context.project)
            this.config = config
            config
        }
        if (sdkPatcherConfig.isEnable.not()) {
            return bytecode
        }
        val classReader = ClassReader(bytecode)

        if (classReader.isQualifiedClass.not()) {
            return bytecode
        }

        return ClassWriter(ClassWriter.COMPUTE_MAXS).also { writer ->
            val classNode = ClassNode().also { cn ->
                classReader.accept(cn, 0)
            }
//            if (classNode.name.contains("DrawableDecoderCompat", ignoreCase = true)) {
//                context.logger("name: ${classNode.name}, sourceFile: ${classNode.sourceFile}")
//            }
            logger("name: ${classNode.name}")
            classNode.accept(writer)
        }.toByteArray()
    }

    private fun logger(message: String) {
        com.jiaoay.plugins.core.annotation.logger("${config?.tag ?: TAG}: $message")
    }
}
