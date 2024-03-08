package com.jiaoay.plugins.core.transform

import com.android.SdkConstants
import com.android.build.api.variant.Variant
import com.didiglobal.booster.kotlinx.search
import com.jiaoay.plugins.core.gradle.aar
import com.jiaoay.plugins.core.gradle.allArtifacts
import com.jiaoay.plugins.core.gradle.allClasses
import com.jiaoay.plugins.core.gradle.apk
import com.jiaoay.plugins.core.gradle.mergedAssets
import com.jiaoay.plugins.core.gradle.mergedManifests
import com.jiaoay.plugins.core.gradle.mergedRes
import com.jiaoay.plugins.core.gradle.processedRes
import com.jiaoay.plugins.core.gradle.symbolList
import com.jiaoay.plugins.core.gradle.symbolListWithPackageName
import java.io.File

val Variant.artifactManager: ArtifactManager
    get() = object : ArtifactManager {

        override fun get(type: String): Collection<File> = when (type) {
            ArtifactManager.AAR -> aar.files
            ArtifactManager.ALL_CLASSES -> allClasses.files
            ArtifactManager.APK -> apk.files
            ArtifactManager.MERGED_ASSETS -> mergedAssets.files
            ArtifactManager.MERGED_RES -> mergedRes.files
            ArtifactManager.MERGED_MANIFESTS -> mergedManifests.search {
                SdkConstants.FN_ANDROID_MANIFEST_XML == it.name
            }

            ArtifactManager.PROCESSED_RES -> processedRes.search {
                it.name.startsWith(SdkConstants.FN_RES_BASE) && it.name.endsWith(
                    SdkConstants.EXT_RES,
                )
            }

            ArtifactManager.SYMBOL_LIST -> symbolList.files
            ArtifactManager.SYMBOL_LIST_WITH_PACKAGE_NAME -> symbolListWithPackageName.files
            ArtifactManager.DATA_BINDING_DEPENDENCY_ARTIFACTS -> allArtifacts[ArtifactManager.DATA_BINDING_DEPENDENCY_ARTIFACTS]?.files
                ?: emptyList()

            else -> TODO("Unexpected type: $type")
        }
    }
