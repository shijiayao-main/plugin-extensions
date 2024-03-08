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
        if (annotationNodeDesc != traceAnnotationType.descriptor) {
            return
        }

        logger("TraceAnnotation: handleFile: className: $className")
        val classSet = config?.traceClassSet
        if (classSet == null) {
            config?.traceClassSet = mutableSetOf(className)
        } else {
            classSet.add(className)
        }
    }
}
