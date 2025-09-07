package com.sdp.m1.Pages;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.epam.healenium.SelfHealingDriver;
import com.epam.healenium.SelfHealingDriverWait;

// import java.time.Duration;
import java.util.logging.Logger;

// import com.codeborne.selenide.conditions.webdriver.Url;

public class LoginPage {
    private static final Logger logger = Logger.getLogger(LoginPage.class.getName());
    private final SelfHealingDriver driver;
    private final SelfHealingDriverWait wait;

    // Locators
    private final By usernameField = By.id("username");
    private final By passwordField = By.id("password");
    private final By errorMessage = By.id("status");
    // private final By successMsg = By.id("msg");
    private final By loginButton = By.xpath("//*[@id=\"login\"]/div[3]/input[3]");
    // private final By pageTitle = By.tagName("title");
    private final By usernameLabel = By.xpath("//label[@for=\"username\"]");
    private final By passwordLabel = By.xpath("//label[@for=\"password\"]");

    public LoginPage(SelfHealingDriver driver, SelfHealingDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void clearPassword() {
        try {
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(passwordField));
            element.clear();
            logger.info("Password field cleared successfully");
        } catch (TimeoutException e) {
            logger.severe(String.format("Password field not clickable: %s", e.getMessage()));
            throw new RuntimeException("Password field not accessible", e);
        }
    }

    public void clearUsername() {
        try {
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(usernameField));
            element.clear();
            logger.info("Username field cleared successfully");
        } catch (TimeoutException e) {
            logger.severe(String.format("Username field not clickable: %s", e.getMessage()));
            throw new RuntimeException("Username field not accessible", e);
        }
    }

    public void enterUsername(String username) {
        try {
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(usernameField));
            element.clear();
            element.sendKeys(username);
            logger.info(String.format("Username entered: %s", username));
        } catch (TimeoutException e) {
            logger.severe(String.format("Username field not clickable: %s", e.getMessage()));
            throw new RuntimeException("Username field not accessible", e);
        }
    }

    public void enterPassword(String password) {
        try {
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(passwordField));
            element.clear();
            element.sendKeys(password);
            logger.info("Password entered successfully");
        } catch (TimeoutException e) {
            logger.severe(String.format("Password field not clickable: %s", e.getMessage()));
            throw new RuntimeException("Password field not accessible", e);
        }
    }

    public void clickLoginButton() {
        try {
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(loginButton));
            element.click();
            logger.info("Login button clicked");
        } catch (TimeoutException e) {
            logger.severe(String.format("Login button not clickable: %s", e.getMessage()));
            throw new RuntimeException("Login button not accessible", e);
        }
    }

    public void login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLoginButton();
    }

    public boolean isUsernameFieldDisplayed() {
        try {
            return driver.findElement(usernameField).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean isPasswordFieldDisplayed() {
        try {
            return driver.findElement(passwordField).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean isLoginButtonEnabled() {
        try {
            return driver.findElement(loginButton).isEnabled();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public String getUsernameFieldValue() {
        try {
            return driver.findElement(usernameField).getAttribute("value");
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    public String getPasswordFieldValue() {
        try {
            return driver.findElement(passwordField).getAttribute("value");
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    public void verifyErrorMessage() {
        try {
            WebElement errorElement = wait.until(ExpectedConditions.visibilityOfElementLocated(errorMessage));
            String actualMessage = errorElement.getText();
            String messageClass = errorElement.getAttribute("class");

            logger.info(String.format("Actual error message: %s", actualMessage));
            logger.info(String.format("Error message class: %s", messageClass));

            // More flexible error message validation
            if (actualMessage == null || actualMessage.trim().isEmpty()) {
                throw new AssertionError("Error message is empty or null");
            }

            if (messageClass != null && !messageClass.contains("error")) {
                logger.warning(String.format("Error message may not have expected styling class"));
            }
        } catch (TimeoutException e) {
            logger.severe(String.format("Error message not displayed within timeout: %s", e.getMessage()));
            throw new RuntimeException("Error message not displayed", e);
        }
    }

    public void verifyDashboard() {
        try {
            // WebElement successElement =
            // wait.until(ExpectedConditions.visibilityOfElementLocated(pageTitle));
            // WebElement titleElement = successElement.findElement(By.tagName("h2"));
            // String actualTitle = titleElement.getText();

            // logger.info(String.format("Actual success message: %s",
            // successElement.getText()));

            // Check if the current URL matches the expected provisioning pattern
            String currentUrl = driver.getCurrentUrl();
            logger.info(String.format("Current URL: %s", currentUrl));
            if (!currentUrl.matches("https://m1-impl\\.hsenidmobile\\.com/provisioning/.*")) {
                throw new AssertionError("Current URL does not match expected provisioning pattern: " + currentUrl);
            }

            // // More flexible success validation
            // if (actualTitle == null || actualTitle.trim().isEmpty()) {
            // throw new AssertionError("Success message is empty or null");
            // }

            // // Check if the message contains expected keywords
            // String lowerTitle = actualTitle.toLowerCase();
            // if (!lowerTitle.contains("success") && !lowerTitle.contains("welcome")
            // && !lowerTitle.contains("M1-SDP Provisioning")) {
            // logger.warning(String.format("Success message may not indicate successful
            // login: %s", actualTitle));
            // }
        } catch (TimeoutException e) {
            logger.severe(String.format("Success message not displayed within timeout: %s", e.getMessage()));
            throw new RuntimeException("Success message not displayed", e);
        }
    }

    public String getPageTitle() {
        try {
            return driver.getTitle();
        } catch (Exception e) {
            logger.warning(String.format("Could not get page title: %s", e.getMessage()));
            return "";
        }
    }

    public boolean isOnLoginPage() {
        try {
            return driver.getCurrentUrl().contains("login") ||
                    driver.findElement(usernameField).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void waitForPageLoad() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(usernameField));
            wait.until(ExpectedConditions.presenceOfElementLocated(passwordField));
            wait.until(ExpectedConditions.presenceOfElementLocated(loginButton));
            logger.info(String.format("Login page loaded successfully"));
        } catch (TimeoutException e) {
            logger.severe(String.format("Login page did not load completely: %s", e.getMessage()));
            throw new RuntimeException("Login page load timeout", e);
        }
    }

    // --- Helper methods for step definitions ---

    public boolean hasUsernameLabel() {
        try {
            WebElement label = wait.until(ExpectedConditions.visibilityOfElementLocated(usernameLabel));
            return label.isDisplayed();
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    public boolean hasPasswordLabel() {
        try {
            WebElement label = wait.until(ExpectedConditions.visibilityOfElementLocated(passwordLabel));
            return label.isDisplayed();
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    public boolean isPasswordMasked() {
        try {
            WebElement password = wait.until(ExpectedConditions.visibilityOfElementLocated(passwordField));
            String type = password.getAttribute("type");
            return "password".equals(type);
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    public boolean isLoginButtonAccessible() {
        try {
            WebElement button = wait.until(ExpectedConditions.elementToBeClickable(loginButton));
            return button.isEnabled();
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    public boolean isPasswordVisibleInPageSource() {
        String pageSource = driver.getPageSource();
        // This checks for the password input type, not actual values
        return pageSource.contains("type=\"password\"");
    }

    public boolean hasSessionCookie() {
        // If TestUtils has a session check, use it here
        // Otherwise, keep this implementation
        return driver.manage().getCookies().stream().anyMatch(c -> c.getName().toLowerCase().contains("jsessionid"));
    }

    public void clickLogoutButton() {
        try {
            WebElement logoutBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='ygmauserinfo']/a")));
            logoutBtn.click();
            logger.info(String.format("Logout button clicked"));
        } catch (TimeoutException | NoSuchElementException e) {
            throw new RuntimeException("Logout button not found or not clickable", e);
        }
    }
}