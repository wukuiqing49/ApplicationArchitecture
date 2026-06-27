package com.wkq.iptc.upload

enum class FtpsSecurityMode {
    EXPLICIT,
    IMPLICIT;

    companion object {
        fun fromValue(value: String?): FtpsSecurityMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXPLICIT
        }
    }
}

