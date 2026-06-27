package com.wkq.iptc.feature.press.metadata

import androidx.exifinterface.media.ExifInterface
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

object XmpMetadataReader {

    data class FieldcamMetadata(
        val templateId: String = "",
        val templateName: String = "",
        val templateType: String = "",
        val sceneFields: List<SceneField> = emptyList()
    ) {
        val hasContent: Boolean
            get() = templateId.isNotBlank() ||
                templateName.isNotBlank() ||
                templateType.isNotBlank() ||
                sceneFields.isNotEmpty()
    }

    data class SceneField(
        val name: String,
        val value: String
    )

    fun readFieldcamMetadata(photoFile: File): Result<FieldcamMetadata> = runCatching {
        val xmp = ExifInterface(photoFile).getAttribute(ExifInterface.TAG_XMP).orEmpty()
        if (xmp.isBlank()) return@runCatching FieldcamMetadata()

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(xmp)))
        val root = document.documentElement

        FieldcamMetadata(
            templateId = root.firstText("fieldcam:templateId"),
            templateName = root.firstText("fieldcam:templateName"),
            templateType = root.firstText("fieldcam:templateType"),
            sceneFields = root.sceneFields()
        )
    }

    private fun Element.sceneFields(): List<SceneField> {
        val container = firstElement("fieldcam:sceneFields") ?: return emptyList()
        val items = container.getElementsByTagName("rdf:li")
        return buildList {
            for (index in 0 until items.length) {
                val item = items.item(index) as? Element ?: continue
                val name = item.firstText("fieldcam:name")
                if (name.isBlank()) continue
                add(SceneField(name = name, value = item.firstText("fieldcam:value")))
            }
        }
    }

    private fun Element.firstText(tagName: String): String {
        return firstElement(tagName)?.textContent?.trim().orEmpty()
    }

    private fun Element.firstElement(tagName: String): Element? {
        val nodes = getElementsByTagName(tagName)
        return nodes.item(0) as? Element
    }
}
