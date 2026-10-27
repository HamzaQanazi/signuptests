package com.qa.pages;

import com.qa.config.ConfigReader;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class SignupPage {

    // ponytail: ids like button-<uuid> and textField__input--<uuid> change per load, so buttons are found by text
    private static final By FIRST_AND_LAST_NAME = By.xpath("//form//input[starts-with(@id,'textField__input')]");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public SignupPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @Step("Open the Rainbow signup page")
    public void openSignup() {
        driver.get(ConfigReader.get("signup.url"));
    }

    @Step("Enter email '{0}'")
    public void enterEmail(String email) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("inputEmail"))).sendKeys(email);
    }

    @Step("Click '{0}'")
    public void clickButton(String text) {
        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[normalize-space()='" + text + "']"))).click();
    }

    @Step("Enter the 6-digit code")
    public void enterCode(String code) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("codeInput"))).sendKeys(code);
    }

    public void enterPassword(String pass) {
        // not @Step: Allure would write the password argument into allure-results
        Allure.step("Enter password");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("pwdInput"))).sendKeys(pass);
    }

    @Step("Accept the terms and privacy policy")
    public void acceptTerms() {
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//rb-checkbox//label/div"))).click();
    }

    @Step("Enter name '{0} {1}'")
    public void enterName(String first, String last) {
        List<WebElement> fields = nameFields();
        fields.get(0).sendKeys(first);
        fields.get(1).sendKeys(last);
    }

    @Step("Enter last name '{0}'")
    public void enterLastName(String last) {
        // does not touch the first name: even sendKeys("") would focus it and change the form state
        nameFields().get(1).sendKeys(last);
    }

    @Step("Press Enter on the name step")
    public void submitNameStep() {
        nameFields().get(0).sendKeys(Keys.ENTER);
    }

    private List<WebElement> nameFields() {
        return wait.until(d -> {
            List<WebElement> found = d.findElements(FIRST_AND_LAST_NAME);
            return found.size() >= 2 && found.get(0).isDisplayed() ? found : null;
        });
    }

    @Step("Select country '{0}'")
    public void selectCountry(String country) {
        WebElement select = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//form//select")));
        new Select(select).selectByVisibleText(country);
    }

    @Step("Check the '{0}' button is shown")
    public boolean isButtonVisible(String text) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//button[normalize-space()='" + text + "']"))).isDisplayed();
    }

    @Step("Delete the typed character from the password field")
    public void clearPassword() {
        driver.findElement(By.id("pwdInput")).sendKeys(Keys.BACK_SPACE);
    }

    @Step("Leave the password field")
    public void leavePassword() {
        // errors can show on blur, so tab out like a user would
        driver.findElement(By.id("pwdInput")).sendKeys(Keys.TAB);
    }

    @Step("Check a field error is shown")
    public boolean isFieldErrorShown() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".rb-badge__text"))).isDisplayed();
    }

    @Step("Check the wrong-code error is shown")
    public boolean isCodeErrorShown() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("h2.authWindowContent__information--error"))).isDisplayed();
    }

    @Step("Check the code field is still shown")
    public boolean isCodeStepShown() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("codeInput"))).isDisplayed();
    }

    @Step("Check the Continue button is enabled")
    public boolean isContinueEnabled() {
        WebElement button = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//button[normalize-space()='Continue']")));
        // some steps use the real disabled attribute, the name step only sets aria-disabled="true"
        return button.isEnabled() && !"true".equals(button.getDomAttribute("aria-disabled"));
    }

    @Step("Upload the avatar image")
    public void uploadAvatar(String absolutePath) {
        // the visible "Browse" button only opens the OS dialog; the hidden file input takes the path directly
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']")))
            .sendKeys(absolutePath);
    }
}
