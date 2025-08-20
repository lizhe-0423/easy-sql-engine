package com.easysql.engine.server;

import com.easysql.engine.EasySQLEngine;
import com.easysql.engine.TemplateMapper;
import com.easysql.engine.executor.JDBCSQLExecutor;
import com.easysql.engine.executor.SQLExecutor;
import com.easysql.engine.model.Template;
import com.easysql.engine.version.TemplateVersion;
import com.easysql.engine.version.TemplateVersionManager;
import com.easysql.engine.version.VersionComparison;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 模板与SQL服务控制器
 *
 * 功能概览：
 * 1) 模板版本管理：保存新版本、查看历史、查询指定/活跃版本、比较、回滚、删除
 * 2) SQL 构建：基于模板对象或模板JSON生成SQL
 * 3) SQL 执行（演示用）：构建后通过JDBC直连执行查询
 *
 * 统一路径前缀：/api
 * 说明：当前版本管理为进程内存实现，适合单实例/演示场景。
 */
@RestController
@RequestMapping("/api")
public class TemplateController {

    // EasySQLEngine：负责解析模板、构建SQL、收集指标等
    private final EasySQLEngine engine = new EasySQLEngine();
    // 模板版本管理器：用于模板的版本保存、历史查询、回滚与比较等
    private final TemplateVersionManager versionManager = engine.getVersionManager();

    // ========== 版本管理 ==========

    /**
     * 保存模板的新版本（自动生成版本号，并设置为活跃版本）
     * @param template 模板对象（请求体JSON）
     * @param comment  版本备注（可选，query 参数）
     * @return 新保存的模板版本信息（包含 templateId、version、timestamp、comment、template）
     */
    @PostMapping(value = "/templates/version", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TemplateVersion saveVersion(@RequestBody Template template, @RequestParam(value = "comment", required = false) String comment) {
        return versionManager.saveVersion(template, comment);
    }

    /**
     * 获取指定模板的全部版本历史
     * @param templateId 模板ID
     * @return 版本历史列表（按保存顺序）
     */
    @GetMapping("/templates/{templateId}/versions")
    public List<TemplateVersion> versions(@PathVariable String templateId) {
        return versionManager.getVersionHistory(templateId);
    }

    /**
     * 获取当前已保存的全部模板ID
     * @return 模板ID集合
     */
    @GetMapping("/templates")
    public Set<String> templateIds() {
        return versionManager.getAllTemplateIds();
    }

    /**
     * 获取指定模板的当前活跃版本对应的模板内容
     * @param templateId 模板ID
     * @return 活跃版本的模板
     */
    @GetMapping("/templates/{templateId}/active")
    public Template active(@PathVariable String templateId) {
        return versionManager.getActiveTemplate(templateId);
    }

    /**
     * 获取指定模板的当前活跃版本号
     * @param templateId 模板ID
     * @return 当前活跃版本号，如 1.0.0
     */
    @GetMapping("/templates/{templateId}/active/version")
    public String activeVersion(@PathVariable String templateId) {
        return versionManager.getActiveVersion(templateId);
    }

    /**
     * 将指定模板的活跃版本回滚到给定版本号
     * @param templateId 模板ID
     * @param version    目标版本号
     * @return 回滚是否成功
     */
    @PostMapping("/templates/{templateId}/rollback/{version}")
    public boolean rollback(@PathVariable String templateId, @PathVariable String version) {
        return versionManager.rollbackToVersion(templateId, version);
    }

    /**
     * 比较同一模板的两个版本，返回对比结果（包含v1/v2对应的模板，便于前端做差异展示）
     * @param templateId 模板ID
     * @param v1         版本号1
     * @param v2         版本号2
     * @return 版本对比结果
     */
    @GetMapping("/templates/{templateId}/compare")
    public VersionComparison compare(@PathVariable String templateId,
                                     @RequestParam("v1") String v1,
                                     @RequestParam("v2") String v2) {
        return versionManager.compareVersions(templateId, v1, v2);
    }

    /**
     * 获取指定模板的某个版本
     * @param templateId 模板ID
     * @param version    版本号
     * @return 指定版本的模板
     */
    @GetMapping("/templates/{templateId}/versions/{version}")
    public Template getTemplateByVersion(@PathVariable String templateId, @PathVariable String version) {
        return versionManager.getTemplate(templateId, version);
    }

    /**
     * 删除指定模板的全部版本与活跃指针
     * @param templateId 模板ID
     * @return 删除是否成功
     */
    @DeleteMapping("/templates/{templateId}")
    public boolean deleteTemplate(@PathVariable String templateId) {
        return versionManager.deleteTemplate(templateId);
    }

    // ========== SQL 构建 ==========

    @PostMapping(value = "/build", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EasySQLEngine.QueryResult build(@RequestBody Template template) {
        long start = System.currentTimeMillis();
        String sql = engine.buildSQL(template);
        long end = System.currentTimeMillis();
        return new EasySQLEngine.QueryResult(sql, template, end - start);
    }

    @PostMapping(value = "/build-json", consumes = MediaType.TEXT_PLAIN_VALUE)
    public EasySQLEngine.QueryResult buildFromJson(@RequestBody String json) throws Exception {
        Template t = TemplateMapper.fromJson(json);
        return engine.parseAndBuild(t);
    }

    // ========== SQL 执行（Demo：使用JDBC直连，请在params中传入url,user,password） ==========

    @PostMapping(value = "/execute", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SQLExecutor.QueryResult execute(@RequestBody ExecuteRequest req) throws Exception {
        Template t = req.template;
        String sql = engine.buildSQL(t);
        try (Connection conn = DriverManager.getConnection(req.url, req.user, req.password)) {
            JDBCSQLExecutor executor = engine.createExecutor();
            Map<String, Object> params = req.params == null ? new HashMap<>() : req.params;
            return executor.executeQuery(conn, sql, t, params);
        }
    }

    /**
     * 执行请求体
     * - template: 模板对象
     * - url/user/password: JDBC 连接信息
     * - params: 运行时参数（如SQL中的命名参数）
     */
    public static class ExecuteRequest {
        // 模板对象
        public Template template;
        // JDBC URL，如 jdbc:mysql://host:3306/db
        public String url;
        // 数据库用户名
        public String user;
        // 数据库密码
        public String password;
        // 运行时参数
        public Map<String, Object> params;
    }
}