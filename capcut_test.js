// ==UserScript==
// @name         CapCut Account Generator (RoCatBrowser)
// @version      1.0.0
// @description  Generate CapCut account via the RoCatBrowser automation engine (Playwright-like).
// @author       RoCat AI
// @category     Automation
// @icon         https://www.capcut.com/favicon.ico
// @match        https://www.capcut.com/*
// ==/UserScript==

// ============================================================
// Contoh skrip untuk RoCatBrowser (Tahap 25) — general-purpose
// browser automation. API-nya meniru Playwright/Puppeteer namun
// SINKRON (Rhino 1.7.15 tanpa async/await):
//
//   RoCatBrowser.launch({ headless, viewport }) -> Browser
//   browser.newPage() / browser.page()           -> Page
//   page.goto(url, { waitUntil, timeout })
//   page.locator(sel).click/fill/text/waitFor
//   page.click(sel) / page.fill(sel, txt)
//   page.waitForSelector(sel, timeout)
//   page.screenshot({ path, quality })
//   page.cookies() / page.setCookie(cookie) / page.clearCookies()
//
// Semua operasi berjalan di satu WebView tersembunyi; cookie
// otomatis ter-sync dengan mesin fetch() OkHttp (AndroidCookieJar).
// ============================================================

var BASE_URL = "https://www.capcut.com";

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

function log(res, label) {
    if (res && res.success === false) {
        RoCatUI.addAlert(label + ": " + res.error, "warning");
    }
}

// ============ Main Account Creation ============

function createCapcutAccount(email) {
    var browser = null;
    try {
        // --- Generate data ---
        if (!email || email === "") email = generateEmail();
        var password = generatePassword(12);
        var birthday = generateBirthday();
        var monthName = MONTH_NAMES[birthday.month];

        RoCatUI.log("🚀 Starting CapCut account creation");
        RoCatUI.log("📧 Email: " + email);
        RoCatUI.log("🔑 Password: " + password);

        // --- Launch browser (single hidden WebView, cookie-synced with fetch) ---
        browser = RoCatBrowser.launch({
            headless: false,
            viewport: { width: 1366, height: 768 }
        });
        var page = browser.newPage();

        // --- Navigate to signup ---
        RoCatUI.log("🌐 Opening signup page...");
        page.goto(BASE_URL + "/signup", {
            waitUntil: "domcontentloaded",
            timeout: 30000
        });
        page.waitForTimeout(2000);

        // --- "Continue with email" ---
        RoCatUI.log("📧 Clicking 'Continue with email'...");
        log(page.click('button:has-text("Continue with email"), text="Continue with email"'), "Continue-with-email");
        page.waitForTimeout(1000);

        // --- Fill email ---
        RoCatUI.log("📝 Filling email...");
        var emailInput = page.locator('input[type="email"], input[name="username"]');
        emailInput.waitFor(5000);
        log(emailInput.fill(email), "Email");
        page.waitForTimeout(500);

        // --- Continue ---
        RoCatUI.log("➡️ Clicking Continue...");
        log(page.click('button:has-text("Continue")'), "Continue");
        page.waitForTimeout(1500);

        // --- Fill password ---
        RoCatUI.log("🔑 Filling password...");
        var passInput = page.locator('input[type="password"]');
        passInput.waitFor(5000);
        log(passInput.fill(password), "Password");

        // --- Sign up ---
        RoCatUI.log("✅ Clicking Sign up...");
        log(page.click('button:has-text("Sign up"), button:has-text("Register"), button:has-text("Continue")'), "Sign-up");
        page.waitForTimeout(1000);

        // --- Fill birthday ---
        RoCatUI.log("🎂 Filling birthday: " + birthday.year + " " + monthName + " " + birthday.day);
        var yearInput = page.locator('input[placeholder="Year"], input[name="year"]');
        if (yearInput.exists()) {
            log(yearInput.fill(String(birthday.year)), "Year");
            page.waitForTimeout(500);
        }
        log(page.click('select[name="month"], [role="combobox"]:has-text("Month")'), "Month dropdown");
        log(page.click('option:has-text("' + monthName + '")'), "Month value");
        page.waitForTimeout(500);
        log(page.click('select[name="day"], [role="combobox"]:has-text("Day")'), "Day dropdown");
        log(page.click('option:has-text("' + birthday.day + '")'), "Day value");
        page.waitForTimeout(500);

        // --- Submit ---
        RoCatUI.log("📤 Submitting form...");
        log(page.click('button:has-text("Next"), button:has-text("Continue"), button:has-text("Submit")'), "Submit");
        page.waitForTimeout(3000);

        // --- Success ---
        RoCatUI.log("✅ Account creation reached OTP step!");
        var shot = page.screenshot({ path: "", quality: 80 });
        if (shot) RoCatUI.log("📸 Screenshot: " + shot);
        var cookies = page.cookies();
        if (cookies && cookies.length) RoCatUI.log("🍪 Cookies: " + cookies.length);

        browser.close();

        return {
            success: true,
            email: email,
            password: password,
            birthday: birthday,
            status: "PENDING_OTP",
            message: "Check email for OTP code",
            screenshot: shot
        };
    } catch (e) {
        RoCatUI.log("❌ Error: " + e.message);
        RoCatUI.addAlert("Failed: " + e.message, "error");
        if (browser) { try { browser.close(); } catch (_) {} }
        return { success: false, error: e.message };
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
            { type: "json", data: result, title: "📋 Account Details", copy: true },
            { type: "alert", message: "✅ Account created! Check email for OTP", level: "success" }
        ]);
    } else {
        RoCat.render([
            { type: "clear" },
            { type: "button", label: "🏠 Home", fn: "onLaunch" },
            { type: "alert", message: "❌ Failed: " + (result ? result.error : "Unknown error"), level: "error" }
        ]);
    }
}

function generateRandomEmail() {
    var email = generateEmail();
    RoCatUI.log("📧 Random email: " + email);
    RoCatUI.addAlert("Email: " + email, "info");
}

// ============ Headless run (tanpa UI) ============
if (typeof RoCatUI === "undefined") {
    print("Running in headless mode...");
    var result = createCapcutAccount("");
    print("Result: " + JSON.stringify(result, null, 2));
}