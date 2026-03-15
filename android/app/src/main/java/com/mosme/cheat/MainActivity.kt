package com.mosme.cheat

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var btnAnswer: Button
    private lateinit var gestureDetector: GestureDetectorCompat
    private var tapCount = 0
    private var lastTapTime = 0L

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        btnAnswer = findViewById(R.id.btnAnswer)
        btnAnswer.visibility = View.GONE

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // 三連點切換按鈕顯示/隱藏
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val now = System.currentTimeMillis()
                if (now - lastTapTime < 500) tapCount++ else tapCount = 1
                lastTapTime = now
                if (tapCount >= 3) {
                    tapCount = 0
                    btnAnswer.visibility =
                        if (btnAnswer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }
                return false
            }
        })

        webView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            v.onTouchEvent(event)
        }

        btnAnswer.setOnClickListener { autoAnswer() }

        webView.loadUrl("https://bao.ipoe.cc/Member/Login?ReturnUrl=https%3a%2f%2fwww.mosme.net")
    }

    private fun autoAnswer() {
        val js = """
            (function() {
                // 判斷某個選項是否處於已選中狀態
                function isSelected(el) {
                    if (!el) return false;
                    var cls = el.className || '';
                    if (/selected|active|checked|correct/i.test(cls)) return true;
                    var inp = el.querySelector('input[type="radio"], input[type="checkbox"]');
                    if (inp && inp.checked) return true;
                    return false;
                }

                var questions = document.querySelectorAll('.question');
                var total = questions.length;
                if (total === 0) return '找不到題目，請確認已開啟測驗頁面';

                // 策略 1：Knockout.js ViewModel
                var qList = null;
                try {
                    var koCtx = ko && ko.dataFor && ko.dataFor(document.body);
                    if (koCtx) {
                        var unwrap = function(v) { return typeof v === 'function' ? v() : v; };
                        for (var key of Object.keys(koCtx)) {
                            var val = unwrap(koCtx[key]);
                            if (Array.isArray(val) && val.length > 0) {
                                var first = val[0];
                                if (first && (first.Answer !== undefined || first.answer !== undefined ||
                                    first.CorrectAnswer !== undefined || first.ans !== undefined)) {
                                    qList = val; break;
                                }
                            }
                        }
                    }
                } catch(e) {}

                var skipped = 0, clicked = 0, fixed = 0;

                if (qList) {
                    for (var i = 0; i < total && i < qList.length; i++) {
                        var q = questions[i];
                        var correctAns = String(qList[i].Answer || qList[i].answer ||
                            qList[i].CorrectAnswer || qList[i].ans || '');
                        var opts = q.querySelectorAll('.option');
                        var correctOpt = null;
                        for (var opt of opts) {
                            var v = opt.dataset.value || opt.dataset.ans || opt.getAttribute('value') || '';
                            var t = opt.textContent.trim();
                            if (v === correctAns || t.startsWith('(' + correctAns + ')') || t.startsWith(correctAns + '.')) {
                                correctOpt = opt; break;
                            }
                        }
                        if (!correctOpt) { skipped++; continue; }

                        if (isSelected(correctOpt)) {
                            // 已選正確，跳過
                            skipped++;
                        } else {
                            // 點擊正確答案
                            var btn = correctOpt.querySelector('.option-button');
                            if (btn) btn.click(); else correctOpt.click();
                            clicked++;
                        }
                    }
                    return 'KO 作答:' + clicked + ' 已正確:' + skipped + ' 共:' + total;
                }

                // 策略 2：isanswer="1" 屬性
                for (var q of questions) {
                    var correct = q.querySelector('.option[isanswer="1"]');
                    if (!correct) { skipped++; continue; }
                    if (isSelected(correct)) {
                        skipped++;
                    } else {
                        var btn = correct.querySelector('.option-button');
                        if (btn) btn.click(); else correct.click();
                        clicked++;
                    }
                }
                if (clicked === 0 && skipped === 0) return '找不到 isanswer 標記';
                return 'isanswer 作答:' + clicked + ' 已正確:' + skipped + ' 共:' + total;
            })()
        """.trimIndent()

        webView.evaluateJavascript(js) { result ->
            runOnUiThread {
                btnAnswer.text = result.trim('"')
            }
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
