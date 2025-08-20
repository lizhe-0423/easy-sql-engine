package com.easysql.engine.version;

import com.easysql.engine.model.Template;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模板版本管理器
 * 负责模板的版本控制、历史记录和版本比较
 */
public class TemplateVersionManager {
    
    // 模板ID -> 版本历史列表（按版本号排序）
    private final Map<String, List<TemplateVersion>> templateVersions = new ConcurrentHashMap<>();
    
    // 模板ID -> 当前活跃版本号
    private final Map<String, String> activeVersions = new ConcurrentHashMap<>();
    
    /**
     * 保存新版本的模板
     */
    public TemplateVersion saveVersion(Template template, String comment) {
        if (template.id == null || template.id.trim().isEmpty()) {
            throw new IllegalArgumentException("Template ID is required");
        }
        
        String templateId = template.id;
        String newVersion = generateNextVersion(templateId);
        
        // 设置模板版本
        template.version = newVersion;
        
        // 创建版本记录
        TemplateVersion templateVersion = new TemplateVersion(
            templateId,
            newVersion,
            template,
            comment,
            System.currentTimeMillis()
        );
        
        // 保存到版本历史
        templateVersions.computeIfAbsent(templateId, k -> new ArrayList<>()).add(templateVersion);
        
        // 更新活跃版本
        activeVersions.put(templateId, newVersion);
        
        return templateVersion;
    }
    
    /**
     * 获取指定版本的模板
     */
    public Template getTemplate(String templateId, String version) {
        List<TemplateVersion> versions = templateVersions.get(templateId);
        if (versions == null) {
            return null;
        }
        
        return versions.stream()
            .filter(v -> v.getVersion().equals(version))
            .map(TemplateVersion::getTemplate)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 获取当前活跃版本的模板
     */
    public Template getActiveTemplate(String templateId) {
        String activeVersion = activeVersions.get(templateId);
        if (activeVersion == null) {
            return null;
        }
        return getTemplate(templateId, activeVersion);
    }
    
    /**
     * 获取模板的所有版本历史
     */
    public List<TemplateVersion> getVersionHistory(String templateId) {
        List<TemplateVersion> versions = templateVersions.get(templateId);
        return versions != null ? new ArrayList<>(versions) : new ArrayList<>();
    }
    
    /**
     * 回滚到指定版本
     */
    public boolean rollbackToVersion(String templateId, String version) {
        Template template = getTemplate(templateId, version);
        if (template == null) {
            return false;
        }
        
        activeVersions.put(templateId, version);
        return true;
    }
    
    /**
     * 比较两个版本的差异
     */
    public VersionComparison compareVersions(String templateId, String version1, String version2) {
        Template t1 = getTemplate(templateId, version1);
        Template t2 = getTemplate(templateId, version2);
        
        if (t1 == null || t2 == null) {
            throw new IllegalArgumentException("Version not found");
        }
        
        return new VersionComparison(templateId, version1, version2, t1, t2);
    }
    
    /**
     * 获取当前活跃版本号
     */
    public String getActiveVersion(String templateId) {
        return activeVersions.get(templateId);
    }
    
    /**
     * 获取所有模板ID
     */
    public Set<String> getAllTemplateIds() {
        return new HashSet<>(templateVersions.keySet());
    }
    
    /**
     * 删除模板的所有版本
     */
    public boolean deleteTemplate(String templateId) {
        templateVersions.remove(templateId);
        activeVersions.remove(templateId);
        return true;
    }
    
    /**
     * 生成下一个版本号 (简单的递增版本号)
     */
    private String generateNextVersion(String templateId) {
        List<TemplateVersion> versions = templateVersions.get(templateId);
        if (versions == null || versions.isEmpty()) {
            return "1.0.0";
        }
        
        // 获取最新版本号并递增
        String lastVersion = versions.get(versions.size() - 1).getVersion();
        return incrementVersion(lastVersion);
    }
    
    /**
     * 递增版本号 (patch版本递增)
     */
    private String incrementVersion(String version) {
        String[] parts = version.split("\\.");
        if (parts.length != 3) {
            return "1.0.0";
        }
        
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]);
            
            return major + "." + minor + "." + (patch + 1);
        } catch (NumberFormatException e) {
            return "1.0.0";
        }
    }
}