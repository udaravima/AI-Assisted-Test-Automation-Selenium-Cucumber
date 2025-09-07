package com.sdp.m1.Steps;

import com.sdp.m1.Pages.LoginPage;
import com.sdp.m1.Pages.ServiceProviderRegistrationPage;
import com.sdp.m1.Utils.TestConfigs;
import com.sdp.m1.Utils.TestUtils;
import com.epam.healenium.SelfHealingDriver;
import com.epam.healenium.SelfHealingDriverWait;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import io.cucumber.java.Before;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServiceProviderRegistrationSteps {

    private final SelfHealingDriver driver;
    private final SelfHealingDriverWait wait;
    private ServiceProviderRegistrationPage registrationPage;
    private LoginPage loginPage;

    public ServiceProviderRegistrationSteps() {
        this.driver = TestUtils.getDriver(TestConfigs.getBrowser());
        this.wait = TestUtils.getWaitDriver(this.driver);
        this.registrationPage = new ServiceProviderRegistrationPage(driver, wait);
        this.loginPage = new LoginPage(driver, wait);
    }

    @Given("I am logged in as an administrator")
    public void i_am_logged_in_as_an_administrator() {
        driver.get(TestConfigs.getBaseUrl());
        loginPage.waitForPageLoad();
        loginPage.enterUsername(TestConfigs.getAdminUsername());
        loginPage.enterPassword(TestConfigs.getAdminPassword());
        loginPage.clickLoginButton();
        loginPage.verifyDashboard();
    }

    @Given("I am on the service provider registration page")
    public void i_am_on_the_service_provider_registration_page() {
        registrationPage.navigateToPage();
    }

    @When("I fill the {string} field with {string}")
    public void i_fill_the_field_with(String fieldName, String value) {
        switch (fieldName) {
            case "Service Provider ID":
                registrationPage.enterServiceProviderId(value);
                break;
            case "Company name":
                registrationPage.enterCompanyName(value);
                break;
            case "Address":
                registrationPage.enterAddress(value);
                break;
            case "Description":
                registrationPage.enterDescription(value);
                break;
            case "White Listed Users":
                registrationPage.enterWhiteListedUsers(value);
                break;
        }
    }

    @When("I select {string} from the {string} list")
    public void i_select_from_the_list(String item, String listName) {
        switch (listName) {
            case "SP users":
                registrationPage.selectSpUser(item);
                break;
            case "Marketing Users":
                registrationPage.selectMarketingUser(item);
                break;
        }
    }

    @When("I select the {string} resource")
    public void i_select_the_resource(String resource) {
        registrationPage.selectResource(resource);
    }

    @When("I click the {string} button")
    public void i_click_the_button(String buttonName) {
        if ("Submit".equals(buttonName)) {
            registrationPage.clickSubmitButton();
        }
    }

    @Then("I should see the success message {string}")
    public void i_should_see_the_success_message(String expectedMessage) {
        assertEquals(expectedMessage, registrationPage.getSuccessMessage());
    }

    @Then("I should see the error message {string}")
    public void i_should_see_the_error_message(String expectedMessage) {
        assertEquals(expectedMessage, registrationPage.getErrorMessage());
    }

    @When("I fill all mandatory fields for a valid registration")
    public void i_fill_all_mandatory_fields_for_a_valid_registration() {
        registrationPage.enterServiceProviderId("87654321");
        registrationPage.enterCompanyName("Temp Valid Company");
        registrationPage.enterWhiteListedUsers("98765432");
        registrationPage.selectSpUser("SdpSp");
        registrationPage.selectMarketingUser("SdpMktg");
        registrationPage.selectResource("SMS");
    }

    @And("I do not select any user from the {string} list")
    public void i_do_not_select_any_user_from_the_list(String listName) {
        switch (listName) {
            case "SP users":
                registrationPage.deselectAllSpUsers();
                break;
            case "Marketing Users":
                registrationPage.deselectAllMarketingUsers();
                break;
        }
    }

    @And("I do not select any {string}")
    public void i_do_not_select_any(String listName) {
        if ("Resources".equals(listName)) {
            registrationPage.deselectAllResources();
        }
    }
}