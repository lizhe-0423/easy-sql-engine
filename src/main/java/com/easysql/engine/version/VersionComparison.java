package com.easysql.engine.version;

import com.easysql.engine.model.Template;

/**
 * 版本比较结果
 * 为简化实现，这里主要记录两版本的基本元信息，
 * 后续可扩展字段级差异、SQL结构差异等。
 */
public class VersionComparison {
    private final String templateId;
    private final String version1;
    private final String version2;
    private final Template template1;
    private final Template template2;

    public VersionComparison(String templateId, String version1, String version2, Template template1, Template template2) {
        this.templateId = templateId;
        this.version1 = version1;
        this.version2 = version2;
        this.template1 = template1;
        this.template2 = template2;
    }

    public String getTemplateId() { return templateId; }
    public String getVersion1() { return version1; }
    public String getVersion2() { return version2; }
    public Template getTemplate1() { return template1; }
    public Template getTemplate2() { return template2; }
}