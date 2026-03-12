package com.jiaoay.plugins.trace

import com.jiaoay.plugins.core.annotation.PluginAnnotation
import com.jiaoay.plugins.core.annotation.logger
import com.jiaoay.plugins.trace.config.TraceConfig
import org.gradle.api.Project
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

class TraceAnnotation : PluginAnnotation {

    private var config: TraceConfig? = null

    private val traceAnnotationType: Type by lazy {
        Type.getType(Trace::class.java)
    }

    private val traceNotAnnotationType: Type by lazy {
        Type.getType(TraceNot::class.java)
    }

    override fun onPreAnnotationCheck(project: Project) {
        if (config == null) {
            config = TraceConfig.get(project = project)
        }
    }

    override fun checkAnnotationNode(
        className: String,
        classNode: ClassNode,
        annotationNode: AnnotationNode,
    ) {
        val annotationNodeDesc = annotationNode.desc
        when(annotationNodeDesc) {
            traceAnnotationType.descriptor -> {
                logger("TraceAnnotation: handleFile: white className: $className")
                val whiteSet = config?.traceWhiteSet
                if (whiteSet == null) {
                    config?.traceWhiteSet = mutableSetOf(className)
                } else {
                    whiteSet.add(className)
                }
            }

            traceNotAnnotationType.descriptor -> {
                logger("TraceAnnotation: handleFile: black className: $className")
                val blackSet = config?.traceBlackSet
                if (blackSet == null) {
                    config?.traceBlackSet = mutableSetOf(className)
                } else {
                    blackSet.add(className)
                }
            }
        }
    }

    override fun fileCheckEnd() {
        logger("fileCheckEnd: ")
        config?.traceWhiteSet?.forEach {
            logger("white: $it")
        }
        config?.traceBlackSet?.forEach {
            logger("black: $it")
        }
    }
}
