package com.jiaoay.plugins.trace

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.commons.Method

class TraceMethodVisitor(
    private val traceInfo: TraceInfo,
    api: Int,
    methodVisitor: MethodVisitor?,
    access: Int,
    name: String?,
    descriptor: String?,
) : AdviceAdapter(api, methodVisitor, access, name, descriptor) {

    private val isStatic = access.and(Opcodes.ACC_STATIC) != 0

    private val methodTraceType = Type.getType(PluginTraceInfo::class.java)
    private var methodTraceLocal: Int = -1
    private var lineNumber = -1

    override fun visitLineNumber(line: Int, start: Label?) {
        super.visitLineNumber(line, start)
        lineNumber = if (lineNumber >= 0) {
            line.coerceAtMost(lineNumber)
        } else {
            line
        }
    }

    override fun onMethodEnter() {
        super.onMethodEnter()

        newInstance(methodTraceType)
        dup()
        mv.visitLdcInsn(traceInfo.source)
        mv.visitLdcInsn(name)

        mv.visitLdcInsn(
            if (isStatic) {
                1
            } else {
                0
            },
        )

        invokeConstructor(
            methodTraceType,
            Method(
                "<init>",
                Type.getMethodDescriptor(
                    Type.VOID_TYPE,
                    Type.getType(String::class.java),
                    Type.getType(String::class.java),
                    Type.getType(Int::class.java),
                ),
            ),
        )

        methodTraceLocal = newLocal(methodTraceType)
        storeLocal(methodTraceLocal)

        // set method args.
        loadLocal(methodTraceLocal)

        if (argumentTypes.isNotEmpty()) {
            loadArgArray()
        } else {
            visitInsn(ACONST_NULL)
        }

        invokeVirtual(
            methodTraceType,
            Method(
                "setArguments",
                Type.getMethodDescriptor(
                    Type.VOID_TYPE,
                    Type.getType(Array<Any?>::class.java),
                ),
            ),
        )
    }

    override fun onMethodExit(opcode: Int) {
        super.onMethodExit(opcode)
        // set method result.
        when (opcode) {
            Opcodes.RETURN -> {
                visitInsn(ACONST_NULL)
            }

            Opcodes.ARETURN, Opcodes.ATHROW -> {
                dup()
            }

            Opcodes.LRETURN, Opcodes.DRETURN -> {
                dup2()
                box(Type.getReturnType(this.methodDesc))
            }

            else -> {
                dup()
                box(Type.getReturnType(this.methodDesc))
            }
        }

        mv.visitVarInsn(Opcodes.ALOAD, methodTraceLocal)
        swap()
        mv.visitLdcInsn(lineNumber)
        invokeVirtual(
            methodTraceType,
            Method(
                "endContext",
                Type.getMethodDescriptor(
                    Type.VOID_TYPE,
                    Type.getType(Any::class.java),
                    Type.getType(Int::class.java),
                ),
            ),
        )
    }
}
