package com.sdp.m1.Steps;

import java.util.logging.Logger;

import com.epam.healenium.SelfHealingDriver;
import com.epam.healenium.SelfHealingDriverWait;

import com.sdp.m1.Pages.LoginPage;
import com.sdp.m1.Utils.TestConfigs;
import com.sdp.m1.Utils.TestUtils;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class LoginSteps {

    private static final Logger logger = Logger.getLogger(LoginSteps.class.getName());
    private SelfHealingDriver driver;
    private SelfHealingDriverWait wait;
    private final LoginPage loginPage;
    private String browserType = TestConfigs.getBrowser();

    public LoginSteps() {
        this.driver = TestUtils.getDriver(browserType);
        this.wait = TestUtils.getWaitDriver(driver);
        this.loginPage = new LoginPage(driver, wait);
    }

    @When("I enter valid password")
    public void i_enter_valid_password() {
        loginPage.clearUsername();
        loginPage.enterPassword("test"); // Use your valid password value or parameterize if needed
        logger.info("Valid password entered");
    }

    @When("I enter valid username")
    public void i_enter_valid_username() {
        loginPage.clearPassword();
        loginPage.enterUsername("sdpsp"); // Use your valid username value or parameterize if needed
        logger.info("Valid username entered");
    }

    @Then("I should be redirected to the login page")
    public void i_should_be_redirected_to_the_login_page() {
        if (!loginPage.isOnLoginPage()) {
            throw new AssertionError("Not redirected to login page after logout");
        }
        logger.info("Redirected to login page after logout");
    }

    @Given("I am logged in successfully")
    public void i_am_logged_in_successfully() {
        i_navigate_to_the_login_page();
        i_enter_valid_username_and_password(TestConfigs.getUsername(), TestConfigs.getPassword());
        i_click_the_login_button();
        i_should_be_redirected_to_the_dashboard();
        // Add a small wait to ensure session is fully established before next step
        TestUtils.wait(1);
    }

    @Given("I am logged in as admin successfully")
    public void i_am_logged_in_as_admin_successfully() {
        i_navigate_to_the_login_page();
        i_enter_valid_username_and_password(TestConfigs.getAdminUsername(), TestConfigs.getAdminPassword());
        i_click_the_login_button();
        i_should_be_redirected_to_the_dashboard();
        // Add a small wait to ensure session is fully established before next step
        TestUtils.wait(1);
    }

    @Given("I navigate to the login page using {string}")
    public void i_navigate_to_the_login_page_using_browser(String browser) {
        browserType = browser.toLowerCase();
        i_navigate_to_the_login_page();
    }

    @Given("I navigate to the login page")
    public void i_navigate_to_the_login_page() {
        try {
            String loginUrl = TestConfigs.getBaseUrl() + "/cas/login";
            logger.info(String.format("Navigating to: %s", loginUrl));
            driver.navigate().to(loginUrl);
            loginPage.waitForPageLoad();

            logger.info("Login page loaded successfully");
        } catch (Exception e) {
            logger.severe(String.format("Failed to navigate to login page: %s", e.getMessage()));
            // Mark test as failed
            throw new RuntimeException("Navigation failed", e);
        }
    }

    @Given("I am on the login page")
    public void i_am_on_the_login_page() {
        if (loginPage == null || !loginPage.isOnLoginPage()) {
            i_navigate_to_the_login_page();
        }
    }

    @When("I enter valid {string} and {string}")
    public void i_enter_valid_username_and_password(String username, String password) {
        try {
            loginPage.enterUsername(username);
            loginPage.enterPassword(password);
            logger.info(String.format("Valid credentials entered - Username: %s", username));
        } catch (Exception e) {
            logger.severe(String.format("Failed to enter valid credentials: %s", e.getMessage()));
            throw new RuntimeException("Failed to enter credentials", e);
        }
    }

    @When("I enter invalid {string} and {string}")
    public void i_enter_invalid_username_and_password(String username, String password) {
        try {
            loginPage.enterUsername(username);
            loginPage.enterPassword(password);
            logger.info(String.format("Invalid credentials entered - Username: %s", username));
        } catch (Exception e) {
            logger.severe(String.format("Failed to enter invalid credentials: %s", e.getMessage()));
            throw new RuntimeException("Failed to enter credentials", e);
        }
    }

    @When("I click the login button")
    public void i_click_the_login_button() {
        try {
            loginPage.clickLoginButton();
            logger.info("Login button clicked");
        } catch (Exception e) {
            logger.severe(String.format("Failed to click login button: %s", e.getMessage()));
            throw new RuntimeException("Login button click failed", e);
        }
    }

    @Then("I should be redirected to the dashboard")
    public void i_should_be_redirected_to_the_dashboard() {
        try {
            loginPage.verifyDashboard();
            logger.info("Successfully redirected to dashboard");
        } catch (Exception e) {
            logger.severe(String.format("Dashboard verification failed: %s", e.getMessage()));
            throw new RuntimeException("Dashboard verification failed", e);
        }
    }

    @Then("I should see an error message")
    public void i_should_see_an_error_message() {
        try {
            loginPage.verifyErrorMessage();
            logger.info("Error message displayed successfully");
        } catch (Exception e) {
            logger.severe(String.format("Error message verification failed: %s", e.getMessage()));
            throw new RuntimeException("Error message verification failed", e);
        }
    }

    @Then("I should remain on the login page")
    public void i_should_remain_on_the_login_page() {
        try {
            if (!loginPage.isOnLoginPage()) {
                throw new AssertionError("User was redirected away from login page");
            }
            logger.info("User remained on login page as expected");
        } catch (Exception e) {
            logger.severe(String.format("Login page verification failed: %s", e.getMessage()));
            throw new RuntimeException("Login page verification failed", e);
        }
    }

    @Then("the login form should be visible")
    public void the_login_form_should_be_visible() {
        try {
            if (!loginPage.isUsernameFieldDisplayed()
                    || !loginPage.isPasswordFieldDisplayed()
                    || !loginPage.isLoginButtonEnabled()) {
                throw new AssertionError("Login form elements are not visible or enabled");
            }
            logger.info("Login form is visible and enabled");
        } catch (Exception e) {
            logger.severe(String.format("Login form visibility check failed: %s", e.getMessage()));
            throw new RuntimeException("Login form visibility check failed", e);
        }
    }

    @When("I clear the username field")
    public void i_clear_the_username_field() {
        try {
            loginPage.enterUsername("");
            logger.info("Username field cleared");
        } catch (Exception e) {
            logger.severe(String.format("Failed to clear username field: %s", e.getMessage()));
            throw new RuntimeException("Failed to clear username field", e);
        }
    }

    @When("I clear the password field")
    public void i_clear_the_password_field() {
        try {
            loginPage.enterPassword("");
            logger.info("Password field cleared");
        } catch (Exception e) {
            logger.severe(String.format("Failed to clear password field: %s", e.getMessage()));
            throw new RuntimeException("Failed to clear password field", e);
        }
    }

    @Then("the username field should be empty")
    public void the_username_field_should_be_empty() {
        try {
            String usernameValue = loginPage.getUsernameFieldValue();
            if (!usernameValue.isEmpty()) {
                throw new AssertionError("Username field is not empty, contains: " + usernameValue);
            }
            logger.info("Username field is empty as expected");
        } catch (Exception e) {
            logger.severe(String.format("Username field empty check failed: %s", e.getMessage()));
            throw new RuntimeException("Username field empty check failed", e);
        }
    }

    @Then("the password field should be empty")
    public void the_password_field_should_be_empty() {
        try {
            String passwordValue = loginPage.getPasswordFieldValue();
            if (!passwordValue.isEmpty()) {
                throw new AssertionError("Password field is not empty");
            }
            logger.info("Password field is empty as expected");
        } catch (Exception e) {
            logger.severe(String.format("Password field empty check failed: %s", e.getMessage()));
            throw new RuntimeException("Password field empty check failed", e);
        }
    }

    @And("I wait for the page to load")
    public void i_wait_for_the_page_to_load() {
        try {
            Thread.sleep(2000); // Wait for any redirects or page changes
            logger.info("Waited for page to load");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning(String.format("Wait interrupted: %s", e.getMessage()));
        }
    }

    @And("I take a screenshot")
    public void i_take_a_screenshot() {
        try {
            TestUtils.takeScreenshot("manual_screenshot");
            logger.info("Manual screenshot taken.");
        } catch (Exception e) {
            logger.severe(String.format("Error taking manual screenshot: %s", e.getMessage()));
        }
    }

    @Then("the username field should have proper label")
    public void the_username_field_should_have_proper_label() {
        if (!loginPage.hasUsernameLabel()) {
            throw new AssertionError("Username field label missing or incorrect");
        }
        logger.info("Username field label verified");
    }

    @Then("the password field should have proper label")
    public void the_password_field_should_have_proper_label() {
        if (!loginPage.hasPasswordLabel()) {
            throw new AssertionError("Password field label missing or incorrect");
        }
        logger.info("Password field label verified");
    }

    @Then("the login button should be accessible")
    public void the_login_button_should_be_accessible() {
        if (!loginPage.isLoginButtonAccessible()) {
            throw new AssertionError("Login button is not accessible");
        }
        logger.info("Login button accessibility verified");
    }

    @Then("the password field should mask the input")
    public void the_password_field_should_mask_the_input() {
        if (!loginPage.isPasswordMasked()) {
            throw new AssertionError("Password field is not masked");
        }
        logger.info("Password field masking verified");
    }

    @And("password should not be visible in page source")
    public void password_should_not_be_visible_in_page_source() {
        if (loginPage.isPasswordVisibleInPageSource()) {
            logger.warning("Password found in page source");
            throw new AssertionError("Password should not be visible in page source");
        }
        logger.info("Password not visible in page source");
    }

    @Then("the session should be properly established")
    public void the_session_should_be_properly_established() {
        if (!loginPage.hasSessionCookie()) {
            throw new AssertionError("Session not established");
        }
        logger.info("Session established");
    }

    @And("I should not be able to access login page again")
    public void i_should_not_be_able_to_access_login_page_again() {
        String loginUrl = TestConfigs.getBaseUrl() + "";
        driver.navigate().to(loginUrl);
        if (loginPage.isOnLoginPage()) {
            throw new AssertionError("Able to access login page after login");
        }
        logger.info("Login page not accessible after login");
    }

    @When("I click the logout button")
    public void i_click_the_logout_button() {
        try {
            loginPage.clickLogoutButton();
        } catch (Exception e) {
            throw new AssertionError("Logout button not found or not clickable");
        }
    }

    @And("the session should be cleared")
    public void the_session_should_be_cleared() {
        if (loginPage.hasSessionCookie()) {
            throw new AssertionError("Session not cleared after logout");
        }
        logger.info("Session cleared after logout");
    }

    @And("the page should load within acceptable time")
    public void the_page_should_load_within_acceptable_time() {
        // Placeholder: Will delegate to m1LoginPage or TestUtils after confirmation
        // Example threshold: 3 seconds
        long start = System.currentTimeMillis();
        loginPage.waitForPageLoad();
        long end = System.currentTimeMillis();
        long loadTime = end - start;
        long acceptableTimeMs = 3000; // 3 seconds
        if (loadTime > acceptableTimeMs) {
            throw new AssertionError(String.format("Page load time exceeded acceptable threshold: %dms", loadTime));
        }
        logger.info(String.format("Page loaded in acceptable time: %dms", loadTime));
    }

    @Given("I navigate to the login page using edge")
    public void i_navigate_to_the_login_page_using_edge() {
        browserType = "edge";
        i_navigate_to_the_login_page();
    }

    @Given("I navigate to the login page using firefox")
    public void i_navigate_to_the_login_page_using_firefox() {
        browserType = "firefox";
        i_navigate_to_the_login_page();
    }

    @Given("I navigate to the login page using chrome")
    public void i_navigate_to_the_login_page_using_chrome() {
        browserType = "chrome";
        i_navigate_to_the_login_page();
    }
}
