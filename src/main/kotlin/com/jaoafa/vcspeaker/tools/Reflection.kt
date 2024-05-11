package com.jaoafa.vcspeaker.tools

import org.reflections.Reflections

inline fun <reified T> getClassesIn(packageName: String) =
    Reflections(packageName)
        .getSubTypesOf(T::class.java)
        .filter {
            it.enclosingClass == null && !it.name.contains("$")
        }
