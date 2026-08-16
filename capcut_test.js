/*
NAME: capcut.com - Generator Account Capcut Non Pro
Type: Scraper
Noted: Jangan lupa follow. Ini non pro ya, kenapa engga scrape yang pro? karna butuh region indo dan vps saya region bukan indo.
Saluran: https://whatsapp.com/channel/0029Vb6dJVWBA1eukbJ5kX1r
Base Url: https://capcut.com/signup
Developer: t.me/hazeloffc
*/
const { chromium } = require('playwright');

function generatePassword(length = 12) {
    const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#';
    let pw = '';
    for (let i = 0; i < length; i++) {
        pw += chars[Math.floor(Math.random() * chars.length)];
    }
    return pw;
}

function generateBirthday() {
    const day = Math.floor(Math.random() * 28) + 1;
    const month = Math.floor(Math.random() * 12) + 1;
    const year = Math.floor(Math.random() * (2004 - 1980 + 1)) + 1980;
    return { day, month, year };
}

const MONTH_EN = {
    1: 'January', 2: 'February', 3: 'March', 4: 'April',
    5: 'May', 6: 'June', 7: 'July', 8: 'August',
    9: 'September', 10: 'October', 11: 'November', 12: 'December'
};

async function createCapcutAccount(email) {
    if (!email) throw new Error('Email wajib diisi');

    const password = generatePassword();
    const birthday = generateBirthday();
    const dayStr = String(birthday.day);
    const yearStr = String(birthday.year);
    const monthName = MONTH_EN[birthday.month];

    const browser = await chromium.launch({
        headless: false,
        args: [
            '--disable-blink-features=AutomationControlled',
            '--no-sandbox',
            '--disable-infobars'
        ]
    });

    const context = await browser.newContext({
        viewport: { width: 1366, height: 768 },
        locale: 'en-US',
        timezoneId: 'America/New_York',
        extraHTTPHeaders: {
            'Accept-Language': 'en-US,en;q=0.9'
        }
    });

    await context.addInitScript(
        "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });"
    );

    const page = await context.newPage();

    await page.goto('https://www.capcut.com/signup', {
        waitUntil: 'domcontentloaded',
        timeout: 30000
    });
    await page.waitForTimeout(2500);

    await page.getByText('Continue with email', { exact: false }).first().click();
    await page.waitForTimeout(2500);

    const emailInput = page.locator('input[type="email"], input[name="username"]').first();
    await emailInput.waitFor({ state: 'visible', timeout: 15000 });
    await emailInput.fill(email);

    await page.locator('button:has-text("Continue")').first().click();
    await page.waitForTimeout(1800);

    await page.locator('input[type="password"]').first().fill(password);

    const passSubmit = page.locator('button').filter({ hasText: /^(Sign up|Register|Continue)$/i }).first();
    await passSubmit.click();
    await page.waitForTimeout(1800);

    const yearInput = page.locator('input').filter({ hasNotText: '' }).first();
    try {
        await page.getByPlaceholder('Year').fill(yearStr);
    } catch {
        await yearInput.fill(yearStr);
    }
    await page.waitForTimeout(500);

    await page.getByRole('combobox').filter({ hasText: /Month/i }).click();
    await page.getByRole('option', { name: monthName }).click();
    await page.waitForTimeout(500);

    await page.getByRole('combobox').filter({ hasText: /Day/i }).click();
    await page.getByRole('option', { name: dayStr, exact: true }).click();
    await page.waitForTimeout(500);

    await page.locator('button').filter({ hasText: /^(Next|Continue|Submit)$/i }).first().click();
    await page.waitForTimeout(3000);

    console.log('Waiting for OTP... Masukkan OTP secara manual di browser');
    console.log(`Email: ${email}`);
    console.log(`Password: ${password}`);

    await page.waitForTimeout(120000);

    await browser.close();
    return { email, password, birthday };
}

createCapcutAccount('your-email@example.com').then(console.log);