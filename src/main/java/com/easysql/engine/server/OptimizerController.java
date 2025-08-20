package com.easysql.engine.server;

import com.easysql.engine.optimizer.OptimizationResult;
import com.easysql.engine.optimizer.SQLOptimizer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * SQL 优化分析控制器
 * - GET /optimizer 页面：输入SQL → 调用后端分析 → 展示结果
 * - POST /api/optimizer/analyze：JSON接口，入参为 {"sql":"..."}
 */
@Controller
public class OptimizerController {

    private final SQLOptimizer optimizer = new SQLOptimizer();

    /**
     * 提供简单的前端页面（纯HTML），便于手动体验
     */
    @GetMapping(value = "/optimizer", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String page() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"zh\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "  <title>SQL 优化分析</title>\n" +
                "  <style>\n" +
                "    :root{--bg:#0f172a;--card:#0b1220;--muted:#94a3b8;--text:#e2e8f0;--primary:#22c55e;--primary-700:#16a34a;--border:#1e293b;--code:#0a0f1c}\n" +
                "    *{box-sizing:border-box} body{margin:0;background:radial-gradient(1200px 600px at 20% -10%,rgba(34,197,94,.15),transparent),radial-gradient(1200px 600px at 110% 20%,rgba(59,130,246,.12),transparent),var(--bg);color:var(--text);font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,'PingFang SC','Microsoft YaHei',sans-serif;line-height:1.6} \n" +
                "    .container{max-width:980px;margin:40px auto;padding:0 20px} .card{background:linear-gradient(180deg,#0d1627,#0a1220);border:1px solid var(--border);border-radius:14px;box-shadow:0 10px 30px rgba(0,0,0,.3);padding:22px 22px} \n" +
                "    h1{margin:0 0 6px;font-size:24px;letter-spacing:.2px} .sub{margin:0 0 18px;color:var(--muted)} .row{margin-top:16px} \n" +
                "    textarea{width:100%;min-height:160px;resize:vertical;border-radius:10px;border:1px solid var(--border);background:var(--code);color:var(--text);padding:12px 14px;font-family:ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,monospace;font-size:14px;outline:none;box-shadow:inset 0 0 0 1px rgba(255,255,255,.02)}\n" +
                "    label{display:block;margin-bottom:8px;color:#cbd5e1} .actions{display:flex;gap:10px;align-items:center;margin-top:12px} \n" +
                "    button{background:var(--primary);border:none;color:#062a11;padding:10px 16px;border-radius:10px;font-weight:600;cursor:pointer;transition:.2s;box-shadow:0 6px 18px rgba(34,197,94,.25)} button:hover{background:var(--primary-700)} button:disabled{opacity:.6;cursor:not-allowed} \n" +
                "    .grid{display:grid;grid-template-columns:1fr;gap:14px;margin-top:18px} @media (min-width:960px){.grid{grid-template-columns:1fr 1fr}} \n" +
                "    .panel{background:#0a0f1c;border:1px solid var(--border);border-radius:12px;padding:12px 12px} .panel h3{margin:0 0 8px;font-size:16px;color:#cbd5e1} \n" +
                "    pre{margin:0;background:transparent;color:#d1e7dd;white-space:pre-wrap;word-break:break-word} ul{margin:0;padding-left:18px} li{margin:4px 0} \n" +
                "    .muted{color:var(--muted)} .kbd{display:inline-block;background:#111827;border:1px solid #374151;color:#e5e7eb;border-radius:6px;padding:0 6px;font-size:12px;margin-left:6px} \n" +
                "    .toolbar{display:flex;gap:8px;align-items:center;margin-top:8px} .copy{background:#111827;color:#e5e7eb;border:1px solid #374151;box-shadow:none} .copy:hover{filter:brightness(1.1)}\n" +
                "    .toast{position:fixed;top:20px;right:20px;background:#111827;border:1px solid #334155;color:#e5e7eb;padding:10px 14px;border-radius:10px;opacity:0;transform:translateY(-8px);transition:.2s} .toast.show{opacity:1;transform:none}\n" +
                "    .spinner{display:inline-block;width:16px;height:16px;border:2px solid rgba(255,255,255,.3);border-top-color:#fff;border-radius:50%;animation:spin 1s linear infinite;margin-right:8px}@keyframes spin{to{transform:rotate(360deg)}}\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"container\">\n" +
                "    <div class=\"card\">\n" +
                "      <h1>SQL 优化分析 <span class=\"muted\">· 轻量规则引擎</span></h1>\n" +
                "      <p class=\"sub\">输入 SQL，系统将给出优化后的 SQL、优化建议与依据。支持 <span class=\"kbd\">Ctrl</span> + <span class=\"kbd\">Enter</span> 快捷分析</p>\n" +
                "      <div class=\"row\">\n" +
                "        <label for=\"sql\">输入 SQL</label>\n" +
                "        <textarea id=\"sql\" spellcheck=\"false\" placeholder=\"例如：SELECT * FROM orders ORDER BY created_at\"></textarea>\n" +
                "        <div class=\"actions\">\n" +
                "          <button id=\"btn\"><span id=\"loading\" style=\"display:none\" class=\"spinner\"></span>分析</button>\n" +
                "          <span class=\"muted\">建议尽量填写真实列名，有助于更准确的改写示例</span>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      <div class=\"grid\">\n" +
                "        <div class=\"panel\">\n" +
                "          <h3>结论（优化后的 SQL）</h3>\n" +
                "          <pre id=\"optimized\" class=\"code\"></pre>\n" +
                "          <div class=\"toolbar\">\n" +
                "            <button class=\"copy\" data-target=\"optimized\">复制</button>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "        <div class=\"panel\">\n" +
                "          <h3>建议</h3>\n" +
                "          <ul id=\"adviceList\" class=\"muted\"></ul>\n" +
                "        </div>\n" +
                "        <div class=\"panel\" style=\"grid-column:1/-1\">\n" +
                "          <h3>根据</h3>\n" +
                "          <ul id=\"reasonList\" class=\"muted\"></ul>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <div id=\"toast\" class=\"toast\">已复制到剪贴板</div>\n" +
                "  <script>\n" +
                "    const txt=document.getElementById('sql'); const btn=document.getElementById('btn'); const spin=document.getElementById('loading');\n" +
                "    const optimized=document.getElementById('optimized'); const adviceList=document.getElementById('adviceList'); const reasonList=document.getElementById('reasonList');\n" +
                "    function toast(msg){const t=document.getElementById('toast');t.textContent=msg||'已复制到剪贴板';t.classList.add('show');setTimeout(()=>t.classList.remove('show'),1200)}\n" +
                "    function splitLines(s){if(!s)return[];return s.split(';').map(x=>x.trim()).filter(Boolean)}\n" +
                "    async function analyze(){\n" +
                "      const sql=txt.value.trim(); if(!sql){toast('请输入 SQL'); return;} btn.disabled=true; spin.style.display='inline-block';\n" +
                "      try{\n" +
                "        const res=await fetch('/api/optimizer/analyze',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({sql})});\n" +
                "        const data=await res.json();\n" +
                "        optimized.textContent=data.optimizedSQL||'';\n" +
                "        adviceList.innerHTML=''; splitLines(data.advice).forEach(s=>{const li=document.createElement('li');li.textContent=s;adviceList.appendChild(li)});\n" +
                "        reasonList.innerHTML=''; splitLines(data.reason).forEach(s=>{const li=document.createElement('li');li.textContent=s;reasonList.appendChild(li)});\n" +
                "      }catch(e){toast('请求失败，请检查服务是否已启动');}\n" +
                "      finally{btn.disabled=false; spin.style.display='none'}\n" +
                "    }\n" +
                "    btn.onclick=analyze;\n" +
                "    txt.addEventListener('keydown',e=>{if((e.ctrlKey||e.metaKey)&&e.key==='Enter'){analyze()}});\n" +
                "    document.querySelectorAll('.copy').forEach(b=>{b.onclick=()=>{const id=b.getAttribute('data-target');const t=document.getElementById(id).textContent;navigator.clipboard.writeText(t||'');toast()}});\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * JSON 接口：分析SQL并返回结论/建议/根据与优化后SQL
     */
    @PostMapping(value = "/api/optimizer/analyze", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public OptimizationResult analyze(@RequestBody SqlRequest req) {
        return optimizer.analyze(req.sql);
    }

    public static class SqlRequest { public String sql; }
}