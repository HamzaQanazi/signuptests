package com.qa.tests;

import com.qa.config.ConfigReader;
import com.qa.mail.MailReader;
import com.qa.pages.SignupPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Epic("Rainbow Signup")
@Feature("Registration")
public class SignupTest extends BaseTest {

    private SignupPage signup;

    @BeforeMethod
    public void openSignupPage() {
        signup = new SignupPage(driver);
        signup.openSignup();
    }

    @Test(
        testName = "Rainbow valid signup, skip avatar",
        description = "Sign up with a new email, the emailed code and valid details, then skip the avatar"
    )
    @Story("Signup with valid data")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Enter a new email, the 6-digit code from the mailbox, a password, name and country, skip the avatar and verify the last step is reached")
    public void testValidSignupSkipAvatar() {
        reachNameStep();

        signup.enterName("Test", "User");
        signup.selectCountry("Palestinian Territory, Occupied");
        signup.clickButton("Continue");

        signup.clickButton("Later");

        // cookies + notifications share one screen: Refuse declines both
        Assert.assertTrue(signup.isButtonVisible("Refuse"), "Signup did not reach the cookies/notifications step");
        signup.clickButton("Refuse");

        // signup is complete when the "thank you, you will be signed out" popup opens
        Assert.assertTrue(signup.isButtonVisible("Log out"), "Signup did not finish: no sign-out popup");
    }

    /** Email, real emailed code, password and terms done: the name step is on screen. */
    private void reachNameStep() {
        // fresh address every run: plus-addressing still lands in the same inbox
        String email = ConfigReader.get("signup.emailPrefix") + System.currentTimeMillis() + "@gmail.com";

        signup.enterEmail(email);
        signup.clickButton("Continue");

        signup.enterCode(MailReader.waitForCode(email));
        signup.enterPassword(ConfigReader.get("signup.password"));
        signup.acceptTerms();
        signup.clickButton("Continue");
    }

    /** Email step done, code step on screen. Any 6 digits can be typed: only the wrong-code test submits them. */
    private void reachCodeStep() {
        signup.enterEmail(ConfigReader.get("signup.emailPrefix") + System.currentTimeMillis() + "@gmail.com");
        signup.clickButton("Continue");
        signup.enterCode("123456");
    }

    @Test(
        testName = "Rainbow wrong code",
        description = "Submit a wrong 6-digit code and verify the signup does not move on"
    )
    @Story("Signup with wrong code")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Enter a wrong code with a valid password and terms, submit, and verify the code error shows and the code step stays")
    public void testWrongCode() {
        reachCodeStep();
        signup.enterPassword(ConfigReader.get("signup.password"));
        signup.acceptTerms();
        signup.clickButton("Continue");

        Assert.assertTrue(signup.isCodeErrorShown(), "No code error shown");
        Assert.assertTrue(signup.isCodeStepShown(), "Left the code step with a wrong code");
    }

    @Test(
        testName = "Rainbow empty password",
        description = "Leave the password empty and verify the field error shows and Continue is disabled"
    )
    @Story("Signup password validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Focus the password field, leave it empty, and verify a field error shows and Continue stays disabled")
    public void testEmptyPassword() {
        reachCodeStep();
        // the field only counts as touched once it has been typed in, so type one character and delete it
        signup.enterPassword("a");
        signup.clearPassword();
        signup.leavePassword();
        signup.acceptTerms();

        Assert.assertTrue(signup.isFieldErrorShown(), "No error for an empty password");
        Assert.assertFalse(signup.isContinueEnabled(), "Continue is enabled with an empty password");
    }

    @Test(
        testName = "Rainbow short password",
        description = "Use a password under 12 characters and verify the field error shows and Continue is disabled"
    )
    @Story("Signup password validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enter a valid-looking but too short password and verify a field error shows and Continue stays disabled")
    public void testShortPassword() {
        reachCodeStep();
        signup.enterPassword("Ab1!");
        signup.leavePassword();
        signup.acceptTerms();

        Assert.assertTrue(signup.isFieldErrorShown(), "No error for a short password");
        Assert.assertFalse(signup.isContinueEnabled(), "Continue is enabled with a short password");
    }

    @Test(
        testName = "Rainbow password missing character types",
        description = "Use a 12+ character password with only lowercase letters and verify the field error shows"
    )
    @Story("Signup password validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enter a long password without upper case, numbers and special characters and verify a field error shows and Continue stays disabled")
    public void testPasswordMissingCharacterTypes() {
        reachCodeStep();
        signup.enterPassword("abcdefghijklmnop");
        signup.leavePassword();
        signup.acceptTerms();

        Assert.assertTrue(signup.isFieldErrorShown(), "No error for a weak password");
        Assert.assertFalse(signup.isContinueEnabled(), "Continue is enabled with a weak password");
    }

    @Test(
        testName = "Rainbow terms not accepted",
        description = "Fill everything except the terms checkbox and verify Continue stays disabled"
    )
    @Story("Signup terms and privacy")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Enter a valid code and password, leave the terms unticked and verify Continue is disabled")
    public void testTermsNotAccepted() {
        reachCodeStep();
        signup.enterPassword(ConfigReader.get("signup.password"));

        Assert.assertFalse(signup.isContinueEnabled(), "Continue is enabled without accepting the terms");
    }

    @Test(
        testName = "Rainbow invalid email",
        description = "Enter an email without a valid format and verify Continue is disabled"
    )
    @Story("Signup email validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Type text that is not an email address and verify the Continue button stays disabled")
    public void testInvalidEmail() {
        signup.enterEmail("not-an-email");

        Assert.assertFalse(signup.isContinueEnabled(), "Continue is enabled with an invalid email");
    }

    @Test(
        testName = "Rainbow already registered email",
        description = "Sign up with an existing account's email and verify the page does not reveal it and the sign-in mail is sent"
    )
    @Story("Signup with an existing email")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Enter an already registered email, verify the page still moves to the code step, and verify the mailbox gets the 'existing account' mail with no code")
    public void testAlreadyRegisteredEmail() {
        String email = ConfigReader.get("signup.existingEmail");
        long start = System.currentTimeMillis() - 60_000; // allow for clock skew with the mail server

        signup.enterEmail(email);
        signup.clickButton("Continue");

        // same screen as a new email, so the page does not reveal that the account exists
        Assert.assertTrue(signup.isCodeStepShown(), "Did not move to the code step");

        // the mail text is the thing under test here, so this one check reads it
        String mail = visibleText(MailReader.waitForMailText(email, start));
        Assert.assertTrue(mail.contains("existing Rainbow account"), "Unexpected mail: " + mail);

        Matcher code = Pattern.compile("\\b\\d{6}\\b").matcher(mail);
        boolean hasCode = code.find();
        Assert.assertFalse(hasCode, "The mail contains a 6-digit number: " + (hasCode ? code.group() : ""));
    }

    /** The mail body as a reader sees it: no style blocks, no tags, single spaces. */
    private static String visibleText(String html) {
        return html.replaceAll("(?s)<style.*?</style>", " ")
            .replaceAll("<[^>]*>", " ")
            .replace("&nbsp;", " ")
            .replaceAll("\\s+", " ");
    }

    @Test(
        testName = "Rainbow empty email",
        description = "Leave the email empty and verify Continue is disabled"
    )
    @Story("Signup email validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Open the signup page without typing anything and verify the Continue button is disabled")
    public void testEmptyEmail() {
        Assert.assertFalse(signup.isContinueEnabled(), "Continue is enabled with an empty email");
    }

    @Test(
        testName = "Rainbow empty first name",
        description = "Leave the first name empty and verify Continue is disabled"
    )
    @Story("Signup name validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Reach the name step, fill the last name and country but not the first name, and verify Continue is disabled")
    public void testEmptyFirstName() {
        reachNameStep();

        // the first name is never touched, so no error shows but Continue stays disabled
        signup.enterLastName("User");
        signup.selectCountry("Palestinian Territory, Occupied");

        Assert.assertFalse(signup.isContinueEnabled(), "Continue is enabled with an empty first name");
    }

    @Test(
        testName = "Rainbow empty names submitted",
        description = "Press Enter with both names empty and verify the mandatory error shows"
    )
    @Story("Signup name validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Reach the name step, press Enter without typing the names and verify the field error shows")
    public void testEmptyNamesSubmitted() {
        reachNameStep();

        signup.submitNameStep();

        Assert.assertTrue(signup.isFieldErrorShown(), "No mandatory error after submitting empty names");
    }

    @Test(
        testName = "Rainbow valid avatar upload",
        description = "Upload a valid image as the avatar and verify the signup moves on"
    )
    @Story("Signup avatar")
    @Severity(SeverityLevel.NORMAL)
    @Description("Upload a valid image, confirm it with 'Use this avatar' and verify the cookies/notifications step is reached")
    public void testValidAvatarUpload() throws Exception {
        reachNameStep();
        signup.enterName("Test", "User");
        signup.selectCountry("Palestinian Territory, Occupied");
        signup.clickButton("Continue");

        signup.uploadAvatar(Path.of(SignupTest.class.getResource("/avatar.png").toURI()).toString());
        signup.clickButton("Use this avatar");

        Assert.assertTrue(signup.isButtonVisible("Refuse"), "Did not reach the cookies/notifications step");
    }
}
