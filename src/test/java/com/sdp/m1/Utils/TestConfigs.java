package com.sdp.m1.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class TestConfigs {
    private static Properties properties;

    // Default values
    private static final String DEFAULT_LOGIN_URL = "https://m1-impl.hsenidmobile.com/provisioning";
    private static final String DEFAULT_USERNAME = "sdpsp";
    private static final String DEFAULT_PASSWORD = "test";
    private static final String DEFAULT_DELAY = "10";
    private static final String DEFAULT_BROWSER = "chrome";
    private static final String DEFAULT_HEADLESS = "false";
    private static final String DEFAULT_TIMEOUT = "30";
    private static final String DEFAULT_PAGE_LOAD_THRESHOLD_MS = "20000";

    static {
        loadProperties();
    }

    private static void loadProperties() {
        properties = new Properties();
        try {
            // Try to load from test.properties first
            String configFile = System.getProperty("config.file", "src/test/resources/test.properties");
            try (FileInputStream input = new FileInputStream(configFile)) {
                properties.load(input);
            }
        } catch (IOException e) {
            // If file doesn't exist, use default values
            System.out.println("Configuration file not found, using default values");
        }
    }

    public static String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            value = properties.getProperty(key);
        }
        return value != null ? value : defaultValue;
    }

    // Configuration getters
    public static String getLoginUrl() {
        return getProperty("login.url", DEFAULT_LOGIN_URL);
    }

    public static String getUsername() {
        return getProperty("username", DEFAULT_USERNAME);
    }

    public static String getPassword() {
        return getProperty("password", DEFAULT_PASSWORD);
    }

    public static String getAdminUsername() {
        return getProperty("admin.username", DEFAULT_USERNAME);
    }

    public static String getAdminPassword() {
        return getProperty("admin.password", DEFAULT_PASSWORD);
    }

    public static String getDelay() {
        return getProperty("delay", DEFAULT_DELAY);
    }

    public static String getBrowser() {
        return getProperty("browser", DEFAULT_BROWSER);
    }

    public static boolean isHeadless() {
        return Boolean.parseBoolean(getProperty("headless", DEFAULT_HEADLESS));
    }

    public static String getTimeout() {
        return getProperty("timeout", DEFAULT_TIMEOUT);
    }

    public static long getPageLoadThresholdMs() {
        return Long.parseLong(getProperty("page.load.threshold.ms", DEFAULT_PAGE_LOAD_THRESHOLD_MS));
    }

    // Legacy constants for backward compatibility
    public static final String LOGIN_URL = getLoginUrl();
    public static final String USERNAME = getUsername();
    public static final String PASSWORD = getPassword();
    public static final String DELAY = getDelay();

    // Environment-specific configurations
    public static String getEnvironment() {
        return getProperty("environment", "test");
    }

    public static boolean isProduction() {
        return "production".equalsIgnoreCase(getEnvironment());
    }

    public static boolean isStaging() {
        return "staging".equalsIgnoreCase(getEnvironment());
    }

    public static boolean isTest() {
        return "test".equalsIgnoreCase(getEnvironment());
    }

    // Test execution configurations
    public static boolean isParallelExecution() {
        return Boolean.parseBoolean(getProperty("parallel.execution", "false"));
    }

    public static int getThreadCount() {
        return Integer.parseInt(getProperty("thread.count", "2"));
    }

    public static boolean isScreenshotOnFailure() {
        return Boolean.parseBoolean(getProperty("screenshot.on.failure", "true"));
    }

    public static boolean isScreenshotOnSuccess() {
        return Boolean.parseBoolean(getProperty("screenshot.on.success", "false"));
    }

    public static String getScreenshotPath() {
        return getProperty("screenshot.path", "target/screenshots");
    }

    // Driver path configurations
    public static String getChromeDriverPath() {
        return getProperty("chromedriver.path", "");
    }

    public static String getGeckoDriverPath() {
        return getProperty("geckodriver.path", "");
    }

    public static String getEdgeDriverPath() {
        return getProperty("edgedriver.path", "");
    }

    public static String getBaseUrl() {
        return getProperty("base.url", DEFAULT_LOGIN_URL);
    }
}

/*
 * Public Methods:
 * ---------------
 * getProperty(String key, String defaultValue)
 * - Retrieves a property value, prioritizing system properties, then loaded
 * properties, and finally a default value.
 *
 * getLoginUrl()
 * - Returns the configured login URL.
 *
 * getUsername()
 * - Returns the configured username.
 *
 * getPassword()
 * - Returns the configured password.
 *
 * getAdminUsername()
 * - Returns the configured admin username.
 *
 * getAdminPassword()
 * - Returns the configured admin password.
 *
 * getDelay()
 * - Returns the configured delay value.
 *
 * getBrowser()
 * - Returns the configured browser name (e.g., "chrome").
 *
 * isHeadless()
 * - Returns true if headless browser mode is enabled, false otherwise.
 *
 * getTimeout()
 * - Returns the configured timeout value.
 *
 * getPageLoadThresholdMs()
 * - Returns the configured page load threshold in milliseconds.
 *
 * getEnvironment()
 * - Returns the current environment name (e.g., "test", "production").
 *
 * isProduction()
 * - Returns true if the environment is "production".
 *
 * isStaging()
 * - Returns true if the environment is "staging".
 *
 * isTest()
 * - Returns true if the environment is "test".
 *
 * isParallelExecution()
 * - Returns true if parallel test execution is enabled.
 *
 * getThreadCount()
 * - Returns the number of threads for parallel execution.
 *
 * isScreenshotOnFailure()
 * - Returns true if screenshots are taken on test failure.
 *
 * isScreenshotOnSuccess()
 * - Returns true if screenshots are taken on test success.
 *
 * getScreenshotPath()
 * - Returns the path where screenshots are saved.
 *
 * getChromeDriverPath()
 * - Returns the path to the ChromeDriver executable.
 *
 * getGeckoDriverPath()
 * - Returns the path to the GeckoDriver executable.
 *
 * getEdgeDriverPath()
 * - Returns the path to the EdgeDriver executable.
 *
 * getBaseUrl()
 * - Returns the base URL for the application.
 *
 * Private Methods:
 * ----------------
 * loadProperties()
 * - Loads configuration properties from a file or uses default values if the
 * file is not found.
 */