package com.jaoafa.vcspeaker.database

import org.jetbrains.exposed.v1.core.Column

interface VersionedTable {
    val version: Column<Int>
}