package com.wkq.site.report.data

import android.content.Context
import androidx.annotation.StringRes
import com.wkq.site.report.R
import kotlinx.coroutines.flow.Flow

class InspectionRecordRepository private constructor(context: Context) {

    private val dao = SiteReportDatabase.getInstance(context).inspectionRecordDao()
    private val itemDao = SiteReportDatabase.getInstance(context).inspectionItemDao()
    private val photoDao = SiteReportDatabase.getInstance(context).inspectionPhotoDao()
    private val signatureDao = SiteReportDatabase.getInstance(context).acceptanceSignatureDao()
    private val signatureProfileDao = SiteReportDatabase.getInstance(context).signatureProfileDao()
    private val appContext = context.applicationContext

    suspend fun createRecord(
        projectName: String,
        location: String,
        templateId: String = InspectionRecordEntity.DEFAULT_TEMPLATE_ID,
        templateName: String = "",
        pdfTemplateId: String = "",
        serverProfileId: String = "",
        autoUploadEnabled: Boolean = false,
        signatureProfileId: String = ""
    ): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            InspectionRecordEntity(
                projectName = projectName,
                location = location,
                templateId = templateId,
                templateName = templateName,
                pdfTemplateId = pdfTemplateId,
                serverProfileId = serverProfileId,
                autoUploadEnabled = autoUploadEnabled,
                signatureProfileId = signatureProfileId,
                status = InspectionRecordEntity.STATUS_ACTIVE,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun queryAll(): List<InspectionRecordEntity> = dao.queryAll()

    fun observeAll(): Flow<List<InspectionRecordEntity>> = dao.observeAll()

    suspend fun queryById(id: Long): InspectionRecordEntity? = dao.queryById(id)

    suspend fun queryItemById(itemId: Long): InspectionItemEntity? = itemDao.queryById(itemId)

    suspend fun queryItems(inspectionId: Long): List<InspectionItemEntity> {
        ensureDefaultItems(inspectionId)
        return itemDao.queryByInspectionId(inspectionId)
    }

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun update(record: InspectionRecordEntity) = dao.update(record.copy(updatedAt = System.currentTimeMillis()))

    suspend fun updateProgress(id: Long, status: String, progress: Int) {
        dao.updateProgress(id = id, status = status, progress = progress.coerceIn(0, 100))
    }

    suspend fun toggleItemDone(itemId: Long): InspectionItemEntity? {
        val current = itemDao.queryById(itemId) ?: return null
        val nextStatus = if (current.status == InspectionItemEntity.STATUS_DONE) {
            InspectionItemEntity.STATUS_PENDING
        } else {
            InspectionItemEntity.STATUS_DONE
        }
        val updated = current.copy(status = nextStatus, updatedAt = System.currentTimeMillis())
        itemDao.update(updated)
        refreshRecordProgress(current.inspectionId)
        return updated
    }

    suspend fun updateItemDetails(
        itemId: Long,
        status: String,
        note: String,
        issue: String,
        suggestion: String,
        result: String = "",
        responsibleParty: String = "",
        rectificationDeadline: String = ""
    ): InspectionItemEntity? {
        val current = itemDao.queryById(itemId) ?: return null
        val updated = current.copy(
            status = status,
            result = result,
            note = note,
            issue = issue,
            suggestion = suggestion,
            responsibleParty = responsibleParty,
            rectificationDeadline = rectificationDeadline,
            updatedAt = System.currentTimeMillis()
        )
        itemDao.update(updated)
        refreshRecordProgress(current.inspectionId)
        return updated
    }

    suspend fun addPhotoForItem(
        itemId: Long,
        filePath: String,
        latitude: Double? = null,
        longitude: Double? = null,
        locationAccuracy: Float? = null,
        locationProvider: String = ""
    ): Long {
        val item = itemDao.queryById(itemId) ?: error(appContext.getString(R.string.sr_core_error_item_missing))
        val photoId = photoDao.insert(
            InspectionPhotoEntity(
                inspectionId = item.inspectionId,
                itemId = item.id,
                filePath = filePath,
                latitude = latitude,
                longitude = longitude,
                locationAccuracy = locationAccuracy,
                locationProvider = locationProvider
            )
        )
        val itemPhotoCount = photoDao.countByItemId(item.id)
        itemDao.update(
            item.copy(
                status = if (item.status == InspectionItemEntity.STATUS_PENDING && !item.required) {
                    InspectionItemEntity.STATUS_DONE
                } else {
                    item.status
                },
                photoCount = itemPhotoCount,
                updatedAt = System.currentTimeMillis()
            )
        )
        val record = dao.queryById(item.inspectionId)
        val totalPhotoCount = photoDao.countByInspectionId(item.inspectionId)
        if (record != null) {
            dao.update(record.copy(photoCount = totalPhotoCount, updatedAt = System.currentTimeMillis()))
        }
        refreshRecordProgress(item.inspectionId)
        return photoId
    }

    suspend fun queryPhotos(inspectionId: Long): List<InspectionPhotoEntity> = photoDao.queryByInspectionId(inspectionId)

    suspend fun queryPhotosByItemId(itemId: Long): List<InspectionPhotoEntity> = photoDao.queryByItemId(itemId)

    suspend fun deletePhoto(photoId: Long): InspectionPhotoEntity? {
        val photo = photoDao.queryById(photoId) ?: return null
        photoDao.deleteById(photoId)
        val item = itemDao.queryById(photo.itemId)
        if (item != null) {
            val itemPhotoCount = photoDao.countByItemId(photo.itemId)
            val missingRequiredPhotos = item.photoRequired && itemPhotoCount < item.minPhotoCount.coerceAtLeast(1)
            itemDao.update(
                item.copy(
                    status = if (missingRequiredPhotos && item.status.isDoneStatus()) {
                        InspectionItemEntity.STATUS_PENDING
                    } else {
                        item.status
                    },
                    photoCount = itemPhotoCount,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        val record = dao.queryById(photo.inspectionId)
        if (record != null) {
            dao.update(
                record.copy(
                    photoCount = photoDao.countByInspectionId(photo.inspectionId),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        refreshRecordProgress(photo.inspectionId)
        return photo
    }

    suspend fun querySignatures(inspectionId: Long): List<AcceptanceSignatureEntity> =
        signatureDao.queryByInspectionId(inspectionId)

    suspend fun saveSignature(signature: AcceptanceSignatureEntity): Long =
        signatureDao.upsert(signature)

    suspend fun querySignatureProfiles(): List<SignatureProfileEntity> =
        signatureProfileDao.queryAll()

    suspend fun querySignatureProfileById(id: Long): SignatureProfileEntity? =
        signatureProfileDao.queryById(id)

    suspend fun querySelectedSignatureProfile(): SignatureProfileEntity? =
        signatureProfileDao.querySelected() ?: signatureProfileDao.queryAll().firstOrNull()

    suspend fun saveSignatureProfile(profile: SignatureProfileEntity): Long {
        val now = System.currentTimeMillis()
        val id = if (profile.id > 0L) {
            signatureProfileDao.update(profile.copy(updatedAt = now))
            profile.id
        } else {
            signatureProfileDao.insert(profile.copy(createdAt = now, updatedAt = now))
        }
        if (profile.selected || signatureProfileDao.countAll() == 1) {
            selectSignatureProfile(id)
        }
        return id
    }

    suspend fun selectSignatureProfile(id: Long) {
        signatureProfileDao.clearSelected()
        signatureProfileDao.markSelected(id, System.currentTimeMillis())
    }

    suspend fun validateBeforeSubmit(inspectionId: Long): List<String> {
        val items = queryItems(inspectionId)
        return buildList {
            items.filter { it.required && it.status == InspectionItemEntity.STATUS_PENDING }
                .forEach { add(appContext.getString(R.string.sr_core_validation_required, it.title)) }
            items.filter { it.photoRequired && it.photoCount < it.minPhotoCount.coerceAtLeast(1) }
                .forEach { add(appContext.getString(R.string.sr_core_validation_photo_required, it.title, it.minPhotoCount.coerceAtLeast(1))) }
            items.filter {
                it.status in listOf(InspectionItemEntity.STATUS_FAILED, InspectionItemEntity.STATUS_RECTIFY) &&
                    it.requireIssue &&
                    it.issue.isBlank()
            }.forEach { add(appContext.getString(R.string.sr_core_validation_issue_required, it.title)) }
            items.filter {
                it.status in listOf(InspectionItemEntity.STATUS_FAILED, InspectionItemEntity.STATUS_RECTIFY) &&
                    it.requireSuggestion &&
                    it.suggestion.isBlank()
            }.forEach { add(appContext.getString(R.string.sr_core_validation_suggestion_required, it.title)) }
            items.filter { it.requireNote && it.note.isBlank() }
                .forEach { add(appContext.getString(R.string.sr_core_validation_note_required, it.title)) }
            items.filter { it.status == InspectionItemEntity.STATUS_NOT_APPLICABLE && !it.allowNotApplicable }
                .forEach { add(appContext.getString(R.string.sr_core_validation_na_not_allowed, it.title)) }
        }
    }

    suspend fun refreshRecordProgress(inspectionId: Long) {
        val total = itemDao.countByInspectionId(inspectionId)
        val completed = itemDao.countCompleted(inspectionId)
        val progress = if (total == 0) 0 else (completed * 100 / total)
        val status = if (total > 0 && completed == total) {
            InspectionRecordEntity.STATUS_DONE
        } else {
            InspectionRecordEntity.STATUS_ACTIVE
        }
        dao.updateProgress(inspectionId, status, progress)
    }

    suspend fun updateUploadState(
        id: Long,
        reportFilePath: String,
        uploadStatus: String,
        uploadRemotePath: String = "",
        uploadError: String = ""
    ) {
        dao.updateUploadState(
            id = id,
            reportFilePath = reportFilePath,
            uploadStatus = uploadStatus,
            uploadRemotePath = uploadRemotePath,
            uploadError = uploadError
        )
    }

    suspend fun countAll(): Int = dao.countAll()

    suspend fun countActive(): Int = dao.countActive()

    suspend fun countDone(): Int = dao.countDone()

    private suspend fun ensureDefaultItems(inspectionId: Long) {
        if (inspectionId <= 0L || itemDao.countByInspectionId(inspectionId) > 0) return
        val record = dao.queryById(inspectionId) ?: return
        itemDao.insertAll(
            templateItems(record.templateId).mapIndexed { index, it ->
                InspectionItemEntity(
                    inspectionId = inspectionId,
                    sectionKey = it.sectionKey,
                    sectionTitle = it.sectionTitle,
                    itemKey = it.key,
                    title = it.title,
                    description = it.description,
                    required = it.required,
                    photoRequired = it.photoRequired,
                    minPhotoCount = it.minPhotoCount,
                    allowNotApplicable = it.allowNotApplicable,
                    requireNote = it.requireNote,
                    requireIssue = it.requireIssue,
                    requireSuggestion = it.requireSuggestion,
                    sortOrder = it.sortOrder.takeIf { order -> order > 0 } ?: index
                )
            }
        )
    }

    private fun templateItems(templateId: String): List<InspectionItemSpec> {
        if (templateId.startsWith(CustomInspectionTemplateStore.CUSTOM_KEY_PREFIX)) {
            val customSections = CustomInspectionTemplateStore.findByKey(appContext, templateId)
                ?.sections
                .orEmpty()
            if (customSections.isNotEmpty()) {
                return customSections.sortedBy { it.sortOrder }
                    .flatMapIndexed { sectionIndex, section ->
                        section.items.sortedBy { it.sortOrder }.mapIndexed { itemIndex, item ->
                            InspectionItemSpec(
                                sectionKey = section.key.ifBlank { "section_$sectionIndex" },
                                sectionTitle = section.title.ifBlank {
                                    appContext.getString(R.string.sr_core_section_inspection_items)
                                },
                                key = item.key,
                                title = item.title,
                                description = item.description,
                                required = item.required,
                                photoRequired = item.photoRequired,
                                minPhotoCount = item.minPhotoCount,
                                allowNotApplicable = item.allowNotApplicable,
                                requireNote = item.requireNote,
                                requireIssue = item.requireIssue,
                                requireSuggestion = item.requireSuggestion,
                                sortOrder = sectionIndex * 1000 + itemIndex
                            )
                        }
                    }
            }
        }
        return builtInTemplateItems(templateId)
    }

    private fun builtInTemplateItems(templateId: String): List<InspectionItemSpec> {
        return when (templateId) {
            "house_handover_acceptance" -> listOf(
                spec("exterior_condition", R.string.sr_core_item_exterior_condition, true, 2, "property_overview", R.string.sr_core_section_property_overview),
                spec("interior_condition", R.string.sr_core_item_interior_condition, true, 2, "interior_condition", R.string.sr_core_section_interior_condition),
                spec("doors_windows", R.string.sr_core_item_doors_windows, true, 2, "interior_condition", R.string.sr_core_section_interior_condition),
                spec("plumbing_leaks", R.string.sr_core_item_plumbing_leaks, true, 2, "utilities", R.string.sr_core_section_utilities),
                spec("electrical_lighting", R.string.sr_core_item_electrical_lighting, true, 2, "utilities", R.string.sr_core_section_utilities),
                spec("kitchen_bathroom", R.string.sr_core_item_kitchen_bathroom, true, 2, "fixtures", R.string.sr_core_section_fixtures),
                spec("defect_evidence", R.string.sr_core_item_defect_evidence, true, 1, "evidence", R.string.sr_core_section_evidence)
            )
            "rental_move_in_out_acceptance" -> listOf(
                spec("room_condition", R.string.sr_core_item_room_condition, true, 2, "rental_condition", R.string.sr_core_section_rental_condition),
                spec("furniture_appliances", R.string.sr_core_item_furniture_appliances, true, 2, "assets", R.string.sr_core_section_assets),
                spec("utility_readings", R.string.sr_core_item_utility_readings, true, 2, "utilities", R.string.sr_core_section_utilities),
                spec("keys_access", R.string.sr_core_item_keys_access, true, 1, "handover_items", R.string.sr_core_section_handover_items),
                spec("existing_damage", R.string.sr_core_item_existing_damage, true, 1, "damage_records", R.string.sr_core_section_damage_records),
                spec("tenant_landlord_signoff", R.string.sr_core_item_tenant_landlord_signoff, false, 1, "sign_off", R.string.sr_core_section_sign_off)
            )
            "renovation_completion_acceptance" -> listOf(
                spec("defect_summary", R.string.sr_core_item_defect_summary, true, 1, "punch_list", R.string.sr_core_section_punch_list),
                spec("finish_quality", R.string.sr_core_item_finish_quality, true, 2, "workmanship", R.string.sr_core_section_workmanship),
                spec("paint_tile_trim", R.string.sr_core_item_paint_tile_trim, true, 2, "workmanship", R.string.sr_core_section_workmanship),
                spec("fixture_installation", R.string.sr_core_item_fixture_installation, true, 2, "installation", R.string.sr_core_section_installation),
                spec("mep_testing", R.string.sr_core_item_mep_testing, true, 2, "systems", R.string.sr_core_section_systems),
                spec("priority_owner_due_date", R.string.sr_core_item_priority_owner_due_date, true, 1, "rectification", R.string.sr_core_section_rectification),
                spec("recheck_status", R.string.sr_core_item_recheck_status, false, 1, "close_out", R.string.sr_core_section_close_out)
            )
            "property_common_area_inspection" -> listOf(
                spec("common_area_condition", R.string.sr_core_item_common_area_condition, true, 2, "facility_areas", R.string.sr_core_section_facility_areas),
                spec("fire_life_safety", R.string.sr_core_item_fire_life_safety, true, 2, "life_safety", R.string.sr_core_section_life_safety),
                spec("lighting_signage", R.string.sr_core_item_lighting_signage, true, 1, "building_services", R.string.sr_core_section_building_services),
                spec("elevator_equipment", R.string.sr_core_item_elevator_equipment, false, 1, "building_services", R.string.sr_core_section_building_services),
                spec("cleaning_landscape", R.string.sr_core_item_cleaning_landscape, false, 1, "maintenance", R.string.sr_core_section_maintenance),
                spec("risk_evidence", R.string.sr_core_item_risk_evidence_hazard, true, 1, "evidence", R.string.sr_core_section_evidence)
            )
            "pool_pre_opening_acceptance" -> listOf(
                spec("pool_structure", R.string.sr_core_item_pool_structure, true, 2, "pool_area", R.string.sr_core_section_pool_area),
                spec("water_quality", R.string.sr_core_item_water_quality, true, 1, "water_quality", R.string.sr_core_section_water_quality),
                spec("circulation_filtration", R.string.sr_core_item_circulation_filtration, true, 2, "equipment", R.string.sr_core_section_equipment),
                spec("drain_suction_safety", R.string.sr_core_item_drain_suction_safety, true, 1, "safety", R.string.sr_core_section_safety),
                spec("safety_signage_rescue", R.string.sr_core_item_safety_signage_rescue, true, 2, "safety", R.string.sr_core_section_safety),
                spec("lighting_electrical", R.string.sr_core_item_lighting_electrical, false, 1, "equipment", R.string.sr_core_section_equipment),
                spec("access_barriers", R.string.sr_core_item_access_barriers, true, 2, "access", R.string.sr_core_section_access),
                spec("equipment_room", R.string.sr_core_item_equipment_room, true, 2, "equipment", R.string.sr_core_section_equipment)
            )
            "site_safety_inspection" -> listOf(
                spec("ppe_compliance", R.string.sr_core_item_ppe_compliance, true, 2, "people_safety", R.string.sr_core_section_people_safety),
                spec("access_control", R.string.sr_core_item_access_control, true, 1, "site_control", R.string.sr_core_section_site_control),
                spec("fire_safety", R.string.sr_core_item_fire_safety, true, 2, "emergency_safety", R.string.sr_core_section_emergency_safety),
                spec("working_at_height", R.string.sr_core_item_working_at_height, true, 2, "high_risk_work", R.string.sr_core_section_high_risk_work),
                spec("electrical_safety", R.string.sr_core_item_electrical_safety, true, 1, "high_risk_work", R.string.sr_core_section_high_risk_work),
                spec("housekeeping", R.string.sr_core_item_housekeeping, false, 1, "site_control", R.string.sr_core_section_site_control),
                spec("equipment_guarding", R.string.sr_core_item_equipment_guarding, true, 2, "equipment_safety", R.string.sr_core_section_equipment_safety),
                spec("risk_evidence", R.string.sr_core_item_risk_evidence_corrective, true, 1, "evidence", R.string.sr_core_section_evidence)
            )
            "incident_report" -> listOf(
                spec("incident_overview", R.string.sr_core_item_incident_overview, true, 1, "incident_details", R.string.sr_core_section_incident_details),
                spec("people_involved", R.string.sr_core_item_people_involved, true, 1, "people", R.string.sr_core_section_people),
                spec("injury_damage", R.string.sr_core_item_injury_damage, true, 2, "impact", R.string.sr_core_section_impact),
                spec("scene_evidence", R.string.sr_core_item_scene_evidence, true, 3, "evidence", R.string.sr_core_section_evidence),
                spec("immediate_action", R.string.sr_core_item_immediate_action, true, 1, "response", R.string.sr_core_section_response),
                spec("root_cause", R.string.sr_core_item_root_cause, false, 1, "analysis", R.string.sr_core_section_analysis),
                spec("follow_up", R.string.sr_core_item_follow_up, true, 1, "follow_up", R.string.sr_core_section_follow_up)
            )
            else -> listOf(
                spec("site_condition", R.string.sr_core_item_site_condition, true, 2, "general_inspection", R.string.sr_core_section_general_inspection),
                spec("safety_controls", R.string.sr_core_item_safety_controls, true, 2, "general_inspection", R.string.sr_core_section_general_inspection),
                spec("equipment_services", R.string.sr_core_item_equipment_services, true, 1, "systems", R.string.sr_core_section_systems),
                spec("drainage_leaks", R.string.sr_core_item_drainage_leaks, false, 1, "systems", R.string.sr_core_section_systems),
                spec("photo_evidence", R.string.sr_core_item_photo_evidence, true, 1, "evidence", R.string.sr_core_section_evidence)
            )
        }.mapIndexed { index, item -> item.copy(sortOrder = index) }
    }

    private fun spec(
        key: String,
        @StringRes titleRes: Int,
        required: Boolean,
        minPhotoCount: Int,
        sectionKey: String,
        @StringRes sectionTitleRes: Int
    ): InspectionItemSpec {
        return InspectionItemSpec(
            sectionKey = sectionKey,
            sectionTitle = appContext.getString(sectionTitleRes),
            key = key,
            title = appContext.getString(titleRes),
            required = required,
            photoRequired = minPhotoCount > 0,
            minPhotoCount = minPhotoCount
        )
    }

    private data class InspectionItemSpec(
        val sectionKey: String = "default",
        val sectionTitle: String = "Inspection Items",
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

    private fun String.isDoneStatus(): Boolean =
        this == InspectionItemEntity.STATUS_DONE ||
            this == InspectionItemEntity.STATUS_RECTIFIED ||
            this == InspectionItemEntity.STATUS_RECHECK_PASS

    companion object {
        @Volatile
        private var instance: InspectionRecordRepository? = null

        fun getInstance(context: Context): InspectionRecordRepository {
            return instance ?: synchronized(this) {
                instance ?: InspectionRecordRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
