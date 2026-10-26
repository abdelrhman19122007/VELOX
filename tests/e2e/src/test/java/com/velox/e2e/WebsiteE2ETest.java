package com.velox.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The website and the dashboard in a real (headless) Chrome: E2E-01…03 of the test plan.
 * Needs a running VELOX server: -Dvelox.url=http://localhost:8080 (default).
 */
class WebsiteE2ETest {

    static final String BASE = System.getProperty("velox.url", "http://localhost:8080").replaceAll("/$", "");
    static WebDriver browser;
    static WebDriverWait wait;

    @BeforeAll
    static void open() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--window-size=1280,900", "--lang=ar");
        browser = new ChromeDriver(options);
        wait = new WebDriverWait(browser, Duration.ofSeconds(10));
    }

    @AfterAll
    static void close() {
        if (browser != null) {
            browser.quit();
        }
    }

    @Test
    void landingPageLinksToTheAppAndDashboard() {
        browser.get(BASE + "/");
        assertTrue(browser.getTitle().contains("ڤيلوكس"));
        assertEquals("rtl", browser.findElement(By.tagName("html")).getAttribute("dir"), "Arabic, right to left");
        assertTrue(browser.findElement(By.cssSelector("a[href='/app/']")).isDisplayed());
        assertTrue(browser.findElement(By.cssSelector("a[href='/admin/']")).isDisplayed());
        assertTrue(browser.findElements(By.xpath("//*[contains(text(),'+') and contains(text(),'مطعم')]")).isEmpty(),
                "no invented numbers like '500+ restaurants'");
    }

    @Test
    void wrongPasswordShowsAnErrorAndNoSession() {
        signIn("nobody-" + UUID.randomUUID() + "@example.com", "wrong-password-1");
        WebElement toast = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#toast div")));
        assertTrue(toast.getText().contains("غلط"), toast.getText());
        assertTrue(browser.findElement(By.id("login")).isDisplayed());
    }

    @Test
    void customersCannotUseTheDashboard() throws Exception {
        String email = "e2e-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        HttpResponse<String> r = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(BASE + "/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"E2E\",\"email\":\"" + email
                        + "\",\"phone\":\"01012345678\",\"password\":\"e2e-pass-123\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, r.statusCode(), r.body());

        signIn(email, "e2e-pass-123");
        WebElement toast = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#toast div")));
        assertTrue(toast.getText().contains("للإدارة"), toast.getText());
        assertTrue(browser.findElement(By.id("app")).getAttribute("class").contains("hidden"), "dashboard stays hidden");
    }

    @Test
    void securityHeadersOnPages() throws Exception {
        HttpResponse<Void> r = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(BASE + "/admin/")).build(),
                HttpResponse.BodyHandlers.discarding());
        assertEquals("DENY", r.headers().firstValue("X-Frame-Options").orElse(""), "no clickjacking");
        assertEquals("nosniff", r.headers().firstValue("X-Content-Type-Options").orElse(""));
    }

    private static void signIn(String email, String password) {
        browser.get(BASE + "/admin/");
        browser.manage().deleteAllCookies();
        ((org.openqa.selenium.JavascriptExecutor) browser).executeScript("sessionStorage.clear()");
        browser.navigate().refresh();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys(email);
        browser.findElement(By.id("password")).sendKeys(password);
        browser.findElement(By.cssSelector("#login-form button[type=submit]")).click();
    }
}
