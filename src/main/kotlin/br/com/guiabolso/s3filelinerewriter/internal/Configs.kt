package br.com.guiabolso.s3filelinerewriter.internal

import java.lang.System.getProperty

internal val NEW_LINE = getProperty("br.com.guiabolso.s3filelinerewriter.newline", "\n")

internal val REMOVE_EMPTY_LINES =
    getProperty("br.com.guiabolso.s3filelinerewriter.removeblank", "true")!!.toBoolean()
