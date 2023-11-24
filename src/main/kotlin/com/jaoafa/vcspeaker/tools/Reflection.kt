package com.jaoafa.vcspeaker.tools

import org.reflections.Reflections

inline fun <reified T> getObjectsIn(packageName: String) =
    Reflections(packageName)
        .getSubTypesOf(T::class.java)
        .filter {
            it.enclosingClass == null && !it.name.contains("$")
        }.map {
            it.kotlin.objectInstance
        }