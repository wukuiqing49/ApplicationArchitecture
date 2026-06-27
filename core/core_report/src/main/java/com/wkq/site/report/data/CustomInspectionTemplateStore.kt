package com.wkq.site.report.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CustomInspectionTemplate(
    val id: Long,
    val key: String,
    val title: String,
    val description: String,
    val sections: List<CustomInspectionTemplateSection> = emptyList(),
    val items: List<CustomInspectionTemplateItem> = emptyList()
)

data class CustomInspectionTemplateSection(
    val key: String,
    val title: String,
    val description: String = "",
    val sortOrder: Int = 0,
    val items: List<CustomInspectionTemplateItem>
)

data class CustomInspectionTemplateItem(
    val key: String,
    val title: String,
    val description: String = "",
    val required: Boolean,
    val photoRequired: Boolean = false,
    val minPhotoCount: Int,
    val allowNotApplicable: Boolean = true,
    val requireNote: Boolean = false,
    val requireIssue: Boolean = true,
    val requireSuggestion: Boolean = false,
    val sortOrder: Int = 0
)

object CustomInspectionTemplateStore {
    private const val PREFS = "site_report_custom_templates"
    private const val KEY_TEMPLATES = "templates"
    const val CUSTOM_KEY_PREFIX = "custom_"

    fun loadAll(context: Context): List<CustomInspectionTemplate> {
        val raw = prefs(context).getString(KEY_TEMPLATES, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            val defaultSectionTitle = context.getString(com.wkq.site.report.R.string.sr_core_section_inspection_items)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    add(obj.toTemplate(defaultSectionTitle))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun findByKey(context: Context, key: String): CustomInspectionTemplate? {
        return loadAll(context).firstOrNull { it.key == key }
    }

    fun save(context: Context, template: CustomInspectionTemplate) {
        val existing = loadAll(context).filterNot { it.key == template.key || it.id == template.id }
        saveAll(context, existing + template)
    }

    fun deleteByIds(context: Context, ids: Set<Long>) {
        if (ids.isEmpty()) return
        saveAll(context, loadAll(context).filterNot { it.id in ids })
    }

    fun nextId(context: Context): Long {
        val maxId = loadAll(context).maxOfOrNull { it.id } ?: 10_000L
        return (maxId + 1L).coerceAtLeast(10_001L)
    }

    private fun saveAll(context: Context, templates: List<CustomInspectionTemplate>) {
        val array = JSONArray()
        val defaultSectionTitle = context.getString(com.wkq.site.report.R.string.sr_core_section_inspection_items)
        templates.forEach { array.put(it.toJson(defaultSectionTitle)) }
        prefs(context).edit().putString(KEY_TEMPLATES, array.toString()).apply()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun JSONObject.toTemplate(defaultSectionTitle: String): CustomInspectionTemplate {
        val legacyItems = parseItems(optJSONArray("items") ?: JSONArray())
        val sectionsArray = optJSONArray("sections") ?: JSONArray()
        val sections = buildList {
            for (index in 0 until sectionsArray.length()) {
                val obj = sectionsArray.optJSONObject(index) ?: continue
                val title = obj.optString("title").trim()
                if (title.isBlank()) continue
                val items = parseItems(obj.optJSONArray("items") ?: JSONArray())
                add(
                    CustomInspectionTemplateSection(
                        key = obj.optString("key").ifBlank { "section_$index" },
                        title = title,
                        description = obj.optString("description"),
                        sortOrder = obj.optInt("sortOrder", index),
                        items = items
                    )
                )
            }
        }.ifEmpty {
            if (legacyItems.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    CustomInspectionTemplateSection(
                        key = "default",
                        title = defaultSectionTitle,
                        items = legacyItems
                    )
                )
            }
        }
        return CustomInspectionTemplate(
            id = optLong("id"),
            key = optString("key"),
            title = optString("title"),
            description = optString("description"),
            sections = sections,
            items = legacyItems.ifEmpty { sections.flatMap { it.items } }
        )
    }

    private fun parseItems(itemsArray: JSONArray): List<CustomInspectionTemplateItem> {
        return buildList {
            for (index in 0 until itemsArray.length()) {
                val obj = itemsArray.optJSONObject(index) ?: continue
                val title = obj.optString("title").trim()
                if (title.isBlank()) continue
                val minPhotoCount = obj.optInt("minPhotoCount", 1).coerceAtLeast(0)
                add(
                    CustomInspectionTemplateItem(
                        key = obj.optString("key").ifBlank { "item_$index" },
                        title = title,
                        description = obj.optString("description"),
                        required = obj.optBoolean("required", true),
                        photoRequired = obj.optBoolean("photoRequired", minPhotoCount > 0),
                        minPhotoCount = minPhotoCount,
                        allowNotApplicable = obj.optBoolean("allowNotApplicable", true),
                        requireNote = obj.optBoolean("requireNote", false),
                        requireIssue = obj.optBoolean("requireIssue", true),
                        requireSuggestion = obj.optBoolean("requireSuggestion", false),
                        sortOrder = obj.optInt("sortOrder", index)
                    )
                )
            }
        }
    }

    private fun CustomInspectionTemplate.toJson(defaultSectionTitle: String): JSONObject {
        val normalizedSections = sections.ifEmpty {
            listOf(CustomInspectionTemplateSection(key = "default", title = defaultSectionTitle, items = items))
        }
        return JSONObject()
            .put("id", id)
            .put("key", key)
            .put("title", title)
            .put("description", description)
            .put(
                "sections",
                JSONArray().also { array ->
                    normalizedSections.forEach { section ->
                        array.put(
                            JSONObject()
                                .put("key", section.key)
                                .put("title", section.title)
                                .put("description", section.description)
                                .put("sortOrder", section.sortOrder)
                                .put("items", JSONArray().also { itemsArray ->
                                    section.items.forEach { item ->
                                        itemsArray.put(item.toJson())
                                    }
                                })
                        )
                    }
                }
            )
            .put("items", JSONArray().also { array ->
                normalizedSections.flatMap { it.items }.forEach { item -> array.put(item.toJson()) }
            })
    }

    private fun CustomInspectionTemplateItem.toJson(): JSONObject {
        return JSONObject()
            .put("key", key)
            .put("title", title)
            .put("description", description)
            .put("required", required)
            .put("photoRequired", photoRequired)
            .put("minPhotoCount", minPhotoCount)
            .put("allowNotApplicable", allowNotApplicable)
            .put("requireNote", requireNote)
            .put("requireIssue", requireIssue)
            .put("requireSuggestion", requireSuggestion)
            .put("sortOrder", sortOrder)
    }
}
