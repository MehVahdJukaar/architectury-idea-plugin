package net.mehvahdjukaar.candle.util

enum class AnnotationType(private val annotations: Set<String>) : Set<String> by annotations {
    PLATFORM_IMPLEMENTATION(
        "net.mehvahdjukaar.candlelight.api.PlatformImpl"
    ),
    VIRTUAL_OVERRIDE(
        "net.mehvahdjukaar.candlelight.api.VirtualOverride"
    ),
    OPTIONAL_INTERFACE("net.mehvahdjukaar.candlelight.api.OptionalInterface"
    )
    ;

    constructor(vararg annotations: String) : this(annotations.toSet())
}
