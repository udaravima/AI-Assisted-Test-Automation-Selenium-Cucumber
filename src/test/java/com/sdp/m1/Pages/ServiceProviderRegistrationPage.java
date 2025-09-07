package com.sdp.m1.Pages;

import com.epam.healenium.SelfHealingDriver;
import com.epam.healenium.SelfHealingDriverWait;
import com.sdp.m1.Utils.TestConfigs;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

public class ServiceProviderRegistrationPage {

    private final SelfHealingDriver driver;
    private final SelfHealingDriverWait wait;

    public ServiceProviderRegistrationPage(SelfHealingDriver driver, SelfHealingDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        PageFactory.initElements(driver, this);
    }

    // --- Field Locators ---
    @FindBy(id = "spId")
    private WebElement serviceProviderIdInput;

    @FindBy(id = "companyName")
    private WebElement companyNameInput;

    @FindBy(id = "address")
    private WebElement addressInput;

    @FindBy(id = "description")
    private WebElement descriptionInput;

    @FindBy(id = "whiteListedUsers")
    private WebElement whiteListedUsersInput;

    @FindBy(id = "userNames")
    private WebElement spUsersSelect;

    @FindBy(id = "allowedMarketingUsers")
    private WebElement marketingUsersSelect;

    @FindBy(id = "allowedNcses")
    private WebElement resourcesSelect;

    @FindBy(id = "submitApplication")
    private WebElement submitButton;

    // --- Message Locators ---
    // TODO: Replace with a more specific locator for error messages if available
    @FindBy(xpath = "//div[contains(@class, 'error')]/span")
    private WebElement errorMessageContainer;

    // TODO: Replace with a more specific locator for the success message
    @FindBy(className = "success-msg") // Placeholder locator
    private WebElement successMessageContainer;

    // --- Interaction Methods ---

    public void navigateToPage() {
        // The URL is derived from the SRS JSON, but TestConfigs should be the source of
        // truth for base URLs
        driver.get(TestConfigs.getBaseUrl() + "/registerServiceProvider.html");
    }

    public void enterServiceProviderId(String spId) {
        if (spId != null) {
            serviceProviderIdInput.sendKeys(spId);
        } else {
            serviceProviderIdInput.clear();
        }
    }

    public void enterCompanyName(String companyName) {
        if (companyName != null) {
            companyNameInput.sendKeys(companyName);
        } else {
            companyNameInput.clear();
        }
    }

    public void enterAddress(String address) {
        addressInput.sendKeys(address);
    }

    public void enterDescription(String description) {
        descriptionInput.sendKeys(description);
    }

    public void enterWhiteListedUsers(String users) {
        whiteListedUsersInput.sendKeys(users);
    }

    public void selectSpUser(String userName) {
        new Select(spUsersSelect).selectByVisibleText(userName);
    }

    public void deselectAllSpUsers() {
        new Select(spUsersSelect).deselectAll();
    }

    public void selectMarketingUser(String userName) {
        new Select(marketingUsersSelect).selectByVisibleText(userName);
    }

    public void deselectAllMarketingUsers() {
        new Select(marketingUsersSelect).deselectAll();
    }

    public void selectResource(String resource) {
        new Select(resourcesSelect).selectByVisibleText(resource);
    }

    public void deselectAllResources() {
        new Select(resourcesSelect).deselectAll();
    }

    public void clickSubmitButton() {
        submitButton.click();
    }

    // --- Assertion Methods ---

    public String getErrorMessage() {
        // This assumes errors appear in a single container. This may need refinement.
        return wait.until(d -> errorMessageContainer.getText());
    }

    public String getSuccessMessage() {
        return wait.until(d -> successMessageContainer.getText());
    }
}
