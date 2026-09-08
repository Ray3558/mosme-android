package com.mosme.cheat

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var controlBar: LinearLayout
    private lateinit var settingsPanel: LinearLayout
    private lateinit var btnAnswer: Button
    private lateinit var btnAI: Button
    private lateinit var btnSettingsToggle: Button
    private lateinit var btnFetchModels: Button
    private lateinit var btnSaveSettings: Button
    private lateinit var btnCloseSettings: Button
    private lateinit var etBaseUrl: EditText
    private lateinit var etApiKey: EditText
    private lateinit var spinnerModel: Spinner
    private lateinit var cbPrecise: CheckBox
    private lateinit var cbMajority: CheckBox

    private lateinit var gestureDetector: GestureDetectorCompat
    private var tapCount = 0
    private var lastTapTime = 0L

    private val prefs by lazy { getSharedPreferences("mosme_ai", Context.MODE_PRIVATE) }
    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }
    private val scope = MainScope()
    private var modelList = mutableListOf<String>()

    companion object {
        val SYMBOLS = listOf("A", "B", "C", "D", "E", "F")
    }

    private val interceptorJs = """
        (function() {
            if (window.__mosmeIntercepted) return;
            window.__mosmeIntercepted = true;
            window.__mosmeAnswers = null;
            window.__mosmeQuestions = null;
            window.__mosmeQuizId = null;
            window.__mosmeApiLog = [];
            var _log = function(msg) {
                window.__mosmeApiLog.push(msg);
                console.log('[MOSME] ' + msg);
            };
            function tryParse(t) { try { return JSON.parse(t); } catch(e) { return null; } }

            function handleExamData(obj) {
                var data = obj && (obj.Data || obj.data);
                if (!data) return;
                var qs = data.Questions || data.questions;
                var cfg = data.Config || data.config;
                if (!Array.isArray(qs) || !qs.length) return;
                window.__mosmeQuestions = qs;
                window.__mosmeQuizId = cfg && (cfg.QuizId || cfg.quizId);
                _log('題目已儲存: ' + qs.length + ' 題');
                if (window.__mosmeQuizId) {
                    try {
                        var saved = localStorage.getItem('mosme_ans_' + window.__mosmeQuizId);
                        if (saved) { window.__mosmeAnswers = JSON.parse(saved); _log('從快取載入: ' + Object.keys(window.__mosmeAnswers).length + ' 題'); }
                    } catch(e) {}
                }
            }

            function handleScoreReport(obj) {
                var data = obj && (obj.Data || obj.data);
                if (!data) return;
                var qs = null;
                for (var k of ['Questions','questions','QuestionList','questionList','Items','items']) {
                    if (Array.isArray(data[k]) && data[k].length) { qs = data[k]; break; }
                }
                if (!qs) for (var k of Object.keys(data)) { if (Array.isArray(data[k]) && data[k].length) { qs = data[k]; break; } }
                if (!Array.isArray(qs) || !qs.length) return;
                if (!qs[0].Options && !qs[0].options) return;
                var hasTrue = qs.some(function(q) { return (q.Options||q.options||[]).some(function(o){return o.IsAnswer===true;}); });
                if (!hasTrue) return;
                var map = {};
                qs.forEach(function(q) {
                    var qid = q.QuestionId||q.questionId||q.Id||q.id;
                    (q.Options||q.options||[]).forEach(function(o) { if (o.IsAnswer===true) map[qid]=o.ItemId||o.itemId; });
                });
                if (Object.keys(map).length) {
                    window.__mosmeAnswers = map;
                    _log('成績報告答案: ' + Object.keys(map).length + ' 題');
                    if (window.__mosmeQuizId) { try { localStorage.setItem('mosme_ans_'+window.__mosmeQuizId, JSON.stringify(map)); } catch(e){} }
                }
            }

            function handleResponse(url, text) {
                if (!text) return;
                _log('REQ: ' + url.split('?')[0].slice(-60) + ' len=' + text.length);
                if (text[0]!=='{' && text[0]!=='[') return;
                var obj = tryParse(text); if (!obj) return;
                if (/GetExamData|GetExam/i.test(url)) handleExamData(obj);
                else if (/Score|Report|Result|Submit|Review/i.test(url)) handleScoreReport(obj);
            }

            var origFetch = window.fetch;
            window.fetch = function(input, init) {
                var url = typeof input==='string' ? input : (input&&input.url)||'';
                return origFetch.apply(this, arguments).then(function(r) {
                    r.clone().text().then(function(t) { handleResponse(url, t); }); return r;
                });
            };
            var oOpen=XMLHttpRequest.prototype.open, oSend=XMLHttpRequest.prototype.send;
            XMLHttpRequest.prototype.open=function(m,u){this.__url=u;return oOpen.apply(this,arguments);};
            XMLHttpRequest.prototype.send=function(){
                var s=this;
                this.addEventListener('load',function(){handleResponse(s.__url||'',s.responseText||'');});
                return oSend.apply(this,arguments);
            };
            _log('interceptor ready on '+location.pathname);
        })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        controlBar = findViewById(R.id.controlBar)
        settingsPanel = findViewById(R.id.settingsPanel)
        btnAnswer = findViewById(R.id.btnAnswer)
        btnAI = findViewById(R.id.btnAI)
        btnSettingsToggle = findViewById(R.id.btnSettingsToggle)
        btnFetchModels = findViewById(R.id.btnFetchModels)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        btnCloseSettings = findViewById(R.id.btnCloseSettings)
        etBaseUrl = findViewById(R.id.etBaseUrl)
        etApiKey = findViewById(R.id.etApiKey)
        spinnerModel = findViewById(R.id.spinnerModel)
        cbPrecise = findViewById(R.id.cbPrecise)
        cbMajority = findViewById(R.id.cbMajority)

        WebView.setWebContentsDebuggingEnabled(true)
        setupWebView()
        setupGesture()
        setupButtons()
        loadSettingsToUI()

        webView.loadUrl("https://bao.ipoe.cc/Member/Login?ReturnUrl=https%3a%2f%2fwww.mosme.net")
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                view?.evaluateJavascript(interceptorJs, null)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                android.util.Log.d("MOSME_JS", msg.message())
                return true
            }
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                val popup = buildPopupWebView()
                (resultMsg?.obj as? WebView.WebViewTransport)?.webView = popup
                resultMsg?.sendToTarget()
                return true
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildPopupWebView(): WebView {
        val wv = WebView(this)
        wv.settings.apply {
            javaScriptEnabled = true; domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true; setSupportMultipleWindows(true)
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                view?.evaluateJavascript(interceptorJs, null)
            }
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                android.util.Log.d("MOSME_JS", msg.message()); return true
            }
        }
        val root = webView.parent as? android.view.ViewGroup
        root?.addView(wv, webView.layoutParams)
        wv.setOnTouchListener { v, e -> gestureDetector.onTouchEvent(e); v.onTouchEvent(e) }
        return wv
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGesture() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val now = System.currentTimeMillis()
                if (now - lastTapTime < 500) tapCount++ else tapCount = 1
                lastTapTime = now
                if (tapCount >= 3) {
                    tapCount = 0
                    controlBar.visibility = if (controlBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    if (controlBar.visibility == View.GONE) settingsPanel.visibility = View.GONE
                }
                return false
            }
        })
        webView.setOnTouchListener { v, e -> gestureDetector.onTouchEvent(e); v.onTouchEvent(e) }
    }

    private fun setupButtons() {
        btnAnswer.setOnClickListener { autoAnswer() }
        btnAnswer.setOnLongClickListener { diagnose(); true }
        btnAI.setOnClickListener { aiAnswer() }
        btnAI.setOnLongClickListener {
            webView.evaluateJavascript("window.__mosmeQuizId") { quizId ->
                val id = quizId.trim('"')
                if (id.isNotEmpty() && id != "null") {
                    prefs.edit().remove("mosme_ans_$id").apply()
                    runOnUiThread { btnAI.text = "快取已清除" }
                    webView.evaluateJavascript("localStorage.removeItem('mosme_ans_$id'); window.__mosmeAnswers=null;", null)
                }
            }
            true
        }
        btnSettingsToggle.setOnClickListener {
            settingsPanel.visibility = if (settingsPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        btnFetchModels.setOnClickListener { fetchModels() }
        btnSaveSettings.setOnClickListener { saveSettings() }
        btnCloseSettings.setOnClickListener { settingsPanel.visibility = View.GONE }

        cbMajority.setOnCheckedChangeListener { _, checked -> if (checked) cbPrecise.isChecked = false }
        cbPrecise.setOnCheckedChangeListener { _, checked -> if (checked) cbMajority.isChecked = false }
    }

    // ── 設定 ──────────────────────────────────────────────────────────────

    private fun loadSettingsToUI() {
        etBaseUrl.setText(prefs.getString("baseUrl", ""))
        etApiKey.setText(prefs.getString("apiKey", ""))
        cbPrecise.isChecked = prefs.getBoolean("preciseMode", false)
        cbMajority.isChecked = prefs.getBoolean("majorityMode", false)
        val savedModel = prefs.getString("model", "") ?: ""
        if (savedModel.isNotEmpty()) {
            modelList = mutableListOf(savedModel)
            updateModelSpinner()
        }
    }

    private fun saveSettings() {
        prefs.edit()
            .putString("baseUrl", etBaseUrl.text.toString().trimEnd('/'))
            .putString("apiKey", etApiKey.text.toString().trim())
            .putString("model", spinnerModel.selectedItem?.toString() ?: "")
            .putBoolean("preciseMode", cbPrecise.isChecked)
            .putBoolean("majorityMode", cbMajority.isChecked)
            .apply()
        btnSaveSettings.text = "已儲存 ✓"
        scope.launch { delay(1500); withContext(Dispatchers.Main) { btnSaveSettings.text = "儲存設定" } }
    }

    private fun fetchModels() {
        val baseUrl = etBaseUrl.text.toString().trimEnd('/')
        val apiKey = etApiKey.text.toString().trim()
        if (baseUrl.isEmpty() || apiKey.isEmpty()) { btnFetchModels.text = "請先填 URL 和 Key"; return }
        btnFetchModels.text = "獲取中..."
        btnFetchModels.isEnabled = false
        scope.launch {
            try {
                val req = Request.Builder().url("$baseUrl/v1/models")
                    .header("Authorization", "Bearer $apiKey").get().build()
                val body = withContext(Dispatchers.IO) {
                    http.newCall(req).execute().use { it.body?.string() ?: "" }
                }
                val arr = JSONObject(body).getJSONArray("data")
                modelList = (0 until arr.length()).map { arr.getJSONObject(it).getString("id") }
                    .sortedWith(compareByDescending {
                        when {
                            it.contains("gpt-4o") -> 10
                            it.contains("claude-3-5") || it.contains("claude-3.5") -> 9
                            it.contains("gpt-4") -> 8
                            it.contains("claude") -> 7
                            else -> 0
                        }
                    }).toMutableList()
                withContext(Dispatchers.Main) { updateModelSpinner(); btnFetchModels.text = "獲取(${modelList.size})" }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { btnFetchModels.text = "失敗: ${e.message?.take(30)}" }
            } finally {
                withContext(Dispatchers.Main) { btnFetchModels.isEnabled = true }
            }
        }
    }

    private fun updateModelSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModel.adapter = adapter
        val idx = modelList.indexOf(prefs.getString("model", ""))
        if (idx >= 0) spinnerModel.setSelection(idx)
    }

    // ── AI 作答入口 ────────────────────────────────────────────────────────

    private fun aiAnswer() {
        val baseUrl = prefs.getString("baseUrl", "")?.trimEnd('/') ?: ""
        val apiKey = prefs.getString("apiKey", "") ?: ""
        val model = prefs.getString("model", "") ?: ""
        val preciseMode = prefs.getBoolean("preciseMode", false)
        val majorityMode = prefs.getBoolean("majorityMode", false)
        if (baseUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty()) {
            btnAI.text = "請先設定 API（⚙）"; return
        }

        val modeLabel = when { majorityMode -> "多數決"; preciseMode -> "精確"; else -> "AI" }
        btnAI.text = "$modeLabel 分析中..."
        btnAI.isEnabled = false

        webView.evaluateJavascript("""
            JSON.stringify(window.__mosmeQuestions ? window.__mosmeQuestions.map(function(q,i){
                return {
                    idx: i,
                    qid: q.QuestionId||q.questionId||q.Id||q.id||'',
                    desc: q.QuestionDesc||q.questionDesc||'',
                    opts: (q.Options||q.options||[]).map(function(o,j){
                        return {j:j, id:o.ItemId||o.itemId||'', text:o.Description||o.description||'', gorder:o.GOrder||o.gorder||(j+1)}
                    })
                };
            }) : null)
        """.trimIndent()) { result ->
            val rawJson = try {
                JSONObject("""{"v":$result}""").getString("v").let { if (it == "null") null else it }
            } catch (e: Exception) { null }

            if (rawJson == null || rawJson == "null") {
                runOnUiThread { btnAI.text = "題目未載入，請等頁面完成"; btnAI.isEnabled = true }
                return@evaluateJavascript
            }

            scope.launch {
                try {
                    val questions = JSONArray(rawJson)
                    val answerMap = mutableMapOf<String, String>()
                    val total = questions.length()

                    val textQuestions = mutableListOf<JSONObject>()
                    val imageQuestions = mutableListOf<JSONObject>()
                    for (i in 0 until total) {
                        val q = questions.getJSONObject(i)
                        if (q.getString("desc").contains("<img", ignoreCase = true)) imageQuestions.add(q)
                        else textQuestions.add(q)
                    }

                    when {
                        majorityMode -> {
                            textQuestions.forEachIndexed { i, q ->
                                val r = askAIWithMajorityVote(baseUrl, apiKey, model, q)
                                if (r != null) answerMap[r.first] = r.second
                                withContext(Dispatchers.Main) { btnAI.text = "多數決(${i+1}/$total)..." }
                            }
                        }
                        preciseMode -> {
                            textQuestions.forEachIndexed { i, q ->
                                val r = askAIPrecise(baseUrl, apiKey, model, q)
                                if (r != null) answerMap[r.first] = r.second
                                withContext(Dispatchers.Main) { btnAI.text = "精確(${i+1}/$total)..." }
                            }
                        }
                        else -> {
                            var done = 0
                            textQuestions.chunked(5).forEach { batch ->
                                val batchResult = askAIBatch(baseUrl, apiKey, model, batch)
                                answerMap.putAll(batchResult)
                                val missed = batch.filter { !answerMap.containsKey(it.getString("qid")) }
                                missed.forEach { q ->
                                    val r = askAISingle(baseUrl, apiKey, model, q, withReasoning = false)
                                    if (r != null) answerMap[r.first] = r.second
                                }
                                done += batch.size
                                withContext(Dispatchers.Main) { btnAI.text = "AI($done/$total)..." }
                            }
                        }
                    }

                    imageQuestions.forEachIndexed { i, q ->
                        val r = askAIWithImage(baseUrl, apiKey, model, q)
                        if (r != null) answerMap[r.first] = r.second
                        withContext(Dispatchers.Main) { btnAI.text = "圖片題(${i+1}/${imageQuestions.size})..." }
                    }

                    withContext(Dispatchers.Main) { btnAI.text = "完成 ${answerMap.size}/$total 題，作答中..." }
                    applyAnswersToDOM(answerMap)

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { btnAI.text = "錯誤: ${e.message?.take(40)}"; btnAI.isEnabled = true }
                }
            }
        }
    }

    // ── 多數決：並行問 3 次，取多數票 ────────────────────────────────────

    private suspend fun askAIWithMajorityVote(
        baseUrl: String, apiKey: String, model: String, q: JSONObject
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        val jobs = (1..3).map { async { askAISingle(baseUrl, apiKey, model, q, withReasoning = false) } }
        val answers = jobs.awaitAll().filterNotNull()
        if (answers.isEmpty()) return@withContext null
        val votes = answers.groupBy { it.second }
        votes.maxByOrNull { it.value.size }?.value?.first()
    }

    // ── 精確模式：帶思考鏈，低信心自動重試 ──────────────────────────────

    private suspend fun askAIPrecise(
        baseUrl: String, apiKey: String, model: String, q: JSONObject
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        val opts = q.getJSONArray("opts")
        val optsText = (0 until opts.length()).joinToString("\n") { j ->
            "${SYMBOLS.getOrElse(j){"${j+1}"}}. ${opts.getJSONObject(j).getString("text")}"
        }
        val userMsg = "題目：${stripHtml(q.getString("desc"))}\n$optsText\n\n" +
            "請依序：1.分析各選項 2.確定答案 3.評估把握度(高/中/低)\n最後兩行格式：\n答案：X\n把握度：高"

        val content = try {
            callAPIContent(baseUrl, apiKey, buildPayload(model, systemPrompt(), userMsg, 600, 0.0))
        } catch (e: Exception) { return@withContext null }

        val letter = extractLetter(content) ?: return@withContext null
        val confidence = when {
            content.contains("把握度：高") || content.contains("把握度:高") -> "高"
            content.contains("把握度：低") || content.contains("把握度:低") -> "低"
            else -> "中"
        }
        val firstResult = letterToAnswer(letter, q) ?: return@withContext null

        if (confidence == "低") {
            val retries = (1..2).map { async { askAISingle(baseUrl, apiKey, model, q, withReasoning = false) } }
            val allAnswers = listOf(firstResult) + retries.awaitAll().filterNotNull()
            val votes = allAnswers.groupBy { it.second }
            return@withContext votes.maxByOrNull { it.value.size }?.value?.first() ?: firstResult
        }
        firstResult
    }

    // ── 批次問（每批 5 題，temperature=0）──────────────────────────────────

    private suspend fun askAIBatch(
        baseUrl: String, apiKey: String, model: String, batch: List<JSONObject>
    ): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            batch.forEachIndexed { i, q ->
                sb.append("${i+1}. ${stripHtml(q.getString("desc"))}\n")
                val opts = q.getJSONArray("opts")
                for (j in 0 until opts.length()) {
                    sb.append("   ${SYMBOLS.getOrElse(j){"${j+1}"}}. ${opts.getJSONObject(j).getString("text")}\n")
                }
                sb.append("\n")
            }
            val content = callAPIContent(baseUrl, apiKey, buildPayload(
                model, systemPrompt(),
                "請回答以下 ${batch.size} 道選擇題，只輸出 JSON 格式 {\"1\":\"A\",\"2\":\"B\"}，不加任何說明：\n\n$sb",
                100, 0.0
            ))
            val answerObj = extractJSON(content)
            val result = mutableMapOf<String, String>()
            batch.forEachIndexed { i, q ->
                val letter = answerObj.optString("${i+1}", "").uppercase()
                    .firstOrNull { it in 'A'..'F' }?.toString() ?: return@forEachIndexed
                letterToAnswer(letter, q)?.let { result[it.first] = it.second }
            }
            result
        } catch (e: Exception) { emptyMap() }
    }

    // ── 逐題（快速）──────────────────────────────────────────────────────

    private suspend fun askAISingle(
        baseUrl: String, apiKey: String, model: String, q: JSONObject, withReasoning: Boolean
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val opts = q.getJSONArray("opts")
            val optsText = (0 until opts.length()).joinToString("\n") { j ->
                "${SYMBOLS.getOrElse(j){"${j+1}"}}. ${opts.getJSONObject(j).getString("text")}"
            }
            val (msg, tokens) = if (withReasoning)
                Pair("題目：${stripHtml(q.getString("desc"))}\n$optsText\n\n分析後作答，最後一行：答案：X", 400)
            else
                Pair("題目：${stripHtml(q.getString("desc"))}\n$optsText\n\n答案（只寫字母）：", 5)
            val content = callAPIContent(baseUrl, apiKey, buildPayload(model, systemPrompt(), msg, tokens, 0.0))
            val letter = extractLetter(content) ?: return@withContext null
            letterToAnswer(letter, q)
        } catch (e: Exception) { null }
    }

    // ── 含圖片題 ──────────────────────────────────────────────────────────

    private suspend fun askAIWithImage(
        baseUrl: String, apiKey: String, model: String, q: JSONObject
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val opts = q.getJSONArray("opts")
            val optsText = (0 until opts.length()).joinToString("\n") { j ->
                "${SYMBOLS.getOrElse(j){"${j+1}"}}. ${opts.getJSONObject(j).getString("text")}"
            }
            val contentArr = JSONArray()
            contentArr.put(JSONObject().apply {
                put("type", "text")
                put("text", "題目：${stripHtml(q.getString("desc"))}\n$optsText\n\n請分析圖片和題目，最後一行：答案：X")
            })
            extractImageUrls(q.getString("desc")).forEach { imgUrl ->
                val b64 = downloadImageBase64(imgUrl) ?: return@forEach
                val mime = if (imgUrl.lowercase().endsWith(".png")) "image/png" else "image/jpeg"
                contentArr.put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().put("url", "data:$mime;base64,$b64"))
                })
            }
            val payload = JSONObject().apply {
                put("model", model); put("max_tokens", 500); put("temperature", 0)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply { put("role", "system"); put("content", systemPrompt()) })
                    put(JSONObject().apply { put("role", "user"); put("content", contentArr) })
                })
            }
            val content = callAPIContent(baseUrl, apiKey, payload)
            val letter = extractLetter(content) ?: return@withContext null
            letterToAnswer(letter, q)
        } catch (e: Exception) { null }
    }

    // ── 工具函數 ──────────────────────────────────────────────────────────

    private fun systemPrompt() =
        "你是台灣技術士技能檢定考試的答題專家，精通甲乙丙級：工業電子、電機修護、機械加工、" +
        "冷凍空調、室內配線、數位電子、電腦軟體應用、汽車修護等所有技術科目。" +
        "依據勞動部技能檢定中心公告的標準答案作答，繁體中文。"

    private fun buildPayload(model: String, system: String, user: String, maxTokens: Int, temp: Double) =
        JSONObject().apply {
            put("model", model); put("max_tokens", maxTokens); put("temperature", temp)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", system) })
                put(JSONObject().apply { put("role", "user"); put("content", user) })
            })
        }

    private fun callAPIContent(baseUrl: String, apiKey: String, payload: JSONObject): String {
        val req = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return http.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: throw IOException("Empty response")
            JSONObject(body).getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content").trim()
        }
    }

    private fun extractLetter(content: String): String? =
        Regex("""答案[：:]\s*([A-Fa-f])""").find(content)?.groupValues?.get(1)?.uppercase()
            ?: content.trim().uppercase().firstOrNull { it in 'A'..'F' }?.toString()

    private fun letterToAnswer(letter: String, q: JSONObject): Pair<String, String>? {
        val optIdx = SYMBOLS.indexOf(letter)
        val opts = q.getJSONArray("opts")
        if (optIdx !in 0 until opts.length()) return null
        val qid = q.getString("qid")
        val itemId = opts.getJSONObject(optIdx).getString("id")
        if (qid.isEmpty() || itemId.isEmpty()) return null
        return Pair(qid, itemId)
    }

    private fun downloadImageBase64(url: String): String? = try {
        val conn = URL(url.replace("\\", "/")).openConnection()
        conn.connectTimeout = 10000; conn.readTimeout = 15000
        Base64.encodeToString(conn.getInputStream().readBytes(), Base64.NO_WRAP)
    } catch (e: Exception) { null }

    private fun stripHtml(html: String): String =
        html.replace(Regex("<img[^>]*>", RegexOption.IGNORE_CASE), "[圖片]")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").trim()

    private fun extractImageUrls(html: String): List<String> =
        Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE).findAll(html)
            .map { it.groupValues[1] }
            .filter { it.startsWith("http") || it.startsWith("//") }
            .map { if (it.startsWith("//")) "https:$it" else it.replace("\\", "/") }
            .toList()

    private fun extractJSON(text: String): JSONObject {
        val start = text.indexOf('{'); val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return JSONObject()
        return try { JSONObject(text.substring(start, end + 1)) } catch (e: Exception) { JSONObject() }
    }

    // ── 把答案寫入 DOM ────────────────────────────────────────────────────

    private fun applyAnswersToDOM(answerMap: Map<String, String>) {
        val ansJson = JSONObject(answerMap as Map<*, *>).toString()
        webView.evaluateJavascript("""
            (function() {
                var ansMap = $ansJson;
                var qData = window.__mosmeQuestions;
                window.__mosmeAnswers = ansMap;
                if (window.__mosmeQuizId) {
                    try { localStorage.setItem('mosme_ans_'+window.__mosmeQuizId, JSON.stringify(ansMap)); } catch(e){}
                }
                var questions = document.querySelectorAll('.question-card');
                var clicked=0, skipped=0;
                for (var i=0; i<questions.length; i++) {
                    var qDom=questions[i];
                    var qid=qDom.dataset.questionId||qDom.dataset.id||'';
                    if (!qid && qData && qData[i]) qid=qData[i].QuestionId||qData[i].questionId||'';
                    var correctItemId=ansMap[qid];
                    if (!correctItemId){skipped++;continue;}
                    var opts=qDom.querySelectorAll('.single-option');
                    var correctOpt=null;
                    for (var j=0;j<opts.length;j++){
                        var opt=opts[j];
                        var itemId=opt.dataset.itemId||opt.dataset.id||opt.getAttribute('data-item-id')||'';
                        if (!itemId&&qData&&qData[i]&&qData[i].Options&&qData[i].Options[j])
                            itemId=qData[i].Options[j].ItemId||qData[i].Options[j].itemId||'';
                        if (itemId===correctItemId){correctOpt=opt;break;}
                    }
                    if (!correctOpt){skipped++;continue;}
                    var inp=correctOpt.querySelector('input[type="radio"]');
                    if (inp&&inp.checked){skipped++;continue;}
                    correctOpt.click(); clicked++;
                }
                return 'AI 作答:'+clicked+' 跳過:'+skipped+' 共:'+questions.length;
            })()
        """.trimIndent()) { res ->
            runOnUiThread { btnAI.text = res.trim('"'); btnAI.isEnabled = true }
        }
    }

    // ── 快取作答 ──────────────────────────────────────────────────────────

    private fun autoAnswer() {
        webView.evaluateJavascript("""
            (function() {
                var questions = document.querySelectorAll('.question-card');
                if (questions.length === 0) return '找不到題目\n長按診斷';
                var ansMap = window.__mosmeAnswers;
                var qData = window.__mosmeQuestions;
                if (!ansMap || !Object.keys(ansMap).length)
                    return '無快取答案\n請用 AI 作答 或 先送出一次取得答案';
                var clicked=0, skipped=0;
                for (var i=0; i<questions.length; i++) {
                    var qDom=questions[i];
                    var qid=qDom.dataset.questionId||qDom.dataset.id||'';
                    if (!qid && qData && qData[i]) qid=qData[i].QuestionId||qData[i].questionId||'';
                    var correctItemId=ansMap[qid];
                    if (!correctItemId){skipped++;continue;}
                    var opts=qDom.querySelectorAll('.single-option');
                    var correctOpt=null;
                    for (var j=0;j<opts.length;j++){
                        var opt=opts[j];
                        var itemId=opt.dataset.itemId||opt.dataset.id||opt.getAttribute('data-item-id')||'';
                        if (!itemId&&qData&&qData[i]&&qData[i].Options&&qData[i].Options[j])
                            itemId=qData[i].Options[j].ItemId||qData[i].Options[j].itemId||'';
                        if (itemId===correctItemId){correctOpt=opt;break;}
                    }
                    if (!correctOpt){skipped++;continue;}
                    var inp=correctOpt.querySelector('input[type="radio"]');
                    if (inp&&inp.checked){skipped++;continue;}
                    correctOpt.click(); clicked++;
                }
                return '作答:'+clicked+' 跳過:'+skipped+' 共:'+questions.length;
            })()
        """.trimIndent()) { result ->
            runOnUiThread { btnAnswer.text = result.trim('"').replace("\\n", "\n") }
        }
    }

    private fun diagnose() {
        webView.evaluateJavascript("""
            (function(){
                var lines=['page:'+location.pathname,'QuizId:'+window.__mosmeQuizId,
                    '題目:'+(window.__mosmeQuestions?window.__mosmeQuestions.length:0),
                    '快取:'+(window.__mosmeAnswers?Object.keys(window.__mosmeAnswers).length+'題':'none'),
                    '.question-card:'+document.querySelectorAll('.question-card').length];
                lines.push('--- LOG ---');
                for(var l of (window.__mosmeApiLog||[]).slice(-15)) lines.push(l);
                return lines.join('\n');
            })()
        """.trimIndent()) { result ->
            runOnUiThread {
                btnAnswer.text = result.trim('"').replace("\\n", "\n")
                android.util.Log.d("MOSME_JS", "[DIAG] ${btnAnswer.text}")
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
