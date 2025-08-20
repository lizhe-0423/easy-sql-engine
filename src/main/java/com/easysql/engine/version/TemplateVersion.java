package com.easysql.engine.version;

import com.easysql.engine.model.Template;

/**
 * 模板版本记录
 */
public class TemplateVersion {
    
    private final String templateId;
    private final String version;
    private final Template template;
    private final String comment;
    private final long timestamp;
    
    public TemplateVersion(String templateId, String version, Template template, String comment, long timestamp) {
        this.templateId = templateId;
        this.version = version;
        this.template = template;
        this.comment = comment;
        this.timestamp = timestamp;
    }
    
    public String getTemplateId() {
        return templateId;
    }
    
    public String getVersion() {
        return version;
    }
    
    public Template getTemplate() {
        return template;
    }
    
    public String getComment() {
        return comment;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "TemplateVersion{" +
                "templateId='" + templateId + '\'' +
                ", version='" + version + '\'' +
                ", comment='" + comment + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}