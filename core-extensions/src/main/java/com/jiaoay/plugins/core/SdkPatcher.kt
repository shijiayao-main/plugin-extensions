package com.jiaoay.plugins.core

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class SdkPatcher(
    val name: String = "",
)
