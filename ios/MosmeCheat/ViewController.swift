import UIKit
import WebKit

class ViewController: UIViewController, WKNavigationDelegate, WKUIDelegate {

    private var webView: WKWebView!
    private var answerButton: UIButton!
    private var tapCount = 0
    private var lastTapTime: TimeInterval = 0

    override func viewDidLoad() {
        super.viewDidLoad()
        setupWebView()
        setupAnswerButton()
        setupGestureRecognizer()

        if let url = URL(string: "https://bao.ipoe.cc/Member/Login?ReturnUrl=https%3a%2f%2fwww.mosme.net") {
            webView.load(URLRequest(url: url))
        }
    }

    private func setupWebView() {
        let config = WKWebViewConfiguration()
        config.preferences.javaScriptEnabled = true
        config.preferences.javaScriptCanOpenWindowsAutomatically = true

        webView = WKWebView(frame: view.bounds, configuration: config)
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        webView.navigationDelegate = self
        webView.uiDelegate = self
        webView.customUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        webView.allowsBackForwardNavigationGestures = true

        view.addSubview(webView)
    }

    private func setupAnswerButton() {
        answerButton = UIButton(type: .system)
        answerButton.setTitle("自動答題", for: .normal)
        answerButton.backgroundColor = .systemBlue
        answerButton.setTitleColor(.white, for: .normal)
        answerButton.layer.cornerRadius = 8
        answerButton.titleLabel?.font = .systemFont(ofSize: 16, weight: .medium)
        answerButton.isHidden = true

        answerButton.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(answerButton)

        NSLayoutConstraint.activate([
            answerButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            answerButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -20),
            answerButton.widthAnchor.constraint(equalToConstant: 120),
            answerButton.heightAnchor.constraint(equalToConstant: 44)
        ])

        answerButton.addTarget(self, action: #selector(autoAnswer), for: .touchUpInside)
    }

    private func setupGestureRecognizer() {
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTripleTap))
        tapGesture.numberOfTapsRequired = 1
        webView.addGestureRecognizer(tapGesture)
    }

    @objc private func handleTripleTap() {
        let now = Date().timeIntervalSince1970
        if now - lastTapTime < 0.5 {
            tapCount += 1
        } else {
            tapCount = 1
        }
        lastTapTime = now

        if tapCount >= 3 {
            tapCount = 0
            answerButton.isHidden.toggle()
        }
    }

    @objc private func autoAnswer() {
        let js = """
            (function() {
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
                if (total === 0) return '找不到題目';

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

                var skipped = 0, clicked = 0;

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
                            skipped++;
                        } else {
                            var btn = correctOpt.querySelector('.option-button');
                            if (btn) btn.click(); else correctOpt.click();
                            clicked++;
                        }
                    }
                    return 'KO:' + clicked + ' 已正確:' + skipped + ' 共:' + total;
                }

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
                if (clicked === 0 && skipped === 0) return '找不到答案';
                return 'isanswer:' + clicked + ' 已正確:' + skipped + ' 共:' + total;
            })()
        """

        webView.evaluateJavaScript(js) { [weak self] result, error in
            DispatchQueue.main.async {
                if let result = result as? String {
                    self?.answerButton.setTitle(result, for: .normal)
                } else if let error = error {
                    self?.answerButton.setTitle("錯誤: \\(error.localizedDescription)", for: .normal)
                }
            }
        }
    }

    // WKUIDelegate - Handle popup windows
    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration, for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        if navigationAction.targetFrame == nil {
            webView.load(navigationAction.request)
        }
        return nil
    }
}
