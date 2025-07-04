package com.jiaoay.plugins.trace

import com.jiaoay.plugins.core.annotation.isQualified
import com.jiaoay.plugins.core.annotation.logger
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class TraceClassVisitor(
    api: Int,
    classVisitor: ClassVisitor?,
) : ClassVisitor(api, classVisitor) {

    companion object {
        private const val TAG = "TraceClassVisitor"
    }

    private var rawName: String = ""
    private var displayClassName: String = ""
    private var classAccess: Int = 0
    private var injected: Int = 0
    private val traceInfo: TraceInfo = TraceInfo()

    override fun visitSource(source: String?, debug: String?) {
        super.visitSource(source, debug)
        traceInfo.source = source ?: ""
        traceInfo.debug = debug ?: ""
    }

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?,
    ) {
        super.visit(version, access, name, signature, superName, interfaces)

        rawName = name ?: ""
        displayClassName = rawName.replace('/', '.')
        classAccess = access

        traceInfo.name = rawName
        traceInfo.display = displayClassName
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor? {
        val methodVisitor = cv?.visitMethod(
            access,
            name,
            descriptor,
            signature,
            exceptions,
        ) ?: return super.visitMethod(
            access,
            name,
            descriptor,
            signature,
            exceptions,
        )

        if (access.isQualified.not()) {
            logger(tag = TAG, message = "is not qualified, name: $name, access: $access")
            return methodVisitor
        }

        injected += 1

        traceInfo.isConstruct = name == "<init>"

        return TraceMethodVisitor(
            traceInfo = traceInfo,
            api = api,
            methodVisitor = methodVisitor,
            access = access,
            name = name,
            descriptor = descriptor,
        )
    }

    override fun visitEnd() {
        super.visitEnd()
        if (injected > 0) {
            logger(message = "after $rawName.")
        }
    }
}
