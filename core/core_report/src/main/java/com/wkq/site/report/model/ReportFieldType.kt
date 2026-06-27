package com.wkq.site.report.model

/**
 * 自定义字段类型。
 *
 * 模板渲染、表单输入控件和字段校验都可以根据该类型决定展示方式。
 */
enum class ReportFieldType {
    /** 普通文本。 */
    TEXT,

    /** 数字。 */
    NUMBER,

    /** 日期，不包含具体时间。 */
    DATE,

    /** 日期时间。 */
    DATE_TIME,

    /** 位置信息，例如经纬度或地址。 */
    LOCATION,

    /** 图片路径或图片资源。 */
    IMAGE,

    /** 布尔值，例如是否合格。 */
    BOOLEAN,

    /** 单选字段。 */
    SINGLE_CHOICE,

    /** 多选字段。 */
    MULTI_CHOICE,

    /** 签名字段，例如客户签名、验收人签名。 */
    SIGNATURE
}
