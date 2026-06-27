package com.wkq.site.report.template

import android.content.Context
import com.wkq.site.report.model.ReportEdition
import com.wkq.site.report.model.ReportFieldOption
import com.wkq.site.report.model.ReportFieldType
import com.wkq.site.report.model.ReportSignerRole
import com.wkq.site.report.model.ReportTemplate
import com.wkq.site.report.model.ReportTemplateDefinition
import com.wkq.site.report.model.ReportTemplateField

/**
 * 内置报告模板预设。
 *
 * 模板只定义字段和签名角色，不保存用户填写值。
 */
object ReportTemplatePresets {
    const val SITE_INSPECTION = "site_inspection"
    const val CONSTRUCTION_INSPECTION = "construction_inspection"
    const val FIELD_SERVICE_REPORT = "field_service_report"
    const val SAFETY_INSPECTION = "safety_inspection"
    const val BUILDING_INSPECTION = "building_inspection"
    const val FIELD_VERIFICATION = "field_verification"

    val all: List<ReportTemplateDefinition> = listOf(
        siteInspection(),
        constructionInspection(),
        fieldServiceReport(),
        safetyInspection(),
        buildingInspection(),
        fieldVerification()
    )

    fun getById(templateId: String): ReportTemplateDefinition? {
        return all.firstOrNull { it.id == templateId }
    }

    fun localizedAll(context: Context): List<ReportTemplateDefinition> {
        return all.map { it.localized(context) }
    }

    fun getById(context: Context, templateId: String): ReportTemplateDefinition? {
        return getById(templateId)?.localized(context)
    }

    private fun ReportTemplateDefinition.localized(context: Context): ReportTemplateDefinition {
        return copy(
            template = template.copy(
                name = context.getString(templateNameRes(id)),
                description = context.getString(templateDescriptionRes(id))
            ),
            reportFields = reportFields.map { it.localized(context) },
            photoFields = photoFields.map { it.localized(context) },
            signerRoles = signerRoles.map { it.localized(context) }
        )
    }

    private fun ReportTemplateField.localized(context: Context): ReportTemplateField {
        return copy(
            label = context.getString(fieldLabelRes(key)),
            group = group?.let { context.getString(groupLabelRes(it)) },
            options = options.map { it.localized(context) }
        )
    }

    private fun ReportFieldOption.localized(context: Context): ReportFieldOption {
        return copy(label = context.getString(optionLabelRes(value)))
    }

    private fun ReportSignerRole.localized(context: Context): ReportSignerRole {
        return copy(
            label = context.getString(signerLabelRes(role)),
            consentText = context.getString(com.wkq.site.report.R.string.sr_report_template_consent),
            customFields = customFields.map { it.localized(context) }
        )
    }

    private fun siteInspection(): ReportTemplateDefinition {
        return definition(
            id = SITE_INSPECTION,
            name = "Site Inspection",
            description = "General site inspection report with observations, issues and photos.",
            reportFields = commonReportFields() + listOf(
                field("inspection_scope", "Inspection Scope", order = 100, group = GROUP_INSPECTION),
                choice("overall_status", "Overall Status", statusOptions(), required = true, order = 110, group = GROUP_SUMMARY),
                field("summary", "Summary", order = 120, group = GROUP_SUMMARY)
            ),
            photoFields = commonPhotoFields(),
            signerRoles = listOf(
                signer("inspector", "Inspector", required = true),
                signer("client_representative", "Client Representative")
            )
        )
    }

    private fun constructionInspection(): ReportTemplateDefinition {
        return definition(
            id = CONSTRUCTION_INSPECTION,
            name = "Construction Inspection",
            description = "Construction progress and quality inspection report.",
            reportFields = commonReportFields() + listOf(
                field("contractor", "Contractor", required = true, order = 100, group = GROUP_PROJECT),
                field("construction_phase", "Construction Phase", required = true, order = 110, group = GROUP_INSPECTION),
                field("drawing_reference", "Drawing / Spec Reference", order = 120, group = GROUP_INSPECTION),
                choice("workmanship_status", "Workmanship Status", statusOptions(), required = true, order = 130, group = GROUP_SUMMARY)
            ),
            photoFields = commonPhotoFields() + listOf(
                field("trade", "Trade", order = 100, group = GROUP_PHOTO),
                field("material_batch", "Material / Batch", order = 110, group = GROUP_PHOTO)
            ),
            signerRoles = listOf(
                signer("inspector", "Inspector", required = true),
                signer("site_supervisor", "Site Supervisor", required = true),
                signer("contractor_representative", "Contractor Representative")
            )
        )
    }

    private fun fieldServiceReport(): ReportTemplateDefinition {
        return definition(
            id = FIELD_SERVICE_REPORT,
            name = "Field Service Report",
            description = "On-site installation, repair, maintenance or commissioning report.",
            reportFields = commonReportFields() + listOf(
                field("customer_name", "Customer Name", required = true, order = 100, group = GROUP_PROJECT),
                choice("service_type", "Service Type", serviceTypeOptions(), required = true, order = 110, group = GROUP_SERVICE),
                field("equipment_model", "Equipment Model", order = 120, group = GROUP_SERVICE),
                field("serial_number", "Serial Number", order = 130, group = GROUP_SERVICE),
                field("arrival_time", "Arrival Time", type = ReportFieldType.DATE_TIME, order = 140, group = GROUP_SERVICE),
                field("departure_time", "Departure Time", type = ReportFieldType.DATE_TIME, order = 150, group = GROUP_SERVICE),
                field("work_performed", "Work Performed", required = true, order = 160, group = GROUP_SUMMARY)
            ),
            photoFields = commonPhotoFields(),
            signerRoles = listOf(
                signer("technician", "Technician", required = true),
                signer("customer", "Customer", required = true)
            )
        )
    }

    private fun safetyInspection(): ReportTemplateDefinition {
        return definition(
            id = SAFETY_INSPECTION,
            name = "Safety Inspection",
            description = "Safety hazard inspection report with risk level and corrective action tracking.",
            reportFields = commonReportFields() + listOf(
                field("safety_standard", "Safety Standard / Checklist", order = 100, group = GROUP_INSPECTION),
                choice("overall_risk", "Overall Risk", severityOptions(), required = true, order = 110, group = GROUP_SUMMARY),
                field("immediate_action_required", "Immediate Action Required", type = ReportFieldType.BOOLEAN, order = 120, group = GROUP_SUMMARY)
            ),
            photoFields = commonPhotoFields() + listOf(
                choice("hazard_level", "Hazard Level", severityOptions(), required = true, order = 100, group = GROUP_CORRECTIVE),
                field("responsible_party", "Responsible Party", order = 110, group = GROUP_CORRECTIVE),
                field("due_date", "Due Date", type = ReportFieldType.DATE, order = 120, group = GROUP_CORRECTIVE)
            ),
            signerRoles = listOf(
                signer("safety_inspector", "Safety Inspector", required = true),
                signer("site_supervisor", "Site Supervisor", required = true),
                signer("client_representative", "Client Representative")
            )
        )
    }

    private fun buildingInspection(): ReportTemplateDefinition {
        return definition(
            id = BUILDING_INSPECTION,
            name = "Building Inspection",
            description = "Building condition inspection report for rooms, components and defects.",
            reportFields = commonReportFields() + listOf(
                field("building_name", "Building Name", required = true, order = 100, group = GROUP_PROJECT),
                field("building_type", "Building Type", order = 110, group = GROUP_PROJECT),
                field("inspection_standard", "Inspection Standard", order = 120, group = GROUP_INSPECTION),
                choice("condition_rating", "Condition Rating", conditionOptions(), required = true, order = 130, group = GROUP_SUMMARY)
            ),
            photoFields = commonPhotoFields() + listOf(
                field("floor", "Floor", order = 100, group = GROUP_PHOTO),
                field("room", "Room / Unit", order = 110, group = GROUP_PHOTO),
                field("building_component", "Building Component", required = true, order = 120, group = GROUP_PHOTO),
                choice("defect_severity", "Defect Severity", severityOptions(), order = 130, group = GROUP_CORRECTIVE)
            ),
            signerRoles = listOf(
                signer("building_inspector", "Building Inspector", required = true),
                signer("owner_or_agent", "Owner / Agent")
            )
        )
    }

    private fun fieldVerification(): ReportTemplateDefinition {
        return definition(
            id = FIELD_VERIFICATION,
            name = "Field Verification",
            description = "Field verification report with GPS, timestamp, photo evidence and signature confirmation.",
            reportFields = commonReportFields() + listOf(
                field("verification_object", "Verification Object", required = true, order = 100, group = GROUP_VERIFICATION),
                field("verification_criteria", "Verification Criteria", required = true, order = 110, group = GROUP_VERIFICATION),
                choice("verification_result", "Verification Result", verificationOptions(), required = true, order = 120, group = GROUP_SUMMARY),
                field("reference_number", "Reference Number", order = 130, group = GROUP_VERIFICATION)
            ),
            photoFields = commonPhotoFields() + listOf(
                field("gps_required", "GPS Required", type = ReportFieldType.BOOLEAN, defaultValue = "true", order = 100, group = GROUP_VERIFICATION),
                field("timestamp_required", "Timestamp Required", type = ReportFieldType.BOOLEAN, defaultValue = "true", order = 110, group = GROUP_VERIFICATION),
                field("photo_signature_required", "Photo Signature Required", type = ReportFieldType.BOOLEAN, defaultValue = "true", order = 120, group = GROUP_VERIFICATION)
            ),
            signerRoles = listOf(
                signer("verifier", "Verifier", required = true),
                signer("requestor", "Requestor / Client", required = true),
                signer("witness", "Witness")
            ),
            metadata = mapOf("requires_photo_integrity" to "true")
        )
    }

    private fun definition(
        id: String,
        name: String,
        description: String,
        reportFields: List<ReportTemplateField>,
        photoFields: List<ReportTemplateField>,
        signerRoles: List<ReportSignerRole>,
        metadata: Map<String, String> = emptyMap()
    ): ReportTemplateDefinition {
        return ReportTemplateDefinition(
            template = ReportTemplate(
                id = id,
                name = name,
                assetPath = "report_templates/$id.html",
                minEdition = ReportEdition.BASIC,
                supportsCustomFields = true,
                description = description,
                metadata = metadata + mapOf("category" to "field_report")
            ),
            templateVersion = 1,
            reportFields = reportFields.sortedBy { it.order },
            photoFields = photoFields.sortedBy { it.order },
            signerRoles = signerRoles.sortedBy { it.order },
            metadata = metadata
        )
    }

    private fun commonReportFields(): List<ReportTemplateField> {
        return listOf(
            field("report_number", "Report Number", order = 10, group = GROUP_PROJECT),
            field("project_name", "Project Name", required = true, order = 20, group = GROUP_PROJECT),
            field("site_address", "Site Address", required = true, order = 30, group = GROUP_PROJECT),
            field("inspection_date", "Inspection Date", type = ReportFieldType.DATE, required = true, order = 40, group = GROUP_INSPECTION),
            field("inspector_name", "Inspector Name", required = true, order = 50, group = GROUP_INSPECTION),
            field("company_name", "Company Name", order = 60, group = GROUP_PROJECT),
            field("weather", "Weather", order = 70, group = GROUP_INSPECTION),
            field("conclusion", "Conclusion", required = true, order = 80, group = GROUP_SUMMARY)
        )
    }

    private fun commonPhotoFields(): List<ReportTemplateField> {
        return listOf(
            field("area", "Area / Location", required = true, order = 10, group = GROUP_PHOTO),
            field("category", "Category", order = 20, group = GROUP_PHOTO),
            field("item", "Inspection Item", order = 30, group = GROUP_PHOTO),
            choice("status", "Status", statusOptions(), required = true, order = 40, group = GROUP_PHOTO),
            choice("severity", "Severity", severityOptions(), order = 50, group = GROUP_CORRECTIVE),
            field("issue_description", "Issue Description", order = 60, group = GROUP_CORRECTIVE),
            field("corrective_action", "Corrective Action", order = 70, group = GROUP_CORRECTIVE),
            field("closed", "Closed", type = ReportFieldType.BOOLEAN, order = 80, group = GROUP_CORRECTIVE)
        )
    }

    private fun field(
        key: String,
        label: String,
        type: ReportFieldType = ReportFieldType.TEXT,
        required: Boolean = false,
        order: Int,
        group: String,
        defaultValue: String = ""
    ): ReportTemplateField {
        return ReportTemplateField(
            key = key,
            label = label,
            type = type,
            required = required,
            order = order,
            group = group,
            defaultValue = defaultValue
        )
    }

    private fun choice(
        key: String,
        label: String,
        options: List<ReportFieldOption>,
        required: Boolean = false,
        order: Int,
        group: String
    ): ReportTemplateField {
        return ReportTemplateField(
            key = key,
            label = label,
            type = ReportFieldType.SINGLE_CHOICE,
            required = required,
            order = order,
            group = group,
            options = options
        )
    }

    private fun signer(role: String, label: String, required: Boolean = false): ReportSignerRole {
        return ReportSignerRole(
            role = role,
            label = label,
            required = required,
            order = signerOrder(role),
            consentText = "I confirm that the information in this report is accurate to the best of my knowledge."
        )
    }

    private fun statusOptions(): List<ReportFieldOption> {
        return listOf(
            option("pass", "Pass"),
            option("fail", "Fail"),
            option("na", "N/A")
        )
    }

    private fun severityOptions(): List<ReportFieldOption> {
        return listOf(
            option("low", "Low"),
            option("medium", "Medium"),
            option("high", "High"),
            option("critical", "Critical")
        )
    }

    private fun serviceTypeOptions(): List<ReportFieldOption> {
        return listOf(
            option("installation", "Installation"),
            option("repair", "Repair"),
            option("maintenance", "Maintenance"),
            option("commissioning", "Commissioning"),
            option("inspection", "Inspection")
        )
    }

    private fun conditionOptions(): List<ReportFieldOption> {
        return listOf(
            option("good", "Good"),
            option("fair", "Fair"),
            option("poor", "Poor"),
            option("unsafe", "Unsafe")
        )
    }

    private fun verificationOptions(): List<ReportFieldOption> {
        return listOf(
            option("verified", "Verified"),
            option("not_verified", "Not Verified"),
            option("partial", "Partially Verified")
        )
    }

    private fun option(value: String, label: String): ReportFieldOption {
        return ReportFieldOption(value = value, label = label)
    }

    private fun signerOrder(role: String): Int {
        return when (role) {
            "inspector", "technician", "safety_inspector", "building_inspector", "verifier" -> 10
            "site_supervisor" -> 20
            "customer", "client_representative", "requestor", "owner_or_agent" -> 30
            "contractor_representative", "witness" -> 40
            else -> 100
        }
    }

    private fun templateNameRes(templateId: String): Int {
        return when (templateId) {
            SITE_INSPECTION -> com.wkq.site.report.R.string.sr_report_template_site_inspection
            CONSTRUCTION_INSPECTION -> com.wkq.site.report.R.string.sr_report_template_construction_inspection
            FIELD_SERVICE_REPORT -> com.wkq.site.report.R.string.sr_report_template_field_service
            SAFETY_INSPECTION -> com.wkq.site.report.R.string.sr_report_template_safety_inspection
            BUILDING_INSPECTION -> com.wkq.site.report.R.string.sr_report_template_building_inspection
            FIELD_VERIFICATION -> com.wkq.site.report.R.string.sr_report_template_field_verification
            else -> com.wkq.site.report.R.string.sr_report_template_site_inspection
        }
    }

    private fun templateDescriptionRes(templateId: String): Int {
        return when (templateId) {
            SITE_INSPECTION -> com.wkq.site.report.R.string.sr_report_template_site_inspection_desc
            CONSTRUCTION_INSPECTION -> com.wkq.site.report.R.string.sr_report_template_construction_inspection_desc
            FIELD_SERVICE_REPORT -> com.wkq.site.report.R.string.sr_report_template_field_service_desc
            SAFETY_INSPECTION -> com.wkq.site.report.R.string.sr_report_template_safety_inspection_desc
            BUILDING_INSPECTION -> com.wkq.site.report.R.string.sr_report_template_building_inspection_desc
            FIELD_VERIFICATION -> com.wkq.site.report.R.string.sr_report_template_field_verification_desc
            else -> com.wkq.site.report.R.string.sr_report_template_site_inspection_desc
        }
    }

    private fun fieldLabelRes(key: String): Int {
        return when (key) {
            "report_number" -> com.wkq.site.report.R.string.sr_report_field_report_number
            "project_name" -> com.wkq.site.report.R.string.sr_report_field_project_name
            "site_address" -> com.wkq.site.report.R.string.sr_report_field_site_address
            "inspection_date" -> com.wkq.site.report.R.string.sr_report_field_inspection_date
            "inspector_name" -> com.wkq.site.report.R.string.sr_report_field_inspector_name
            "company_name" -> com.wkq.site.report.R.string.sr_report_field_company_name
            "weather" -> com.wkq.site.report.R.string.sr_report_field_weather
            "conclusion" -> com.wkq.site.report.R.string.sr_report_field_conclusion
            "inspection_scope" -> com.wkq.site.report.R.string.sr_report_field_inspection_scope
            "overall_status" -> com.wkq.site.report.R.string.sr_report_field_overall_status
            "summary" -> com.wkq.site.report.R.string.sr_report_field_summary
            "contractor" -> com.wkq.site.report.R.string.sr_report_field_contractor
            "construction_phase" -> com.wkq.site.report.R.string.sr_report_field_construction_phase
            "drawing_reference" -> com.wkq.site.report.R.string.sr_report_field_drawing_reference
            "workmanship_status" -> com.wkq.site.report.R.string.sr_report_field_workmanship_status
            "trade" -> com.wkq.site.report.R.string.sr_report_field_trade
            "material_batch" -> com.wkq.site.report.R.string.sr_report_field_material_batch
            "customer_name" -> com.wkq.site.report.R.string.sr_report_field_customer_name
            "service_type" -> com.wkq.site.report.R.string.sr_report_field_service_type
            "equipment_model" -> com.wkq.site.report.R.string.sr_report_field_equipment_model
            "serial_number" -> com.wkq.site.report.R.string.sr_report_field_serial_number
            "arrival_time" -> com.wkq.site.report.R.string.sr_report_field_arrival_time
            "departure_time" -> com.wkq.site.report.R.string.sr_report_field_departure_time
            "work_performed" -> com.wkq.site.report.R.string.sr_report_field_work_performed
            "safety_standard" -> com.wkq.site.report.R.string.sr_report_field_safety_standard
            "overall_risk" -> com.wkq.site.report.R.string.sr_report_field_overall_risk
            "immediate_action_required" -> com.wkq.site.report.R.string.sr_report_field_immediate_action_required
            "hazard_level" -> com.wkq.site.report.R.string.sr_report_field_hazard_level
            "responsible_party" -> com.wkq.site.report.R.string.sr_report_field_responsible_party
            "due_date" -> com.wkq.site.report.R.string.sr_report_field_due_date
            "building_name" -> com.wkq.site.report.R.string.sr_report_field_building_name
            "building_type" -> com.wkq.site.report.R.string.sr_report_field_building_type
            "inspection_standard" -> com.wkq.site.report.R.string.sr_report_field_inspection_standard
            "condition_rating" -> com.wkq.site.report.R.string.sr_report_field_condition_rating
            "floor" -> com.wkq.site.report.R.string.sr_report_field_floor
            "room" -> com.wkq.site.report.R.string.sr_report_field_room
            "building_component" -> com.wkq.site.report.R.string.sr_report_field_building_component
            "defect_severity" -> com.wkq.site.report.R.string.sr_report_field_defect_severity
            "verification_object" -> com.wkq.site.report.R.string.sr_report_field_verification_object
            "verification_criteria" -> com.wkq.site.report.R.string.sr_report_field_verification_criteria
            "verification_result" -> com.wkq.site.report.R.string.sr_report_field_verification_result
            "reference_number" -> com.wkq.site.report.R.string.sr_report_field_reference_number
            "gps_required" -> com.wkq.site.report.R.string.sr_report_field_gps_required
            "timestamp_required" -> com.wkq.site.report.R.string.sr_report_field_timestamp_required
            "photo_signature_required" -> com.wkq.site.report.R.string.sr_report_field_photo_signature_required
            "area" -> com.wkq.site.report.R.string.sr_report_field_area
            "category" -> com.wkq.site.report.R.string.sr_report_field_category
            "item" -> com.wkq.site.report.R.string.sr_report_field_item
            "status" -> com.wkq.site.report.R.string.sr_report_field_status
            "severity" -> com.wkq.site.report.R.string.sr_report_field_severity
            "issue_description" -> com.wkq.site.report.R.string.sr_report_field_issue_description
            "corrective_action" -> com.wkq.site.report.R.string.sr_report_field_corrective_action
            "closed" -> com.wkq.site.report.R.string.sr_report_field_closed
            else -> com.wkq.site.report.R.string.sr_report_field_item
        }
    }

    private fun groupLabelRes(group: String): Int {
        return when (group) {
            GROUP_PROJECT -> com.wkq.site.report.R.string.sr_report_group_project
            GROUP_INSPECTION -> com.wkq.site.report.R.string.sr_report_group_inspection
            GROUP_SERVICE -> com.wkq.site.report.R.string.sr_report_group_service
            GROUP_VERIFICATION -> com.wkq.site.report.R.string.sr_report_group_verification
            GROUP_PHOTO -> com.wkq.site.report.R.string.sr_report_group_photo
            GROUP_CORRECTIVE -> com.wkq.site.report.R.string.sr_report_group_corrective
            GROUP_SUMMARY -> com.wkq.site.report.R.string.sr_report_group_summary
            else -> com.wkq.site.report.R.string.sr_report_group_summary
        }
    }

    private fun optionLabelRes(value: String): Int {
        return when (value) {
            "pass" -> com.wkq.site.report.R.string.sr_report_option_pass
            "fail" -> com.wkq.site.report.R.string.sr_report_option_fail
            "na" -> com.wkq.site.report.R.string.sr_report_option_na
            "low" -> com.wkq.site.report.R.string.sr_report_option_low
            "medium" -> com.wkq.site.report.R.string.sr_report_option_medium
            "high" -> com.wkq.site.report.R.string.sr_report_option_high
            "critical" -> com.wkq.site.report.R.string.sr_report_option_critical
            "installation" -> com.wkq.site.report.R.string.sr_report_option_installation
            "repair" -> com.wkq.site.report.R.string.sr_report_option_repair
            "maintenance" -> com.wkq.site.report.R.string.sr_report_option_maintenance
            "commissioning" -> com.wkq.site.report.R.string.sr_report_option_commissioning
            "inspection" -> com.wkq.site.report.R.string.sr_report_option_inspection
            "good" -> com.wkq.site.report.R.string.sr_report_option_good
            "fair" -> com.wkq.site.report.R.string.sr_report_option_fair
            "poor" -> com.wkq.site.report.R.string.sr_report_option_poor
            "unsafe" -> com.wkq.site.report.R.string.sr_report_option_unsafe
            "verified" -> com.wkq.site.report.R.string.sr_report_option_verified
            "not_verified" -> com.wkq.site.report.R.string.sr_report_option_not_verified
            "partial" -> com.wkq.site.report.R.string.sr_report_option_partial
            else -> com.wkq.site.report.R.string.sr_report_option_na
        }
    }

    private fun signerLabelRes(role: String): Int {
        return when (role) {
            "inspector" -> com.wkq.site.report.R.string.sr_report_signer_inspector
            "client_representative" -> com.wkq.site.report.R.string.sr_report_signer_client_representative
            "site_supervisor" -> com.wkq.site.report.R.string.sr_report_signer_site_supervisor
            "contractor_representative" -> com.wkq.site.report.R.string.sr_report_signer_contractor_representative
            "technician" -> com.wkq.site.report.R.string.sr_report_signer_technician
            "customer" -> com.wkq.site.report.R.string.sr_report_signer_customer
            "safety_inspector" -> com.wkq.site.report.R.string.sr_report_signer_safety_inspector
            "building_inspector" -> com.wkq.site.report.R.string.sr_report_signer_building_inspector
            "owner_or_agent" -> com.wkq.site.report.R.string.sr_report_signer_owner_or_agent
            "verifier" -> com.wkq.site.report.R.string.sr_report_signer_verifier
            "requestor" -> com.wkq.site.report.R.string.sr_report_signer_requestor
            "witness" -> com.wkq.site.report.R.string.sr_report_signer_witness
            else -> com.wkq.site.report.R.string.sr_report_signer_inspector
        }
    }

    private const val GROUP_PROJECT = "Project Information"
    private const val GROUP_INSPECTION = "Inspection Information"
    private const val GROUP_SERVICE = "Service Information"
    private const val GROUP_VERIFICATION = "Verification Information"
    private const val GROUP_PHOTO = "Photo Information"
    private const val GROUP_CORRECTIVE = "Corrective Action"
    private const val GROUP_SUMMARY = "Summary"
}
