package com.jiaoay.plugins.core.internal

import com.android.build.api.variant.VariantInfo
import com.jiaoay.plugins.core.ExtensionsPluginTransform
import com.jiaoay.plugins.core.TransformParameter

internal class PluginTransformV34(parameter: TransformParameter) : ExtensionsPluginTransform(parameter) {

    @Suppress("UnstableApiUsage")
    override fun applyToVariant(variant: VariantInfo): Boolean {
        return variant.buildTypeEnabled || (variant.flavorNames.isNotEmpty() && variant.fullVariantEnabled)
    }

    @Suppress("UnstableApiUsage")
    private val VariantInfo.fullVariantEnabled: Boolean
        get() = parameter.properties["booster.transform.${fullVariantName}.enabled"]?.toString()?.toBoolean() ?: true

    @Suppress("UnstableApiUsage")
    private val VariantInfo.buildTypeEnabled: Boolean
        get() = parameter.properties["booster.transform.${buildTypeName}.enabled"]?.toString()?.toBoolean() ?: true

}

