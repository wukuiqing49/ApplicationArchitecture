package com.wkq.iptc.camera.metadata

import android.location.Location
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExifWriter {
    fun writeBasicMetadata(file: File, location: Location?): Result<Unit> {
        return writeAllMetadata(file, location, null)
    }

    fun writeAllMetadata(file: File, location: Location?, xmpPacket: String? = null): Result<Unit> {
        return runCatching {
            val exif = ExifInterface(file)
            val formatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            val now = formatter.format(Date())
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, now)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, now)
            if (location != null) {
                exif.setGpsInfo(location)
            }
            if (!xmpPacket.isNullOrBlank()) {
                exif.setAttribute(ExifInterface.TAG_XMP, xmpPacket)
            }
            exif.saveAttributes()
        }
    }
}
