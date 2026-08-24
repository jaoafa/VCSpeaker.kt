package com.jaoafa.vcspeaker.tools.discord

abstract class EmbedTemplate(val template: EmbedBuilderLambda) {
    fun build(override: EmbedBuilderLambda? = null): EmbedBuilderLambda {
        return {
            apply(template)
            if (override != null) {
                apply(override)
            }
        }
    }

    fun buildSuspended(override: EmbedBuilderLambdaSuspend? = null): EmbedBuilderLambdaSuspend {
        return {
            apply(template)
            override?.invoke(this)
        }
    }
}