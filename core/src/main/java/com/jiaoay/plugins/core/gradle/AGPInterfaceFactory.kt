package com.jiaoay.plugins.core.gradle

import com.android.repository.Revision

interface AGPInterfaceFactory {
    val revision: Revision
    fun newAGPInterface(): AGPInterface
}
