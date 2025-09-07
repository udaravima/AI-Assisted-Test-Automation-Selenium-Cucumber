package com.sdp.m1.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
// import org.openqa.selenium.support.ui.WebDriverWait;

import com.epam.healenium.SelfHealingDriver;
import com.epam.healenium.SelfHealingDriverWait;

public class TestUtils {

    private static final Logger logger = Logger.getLogger(TestUtils.class.getName());
    private static final Random random = new Random();
    private static final AtomicBoolean testFailed = new AtomicBoolean(false);
    private static SelfHealingDriver driver;
    private static SelfHealingDriverWait waitDriver;

    /**
     * Mark test as failed
     */
    public static void markTestFailed() {
        testFailed.set(true);
        logger.warning("Test marked as failed");
    }

    /**
     * Mark test as passed
     */
    public static void markTestPassed() {
        testFailed.set(false);
        logger.info("Test marked as passed");
    }

    /**
     * Check if test has failed
     */
    public static boolean isTestFailed() {
        return testFailed.get();
    }

    /**
     * Reset test failure status
     */
    public static void resetTestStatus() {
        testFailed.set(false);
        logger.info("Test status reset");
    }

    /**
     * Generate a random string of specified length
     */
    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static String generateRandomNumber(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Generate a random email address
     */
    public static String generateRandomEmail() {
        return "test." + generateRandomString(8) + "@example.com";
    }

    /**
     * Generate a random username
     */
    public static String generateRandomUsername() {
        return "user_" + generateRandomString(6);
    }

    /**
     * Generate a random password
     */
    public static String generateRandomPassword() {
        return "Pass" + generateRandomString(8) + "123!";
    }

    /**
     * Generate a random phone number
     */
    public static String generateRandomPhone() {
        return "+1" + (random.nextInt(900) + 100) + "-"
                + (random.nextInt(900) + 100) + "-"
                + (random.nextInt(9000) + 1000);
    }

    /**
     * Take a screenshot and save it to the specified directory
     */
    public static String takeScreenshot(String testName) {
        if (driver == null) {
            logger.warning("Cannot take screenshot, driver is not initialized.");
            return null;
        }
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = testName + "_" + timestamp + ".png";
            String screenshotPath = "target/screenshots/" + fileName;

            // Create screenshots directory if it doesn't exist
            Path directory = Paths.get("target/screenshots");
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            // Get the underlying driver from SelfHealingDriver
            WebDriver underlyingDriver = (WebDriver) driver.getClass().getMethod("getDelegate").invoke(driver);
            File screenshot = ((TakesScreenshot) underlyingDriver).getScreenshotAs(OutputType.FILE);
            Files.copy(screenshot.toPath(), Paths.get(screenshotPath));

            logger.info(String.format("Screenshot saved: %s", screenshotPath));
            return screenshotPath;
        } catch (ReflectiveOperationException | ClassCastException | IOException e) {
            logger.severe(String.format("Failed to take screenshot: %s", e.getMessage()));
            return null;
        }
    }

    /**
     * Wait for a specified number of seconds
     */
    public static void wait(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning(String.format("Wait interrupted: %s", e.getMessage()));
        }
    }

    /**
     * Wait for a specified number of milliseconds
     */
    public static void waitMillis(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning(String.format("Wait interrupted: %s", e.getMessage()));
        }
    }

    /**
     * Generate test data for different scenarios
     */
    public static class TestData {

        public static final String[] VALID_USERNAMES = { "sdpsp", "admin", "user1", "testuser" };
        public static final String[] VALID_PASSWORDS = { "test", "password123", "admin123", "userpass" };
        public static final String[] INVALID_USERNAMES = { "", "   ", "invalid@user", "user'123",
                "<script>alert('xss')</script>" };
        public static final String[] INVALID_PASSWORDS = { "", "123", "weak", "password" };

        public static final String[] SQL_INJECTION_PAYLOADS = {
                "'; DROP TABLE users; --",
                "' OR '1'='1",
                "'; INSERT INTO users VALUES ('hacker', 'password'); --",
                "' UNION SELECT * FROM users --"
        };

        public static final String[] XSS_PAYLOADS = {
                "<script>alert('xss')</script>",
                "javascript:alert('xss')",
                "<img src=x onerror=alert('xss')>",
                "';alert('xss');//"
        };

        public static final String[] COMMAND_INJECTION_PAYLOADS = {
                "& cat /etc/passwd",
                "; ls -la",
                "| whoami",
                "`id`"
        };
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email != null && email.matches(emailRegex);
    }

    /**
     * Validate phone number format
     */
    public static boolean isValidPhone(String phone) {
        String phoneRegex = "^\\+?[1-9]\\d{1,14}$";
        return phone != null && phone.replaceAll("[\\s\\-\\(\\)]", "").matches(phoneRegex);
    }

    /**
     * Validate password strength
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Clean up test data
     */
    public static void cleanupTestData(String... filePaths) {
        for (String filePath : filePaths) {
            try {
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    Files.delete(path);
                    logger.info(String.format("Cleaned up test data: %s", filePath));
                }
            } catch (IOException e) {
                logger.warning(String.format("Failed to cleanup test data %s: %s", filePath, e.getMessage()));
            }
        }
    }

    /**
     * Get current timestamp in readable format
     */
    public static String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /**
     * Get current timestamp in file-safe format
     */
    public static String getCurrentTimestampForFile() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }

    /**
     * Check if a file exists
     */
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Get file size in bytes
     */
    public static long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            logger.warning(String.format("Failed to get file size for %s: %s", filePath, e.getMessage()));
            return -1;
        }
    }

    /**
     * Create a test directory
     */
    public static boolean createTestDirectory(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.info(String.format("Created test directory: %s", directoryPath));
                return true;
            }
            return true;
        } catch (IOException e) {
            logger.severe(String.format("Failed to create test directory %s: %s", directoryPath, e.getMessage()));
            return false;
        }
    }

    /**
     * Delete a test directory and its contents
     */
    public static boolean deleteTestDirectory(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                logger.warning(String.format("Failed to delete file: %s", p));
                            }
                        });
                logger.info(String.format("Deleted test directory: %s", directoryPath));
                return true;
            }
            return true;
        } catch (IOException e) {
            logger.severe(String.format("Failed to delete test directory %s: %s", directoryPath, e.getMessage()));
            return false;
        }
    }

    /**
     * Measure the time taken for a page load action and assert threshold
     * 
     * @param pageLoadAction Runnable that performs the page load (e.g.
     *                       loginPage.waitForPageLoad())
     * @param logger         Logger for logging
     */
    public static void assertPageLoadWithinThreshold(Runnable pageLoadAction, Logger logger) {
        long start = System.currentTimeMillis();
        pageLoadAction.run();
        long end = System.currentTimeMillis();
        long loadTime = end - start;
        // Get threshold from TestConfigs
        long thresholdMs = com.sdp.m1.Utils.TestConfigs.getPageLoadThresholdMs();
        if (loadTime > thresholdMs) {
            logger.warning(
                    String.format("Page load time exceeded threshold: %dms (threshold: %dms)", loadTime, thresholdMs));
            throw new AssertionError(String.format("Page load time exceeded acceptable threshold: %dms", loadTime));
        }
        logger.info(String.format("Page loaded in acceptable time: %dms", loadTime));
    }

    public static void removeDriver() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                logger.warning("Error occurred while quitting WebDriver: " + e.getMessage());
            }
        }
        driver = null;
        waitDriver = null;
    }

    /**
     * Get the WebDriver instance
     */
    public static SelfHealingDriver getDriver(String browserType) {
        // If browserType is null, it's a call to get the existing driver.
        // If the driver is null, we can't proceed without a browser type.
        if (driver == null && browserType == null) {
            return null;
        } else if (driver == null) {
            try {
                switch (browserType.toLowerCase()) {
                    case "firefox" -> {
                        FirefoxOptions firefoxOptions = new FirefoxOptions();
                        firefoxOptions.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                        if (TestConfigs.isHeadless()) {
                            firefoxOptions.addArguments("--headless");
                        } else {
                            firefoxOptions.addArguments("--headed");
                        }
                        driver = SelfHealingDriver.create(new FirefoxDriver(firefoxOptions));
                        break;
                    }
                    case "edge" -> {
                        EdgeOptions edgeOptions = new EdgeOptions();
                        edgeOptions.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                        if (TestConfigs.isHeadless()) {
                            edgeOptions.addArguments("--headless");
                        } else {
                            edgeOptions.addArguments("--headed");
                        }
                        driver = SelfHealingDriver.create(new EdgeDriver(edgeOptions));
                        break;
                    }
                    default -> {
                        // System.setProperty("webdriver.chrome.driver",
                        // com.sdp.m1.Runner.TestConfigs.getChromeDriverPath());
                        ChromeOptions chromeOptions = new ChromeOptions();
                        chromeOptions.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                        if (TestConfigs.isHeadless()) {
                            chromeOptions.addArguments("--headless");
                        } else {
                            chromeOptions.addArguments("--headed");
                        }
                        driver = SelfHealingDriver.create(new ChromeDriver(chromeOptions));
                    }
                }
            } catch (

            Exception e) {
                System.out.println("Error occurred while initializing WebDriver: " + e.getMessage());
                logger.severe(
                        String.format("Failed to get WebDriver instance for %s: %s\nCheck whether healenium is up!!",
                                browserType, e.getMessage()));
            }
            if (driver == null) {
                System.out.println("Failed to initialize WebDriver");
                throw new RuntimeException("WebDriver initialization failed");
            }
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(Integer.parseInt(TestConfigs.getDelay())));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(Integer.parseInt(TestConfigs.getDelay())));
        driver.manage().window().maximize();

        return driver;

    }

    public static SelfHealingDriverWait getWaitDriver(SelfHealingDriver driver) {
        if (driver == null) {
            // Fail fast if the driver is not initialized. This prevents
            // NullPointerExceptions later.
            throw new IllegalStateException("Cannot create a wait instance with a null driver.");
        }
        if (waitDriver == null) {
            // The timeout can also be sourced from TestConfigs for consistency
            waitDriver = new SelfHealingDriverWait(driver, Duration.ofSeconds(10));
        }
        return waitDriver;
    }

}

/*
 * Public Methods:
 * - markTestFailed(): Marks the current test as failed.
 * - markTestPassed(): Marks the current test as passed.
 * - isTestFailed(): Returns true if the test is marked as failed.
 * - resetTestStatus(): Resets the test failure status.
 * - generateRandomString(int length): Generates a random alphanumeric string.
 * - generateRandomNumber(int length): Generates a random numeric string.
 * - generateRandomEmail(): Generates a random email address.
 * - generateRandomUsername(): Generates a random username.
 * - generateRandomPassword(): Generates a random password.
 * - generateRandomPhone(): Generates a random phone number.
 * - takeScreenshot(String testName): Takes a screenshot and saves it with a
 * timestamp.
 * - wait(int seconds): Pauses execution for the given seconds.
 * - waitMillis(long milliseconds): Pauses execution for the given milliseconds.
 * - isValidEmail(String email): Validates email format.
 * - isValidPhone(String phone): Validates phone number format.
 * - isStrongPassword(String password): Checks password strength.
 * - cleanupTestData(String... filePaths): Deletes specified files.
 * - getCurrentTimestamp(): Returns current timestamp in readable format.
 * - getCurrentTimestampForFile(): Returns current timestamp in file-safe
 * format.
 * - fileExists(String filePath): Checks if a file exists.
 * - getFileSize(String filePath): Returns file size in bytes.
 * - createTestDirectory(String directoryPath): Creates a directory if not
 * exists.
 * - deleteTestDirectory(String directoryPath): Deletes a directory and its
 * contents.
 * - assertPageLoadWithinThreshold(Runnable pageLoadAction, Logger logger):
 * Asserts page load time within threshold.
 * - removeDriver(): Quits and cleans up the WebDriver instance.
 * - getDriver(String browserType): Initializes and returns a SelfHealingDriver
 * for the browser.
 * - getWaitDriver(SelfHealingDriver driver): Returns a SelfHealingDriverWait
 * for explicit waits.
 *
 * Private Methods:
 * (None. All methods in this class are public.)
 */
