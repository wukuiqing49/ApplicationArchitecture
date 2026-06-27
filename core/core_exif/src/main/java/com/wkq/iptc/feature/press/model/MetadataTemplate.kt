package com.wkq.iptc.feature.press.model

data class MetadataTemplate(
    val id: String,
    val displayName: String = "",
    val templateType: String = "",
    val creator: String = "",
    val title: String = "",
    val subtitle: String = "",
    val artist: String = "",
    val jobTitle: String = "",
    val state: String = "",
    val email: String = "",
    val iptcAuthor: String = "",
    val writer: String = "",          // IPTC Writer/Editor [2:122]：说明撰写人
    val category: String = "",
    val isoCountryCode: String = "",
    val credit: String = "",
    val source: String = "",
    val copyrightStatus: String = "",
    val rightsUsageTerms: String = "",
    val copyright: String = "",
    val country: String = "",
    val city: String = "",
    val location: String = "",
    val headline: String = "",
    val caption: String = "",
    val keywords: List<String> = emptyList(),
    val customFields: Map<String, String> = emptyMap(),
    val enableAntiTamper: Boolean = false,
    val autoUpload: Boolean = true,
    val isLibraryPreset: Boolean = false,
    val displayNameRes: String? = null,
    val descriptionRes: String? = null,
    val headlineRes: String? = null,
    val captionRes: String? = null,
    val rightsUsageTermsRes: String? = null,
    val copyrightStatusRes: String? = null,
    val enabledFields: List<String> = emptyList(),
    val standardFields: Map<String, String> = emptyMap(),
    val visibleStandardFields: List<String> = emptyList(),
    val workflowPostCaptureAction: String = WORKFLOW_ACTION_QUICK_EDIT,
    val workflowRequiredFields: List<String> = emptyList(),
    val workflowScrubPrivacy: Boolean = false,
    val workflowCompressForUpload: Boolean = false
) {
    fun isFieldEnabled(field: String): Boolean {
        if (enabledFields.isEmpty()) return !MetadataFieldDefinitions.isStandardField(field)
        return enabledFields.contains(field)
    }

    fun standardValue(field: String): String {
        return standardFields[field].orEmpty()
    }

    fun shouldOpenEditorAfterCapture(): Boolean {
        return workflowPostCaptureAction != WORKFLOW_ACTION_DIRECT_PROCESS
    }

    companion object {
        const val WORKFLOW_ACTION_QUICK_EDIT = "quick_edit"
        const val WORKFLOW_ACTION_DIRECT_PROCESS = "direct_process"
    }
}
