Phase 25: RoCat Browser Automation Engine - General Purpose Support


 Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan teknis di `ai_memory/task_YYYYMMDD_HHMM_tahap24_modern_ui_and_media_headers.md`.
   
```markdown
# Role and Objective
Kamu adalah **Senior Android Engineer** dan **RoCat Core Architect** yang memahami arsitektur RoCat secara mendalam.

**Tujuan Utama:** Membangun **Browser Automation Engine** general-purpose untuk RoCat yang memungkinkan skrip berinteraksi dengan halaman web secara dinamis (seperti Playwright/Puppeteer), **TANPA** mengubah kode aplikasi inti.

---

# Core Problem Statement

## Masalah Utama
RoCat saat ini hanya mendukung:
- `fetch()` HTTP sinkron (request-response)
- `RoCatDOM` parsing HTML statis (Jsoup)
- **TIDAK** mendukung interaksi dinamis dengan halaman web (click, fill, wait)

## Solusi yang Diinginkan
Membangun **JavaScript Bridge Layer** di atas WebView yang memungkinkan skrip RoCat untuk:
1. Membuka URL di WebView (headless/visible)
2. Mengisi form, klik tombol
3. Menunggu elemen muncul
4. Mengekstrak data dari DOM dinamis
5. Menjalankan JavaScript di konteks halaman

---

# Architecture Design (Tanpa Modifikasi App)

## Pendekatan: WebView-based Automation Bridge

```

┌─────────────────────────────────────────────────────────────┐
│                    RoCat App (Existing)                     │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │  Script Canvas   │    │    WebView (Hidden/Visible)    │ │
│  │  (Rhino Engine)  │◄──►│    - Load URL                  │ │
│  │                  │    │    - Execute JS                │ │
│  │  RoCatUI.*       │    │    - DOM Manipulation          │ │
│  │  RoCatDOM.*      │    │    - Event Simulation          │ │
│  │  fetch()         │    │    - Cookie Sync               │ │
│  └─────────────────┘    └─────────────────────────────────┘ │
│           │                           │                      │
│           ▼                           ▼                      │
│  ┌─────────────────────────────────────────────────────────┐│
│  │           RoCatBrowser Bridge (New Layer)              ││
│  │  - page.goto(url)                                     ││
│  │  - page.locator(selector).click()                     ││
│  │  - page.locator(selector).fill(text)                  ││
│  │  - page.waitForSelector(selector)                     ││
│  │  - page.evaluate(jsFunction)                          ││
│  │  - page.screenshot()                                  ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘

```

## Komponen yang Dibutuhkan

### 1. WebView Manager (Kotlin - Existing App)
```kotlin
// Tidak perlu modifikasi - RoCat sudah punya WebView
// Hanya perlu mengaksesnya dari script
```

2. JavaScript Bridge (Kotlin - Existing App)

```kotlin
// Tambahkan interface baru yang bisa dipanggil dari skrip
@JavascriptInterface
fun browserCommand(command: String, params: String): String
```

3. Script API (JavaScript - Baru)

```javascript
// Global object baru untuk skrip
var RoCatBrowser = {
    // Navigation
    goto: function(url) { ... },
    back: function() { ... },
    forward: function() { ... },
    reload: function() { ... },
    
    // Page info
    url: function() { ... },
    title: function() { ... },
    content: function() { ... },
    
    // Locator
    locator: function(selector) { ... },
    
    // Wait
    waitForSelector: function(selector, timeout) { ... },
    waitForLoad: function(state) { ... },
    waitForTimeout: function(ms) { ... },
    
    // Evaluate
    evaluate: function(jsCode) { ... },
    
    // Screenshot
    screenshot: function() { ... },
    
    // Cookies
    getCookies: function() { ... },
    setCookie: function(cookie) { ... },
};
```

---

Implementation Plan (Tanpa Modifikasi App)

Phase 1: WebView Bridge (Kotlin - Existing App)

File: app/src/main/java/com/rocat/scripting/api/RoCatBrowserBridge.kt

```kotlin
package com.rocat.scripting.api

import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import org.json.JSONObject
import org.json.JSONArray

class RoCatBrowserBridge {
    companion object {
        private var webView: WebView? = null
        private var pendingCommands = mutableMapOf<String, (String) -> Unit>()
        private var commandId = 0
        
        fun initialize(context: Context) {
            if (webView == null) {
                webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            // Notify script
                            notifyPageLoaded(url)
                        }
                    }
                    
                    webChromeClient = WebChromeClient()
                    
                    // Inject bridge
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onCommandResult(id: String, result: String) {
                            pendingCommands[id]?.invoke(result)
                            pendingCommands.remove(id)
                        }
                    }, "RoCatBridge")
                }
            }
        }
        
        fun executeCommand(command: String, params: JSONObject): String {
            val id = (commandId++).toString()
            val script = """
                (function() {
                    try {
                        var result = ${command}(${params.toString()});
                        RoCatBridge.onCommandResult('$id', JSON.stringify(result));
                    } catch(e) {
                        RoCatBridge.onCommandResult('$id', JSON.stringify({
                            error: e.message,
                            stack: e.stack
                        }));
                    }
                })();
            """.trimIndent()
            
            var result: String? = null
            val latch = CountDownLatch(1)
            
            pendingCommands[id] = { res ->
                result = res
                latch.countDown()
            }
            
            webView?.evaluateJavascript(script, null)
            latch.await(30, TimeUnit.SECONDS)
            
            return result ?: "{\"error\": \"timeout\"}"
        }
        
        private fun notifyPageLoaded(url: String?) {
            // Notify script via Rhino
        }
    }
}
```

Phase 2: Script API (JavaScript - User Script)

File: scripting/api/RoCatBrowser.js (di-inject ke skrip)

```javascript
// ============================================
// RoCatBrowser API - General Purpose
// ============================================

var RoCatBrowser = (function() {
    var _instance = null;
    var _currentPage = null;
    var _defaultTimeout = 30000;
    
    // ============ Locator Class ============
    function Locator(selector, page) {
        this.selector = selector;
        this.page = page;
        this.timeout = _defaultTimeout;
    }
    
    Locator.prototype = {
        // Click element
        click: function() {
            return this.page.evaluate(function(sel) {
                var el = document.querySelector(sel);
                if (!el) throw new Error("Element not found: " + sel);
                el.click();
                return { success: true };
            }, this.selector);
        },
        
        // Fill input
        fill: function(text) {
            return this.page.evaluate(function(sel, txt) {
                var el = document.querySelector(sel);
                if (!el) throw new Error("Element not found: " + sel);
                el.value = txt;
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
                return { success: true };
            }, this.selector, text);
        },
        
        // Get text
        text: function() {
            return this.page.evaluate(function(sel) {
                var el = document.querySelector(sel);
                return el ? el.textContent.trim() : "";
            }, this.selector);
        },
        
        // Get attribute
        getAttribute: function(attr) {
            return this.page.evaluate(function(sel, attrName) {
                var el = document.querySelector(sel);
                return el ? el.getAttribute(attrName) : null;
            }, this.selector, attr);
        },
        
        // Check if exists
        exists: function() {
            return this.page.evaluate(function(sel) {
                return document.querySelector(sel) !== null;
            }, this.selector);
        },
        
        // Wait for element
        waitFor: function(timeout) {
            timeout = timeout || this.timeout;
            var start = Date.now();
            while (Date.now() - start < timeout) {
                if (this.exists()) return true;
                // Simulate delay (Rhino doesn't have setTimeout)
                for (var i = 0; i < 100000; i++) {}
            }
            throw new Error("Timeout waiting for element: " + this.selector);
        },
        
        // Get all matching elements
        all: function() {
            return this.page.evaluate(function(sel) {
                var els = document.querySelectorAll(sel);
                return Array.from(els).map(function(el) {
                    return {
                        text: el.textContent.trim(),
                        html: el.innerHTML,
                        attributes: el.attributes ? Array.from(el.attributes).reduce(function(acc, attr) {
                            acc[attr.name] = attr.value;
                            return acc;
                        }, {}) : {}
                    };
                });
            }, this.selector);
        },
        
        // Click all matching elements
        clickAll: function() {
            return this.page.evaluate(function(sel) {
                var els = document.querySelectorAll(sel);
                var results = [];
                els.forEach(function(el, index) {
                    try {
                        el.click();
                        results.push({ index: index, success: true });
                    } catch(e) {
                        results.push({ index: index, success: false, error: e.message });
                    }
                });
                return results;
            }, this.selector);
        },
        
        // Type with delay (simulate human typing)
        type: function(text, delay) {
            delay = delay || 50;
            var chars = text.split('');
            for (var i = 0; i < chars.length; i++) {
                this.fill(this.text() + chars[i]);
                for (var j = 0; j < delay * 1000; j++) {}
            }
            return this;
        },
        
        // Scroll to element
        scrollIntoView: function() {
            return this.page.evaluate(function(sel) {
                var el = document.querySelector(sel);
                if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                return el !== null;
            }, this.selector);
        },
        
        // Get element position
        getBoundingRect: function() {
            return this.page.evaluate(function(sel) {
                var el = document.querySelector(sel);
                if (!el) return null;
                var rect = el.getBoundingClientRect();
                return {
                    x: rect.x,
                    y: rect.y,
                    width: rect.width,
                    height: rect.height,
                    top: rect.top,
                    right: rect.right,
                    bottom: rect.bottom,
                    left: rect.left
                };
            }, this.selector);
        }
    };
    
    // ============ Page Class ============
    function Page() {
        this.url = "";
        this.title = "";
        this.isLoading = false;
    }
    
    Page.prototype = {
        // Navigate to URL
        goto: function(url, options) {
            options = options || {};
            var timeout = options.timeout || _defaultTimeout;
            
            // Call native WebView
            var result = _executeCommand('goto', {
                url: url,
                timeout: timeout
            });
            
            if (result && result.error) {
                throw new Error(result.error);
            }
            
            this.url = url;
            this.title = result.title || "";
            this.isLoading = false;
            
            // Wait for DOM
            if (options.waitUntil) {
                this.waitForLoad(options.waitUntil, timeout);
            }
            
            return this;
        },
        
        // Wait for page load
        waitForLoad: function(state, timeout) {
            state = state || 'load';
            timeout = timeout || _defaultTimeout;
            
            return _executeCommand('waitForLoad', {
                state: state,
                timeout: timeout
            });
        },
        
        // Get page content
        content: function() {
            return _executeCommand('getContent', {});
        },
        
        // Evaluate JavaScript
        evaluate: function(fn, arg) {
            var fnStr = fn.toString();
            var result = _executeCommand('evaluate', {
                script: fnStr,
                arg: arg
            });
            
            if (result && result.error) {
                throw new Error(result.error);
            }
            
            return result;
        },
        
        // Create locator
        locator: function(selector) {
            return new Locator(selector, this);
        },
        
        // Screenshot
        screenshot: function(options) {
            options = options || {};
            return _executeCommand('screenshot', {
                path: options.path || "",
                quality: options.quality || 80
            });
        },
        
        // Get cookies
        cookies: function() {
            return _executeCommand('getCookies', {});
        },
        
        // Set cookie
        setCookie: function(cookie) {
            return _executeCommand('setCookie', cookie);
        },
        
        // Clear cookies
        clearCookies: function() {
            return _executeCommand('clearCookies', {});
        },
        
        // Wait for selector
        waitForSelector: function(selector, timeout) {
            timeout = timeout || _defaultTimeout;
            var locator = this.locator(selector);
            return locator.waitFor(timeout);
        },
        
        // Click selector
        click: function(selector) {
            return this.locator(selector).click();
        },
        
        // Fill selector
        fill: function(selector, text) {
            return this.locator(selector).fill(text);
        },
        
        // Get text from selector
        text: function(selector) {
            return this.locator(selector).text();
        },
        
        // Go back
        goBack: function() {
            return _executeCommand('goBack', {});
        },
        
        // Go forward
        goForward: function() {
            return _executeCommand('goForward', {});
        },
        
        // Reload
        reload: function() {
            return _executeCommand('reload', {});
        },
        
        // Close page
        close: function() {
            return _executeCommand('closePage', {});
        }
    };
    
    // ============ Browser Class ============
    function Browser() {
        this.pages = [];
        this.currentPage = null;
        this.isHeadless = true;
    }
    
    Browser.prototype = {
        // Launch browser (initialize WebView)
        launch: function(options) {
            options = options || {};
            this.isHeadless = options.headless !== false;
            
            var result = _executeCommand('launch', {
                headless: this.isHeadless,
                viewport: options.viewport || { width: 1366, height: 768 },
                userAgent: options.userAgent || "",
                locale: options.locale || "en-US"
            });
            
            if (result && result.error) {
                throw new Error(result.error);
            }
            
            return this;
        },
        
        // Create new page/tab
        newPage: function() {
            var page = new Page();
            this.pages.push(page);
            this.currentPage = page;
            return page;
        },
        
        // Get current page
        page: function() {
            if (!this.currentPage) {
                this.newPage();
            }
            return this.currentPage;
        },
        
        // Close browser
        close: function() {
            return _executeCommand('closeBrowser', {});
        },
        
        // Set default timeout
        setDefaultTimeout: function(timeout) {
            _defaultTimeout = timeout;
            return this;
        }
    };
    
    // ============ Helper: Execute Native Command ============
    function _executeCommand(command, params) {
        // Bridge to native WebView
        var result = RoCatBrowserBridge.execute(command, JSON.stringify(params));
        try {
            return JSON.parse(result);
        } catch(e) {
            return { error: "Failed to parse result: " + e.message };
        }
    }
    
    // ============ Public API ============
    return {
        // Create new browser instance
        launch: function(options) {
            var browser = new Browser();
            browser.launch(options);
            return browser;
        },
        
        // Get singleton instance
        getInstance: function() {
            if (!_instance) {
                _instance = new Browser();
            }
            return _instance;
        },
        
        // Connect to existing browser
        connect: function() {
            return this.getInstance();
        },
        
        // Version info
        version: function() {
            return "RoCatBrowser v1.0.0";
        },
        
        // Set default timeout globally
        setDefaultTimeout: function(timeout) {
            _defaultTimeout = timeout;
        }
    };
})();

// ============================================
// Auto-initialize on script load
// ============================================
if (typeof RoCatBrowserBridge !== 'undefined') {
    var __browserInstance = RoCatBrowser.getInstance();
    RoCatUI.log("✅ RoCatBrowser initialized");
}
```

Phase 3: Test Script (capcut_test.js - General Purpose)

```javascript
// ==UserScript==
// @name         CapCut Account Generator (RoCatBrowser)
// @version      1.0.0
// @description  Generate CapCut account using RoCatBrowser automation
// @author       RoCat AI
// @category     Automation
// @icon         https://www.capcut.com/favicon.ico
// @match        https://www.capcut.com/*
// ==/UserScript==

// ============================================
// Account Generator using RoCatBrowser
// ============================================

var BASE_URL = "https://www.capcut.com";
var browser = null;
var page = null;

// ============ Helper Functions ============
function generatePassword(length) {
    length = length || 12;
    var chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#";
    var pw = "";
    for (var i = 0; i < length; i++) {
        pw += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return pw;
}

function generateEmail() {
    var domains = ["gmail.com", "outlook.com", "yahoo.com", "protonmail.com"];
    var user = Math.random().toString(36).substring(2, 10);
    return user + "@" + domains[Math.floor(Math.random() * domains.length)];
}

function generateBirthday() {
    var day = Math.floor(Math.random() * 28) + 1;
    var month = Math.floor(Math.random() * 12) + 1;
    var year = Math.floor(Math.random() * (2004 - 1980 + 1)) + 1980;
    return { day: day, month: month, year: year };
}

var MONTH_NAMES = {
    1: "January", 2: "February", 3: "March", 4: "April",
    5: "May", 6: "June", 7: "July", 8: "August",
    9: "September", 10: "October", 11: "November", 12: "December"
};

// ============ Main Account Creation ============
function createCapcutAccount(email) {
    try {
        // Generate data
        if (!email || email === "") {
            email = generateEmail();
        }
        var password = generatePassword(12);
        var birthday = generateBirthday();
        var monthName = MONTH_NAMES[birthday.month];
        
        RoCatUI.log("🚀 Starting CapCut account creation");
        RoCatUI.log("📧 Email: " + email);
        RoCatUI.log("🔑 Password: " + password);
        RoCatUI.log("🎂 Birthday: " + birthday.year + " " + monthName + " " + birthday.day);
        
        // Initialize browser
        browser = RoCatBrowser.launch({
            headless: false,
            viewport: { width: 1366, height: 768 },
            locale: "en-US"
        });
        
        page = browser.newPage();
        
        // Navigate to signup
        RoCatUI.log("🌐 Opening signup page...");
        page.goto(BASE_URL + "/signup", {
            waitUntil: "domcontentloaded",
            timeout: 30000
        });
        
        // Wait for page to load
        RoCatUI.log("⏳ Waiting for page to load...");
        page.waitForTimeout(2000);
        
        // Click "Continue with email"
        RoCatUI.log("📧 Clicking 'Continue with email'...");
        page.click('text="Continue with email"');
        page.waitForTimeout(1000);
        
        // Fill email
        RoCatUI.log("📝 Filling email...");
        var emailInput = page.locator('input[type="email"], input[name="username"]');
        emailInput.waitFor(5000);
        emailInput.fill(email);
        
        // Click Continue
        RoCatUI.log("➡️ Clicking Continue...");
        page.click('button:has-text("Continue")');
        page.waitForTimeout(1000);
        
        // Fill password
        RoCatUI.log("🔑 Filling password...");
        var passInput = page.locator('input[type="password"]');
        passInput.waitFor(5000);
        passInput.fill(password);
        
        // Click Sign up
        RoCatUI.log("✅ Clicking Sign up...");
        page.click('button:has-text("Sign up"), button:has-text("Register"), button:has-text("Continue")');
        page.waitForTimeout(1000);
        
        // Fill birthday
        RoCatUI.log("🎂 Filling birthday...");
        
        // Year
        var yearInput = page.locator('input[placeholder="Year"], input[name="year"]');
        yearInput.waitFor(5000);
        yearInput.fill(String(birthday.year));
        page.waitForTimeout(500);
        
        // Month
        page.click('select[name="month"], [role="combobox"]:has-text("Month")');
        page.click('option:has-text("' + monthName + '")');
        page.waitForTimeout(500);
        
        // Day
        page.click('select[name="day"], [role="combobox"]:has-text("Day")');
        page.click('option:has-text("' + birthday.day + '")');
        page.waitForTimeout(500);
        
        // Submit
        RoCatUI.log("📤 Submitting form...");
        page.click('button:has-text("Next"), button:has-text("Continue"), button:has-text("Submit")');
        page.waitForTimeout(3000);
        
        // Success
        RoCatUI.log("✅ Account created successfully!");
        RoCatUI.log("📧 Email: " + email);
        RoCatUI.log("🔑 Password: " + password);
        RoCatUI.log("⏳ Waiting for OTP verification...");
        
        // Return result
        return {
            success: true,
            email: email,
            password: password,
            birthday: birthday,
            status: "PENDING_OTP",
            message: "Check email for OTP code"
        };
        
    } catch (e) {
        RoCatUI.log("❌ Error: " + e.message);
        RoCatUI.addAlert("Failed: " + e.message, "error");
        return {
            success: false,
            error: e.message
        };
    }
}

// ============ UI Functions ============
function onLaunch() {
    RoCat.render([
        { type: "clear" },
        { type: "alert", message: "🚀 CapCut Account Generator (RoCatBrowser)", level: "info" },
        { type: "input", id: "email", hint: "Email (kosong untuk auto-generate)" },
        { type: "button", label: "🚀 Buat Akun", fn: "doCreateAccount" },
        { type: "button", label: "📧 Generate Email", fn: "generateRandomEmail" },
        { type: "badges", badges: ["v1.0", "RoCatBrowser", "Automation"] }
    ]);
    RoCatUI.log("✅ Ready to create CapCut accounts!");
}

function doCreateAccount(inputs) {
    var email = (inputs && inputs.email || "").trim();
    var result = createCapcutAccount(email);
    
    if (result && result.success) {
        RoCat.render([
            { type: "clear" },
            { type: "button", label: "🏠 Home", fn: "onLaunch" },
            { type: "button", label: "🔄 Buat Lagi", fn: "onLaunch" },
            { type: "json", 
              data: result, 
              title: "📋 Account Details", 
              copy: true },
            { type: "alert", message: "✅ Account created! Check email for OTP", level: "success" }
        ]);
    } else {
        RoCat.render([
            { type: "clear" },
            { type: "button", label: "🏠 Home", fn: "onLaunch" },
            { type: "alert", message: "❌ Failed: " + (result ? result.error : "Unknown error"), 
              level: "error" }
        ]);
    }
}

function generateRandomEmail() {
    var email = generateEmail();
    RoCatUI.log("📧 Random email: " + email);
    RoCatUI.addAlert("Email: " + email, "info");
}

// ============ Auto-run ============
if (typeof RoCatUI === "undefined") {
    print("Running in headless mode...");
    var result = createCapcutAccount("");
    print("Result: " + JSON.stringify(result, null, 2));
}
```

---

Deliverables

1. Browser Bridge (Kotlin)

· RoCatBrowserBridge.kt - WebView manager
· JavaScript interface for script communication
· Cookie synchronization with OkHttp
· Headless mode support

2. Script API (JavaScript)

· RoCatBrowser global object
· Browser class with launch/close
· Page class with goto/click/fill
· Locator class for element operations
· Wait mechanisms (selector, timeout, load)
· Evaluate for custom JavaScript

3. Test Script

· capcut_test.js using RoCatBrowser API
· Full account creation flow
· Error handling
· UI integration

4. Documentation

· DOCS_BROWSER_API.md - Complete API reference
· Migration guide from Playwright
· Example scripts

---

Key Design Decisions

Decision Reasoning
No app modification Skrip runs purely on existing RoCat WebView
Synchronous API Rhino doesn't support async, use callbacks + wait
Polyfill approach Playwright-like API, but internally sync
WebView reuse Single WebView instance, multiple pages via navigate
Cookie sync Share cookies with OkHttp for consistency

---

Success Criteria

1. ✅ Skrip dapat membuka URL di WebView
2. ✅ Skrip dapat mengisi form dan klik tombol
3. ✅ Skrip dapat menunggu elemen muncul
4. ✅ Skrip dapat mengekstrak data dari DOM
5. ✅ Skrip dapat menjalankan JavaScript custom
6. ✅ Semua operasi sinkron (tanpa async/await)
7. ✅ Tidak ada modifikasi kode aplikasi inti
8. ✅ General-purpose (bisa dipakai skrip lain)

```