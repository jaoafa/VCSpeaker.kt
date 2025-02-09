package com.jaoafa.vcspeaker.tools

import java.security.MessageDigest

fun hashMd5(content: String) = MessageDigest
    .getInstance("MD5")
    .digest(content.toByteArray())
    .fold("") { str, it -> str + "%02x".format(it) }