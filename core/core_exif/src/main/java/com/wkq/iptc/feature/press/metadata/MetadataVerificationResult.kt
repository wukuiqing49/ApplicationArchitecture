package com.wkq.iptc.feature.press.metadata

data class MetadataVerificationResult(
    val exifVerified: Boolean,
    val iptcVerified: Boolean,
    val xmpVerified: Boolean,
    val error: String?
)
