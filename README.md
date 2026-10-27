# Signup Tests

Selenium 4 + TestNG UI test automation for the Rainbow signup flow, with Allure reporting, using the Page Object pattern. The 6-digit verification code is read from a Gmail inbox over IMAP.

## Tests covered (`SignupTest`)

**Happy path**
- Valid signup: new email, the emailed code, password, name, country, skip the avatar, refuse cookies and notifications, and the sign-out popup appears.
- Valid avatar upload: upload an image, confirm it with "Use this avatar" and reach the cookies/notifications step.

**Email**
- Invalid email format: Continue stays disabled.
- Empty email: Continue stays disabled.
- Already registered email: the page still moves to the code step (it does not reveal the account exists), and the mailbox gets the "existing account" mail with no code.

**Code and password**
- Wrong code: the "Invalid or expired code" error shows and the code step stays.
- Empty password, password under 12 characters, and a password missing character types: a field error shows and Continue stays disabled.
- Terms not accepted: Continue stays disabled.

**Name**
- Empty first name (never touched): Continue stays disabled.
- Empty names submitted with Enter: the "This field is mandatory" error shows.

Checks look at page state (a button disabled, an error element shown, the step still on screen), not at message wording. The one exception is the already-registered test, which reads the mail text because the mail is the thing under test.

## Tech stack

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.9+ |
| Selenium WebDriver | 4.38.0 |
| TestNG | 7.11.0 |
| Angus Mail (IMAP client) | 2.0.3 |
| Allure TestNG / Allure Maven plugin | 3.0.0 / 3.1.0 |
| AspectJ weaver (powers Allure `@Step`) | 1.9.26 |

## Project structure

```
Signup Tests
├── pom.xml                      Maven build: dependencies, surefire + Allure config
├── testng.xml                   Default suite: browser comes from -Dbrowser (default firefox)
├── testng-cross-browser.xml     Suite that runs Chrome and Firefox in one go
└── src
    ├── main/java/com/qa
    │   ├── config/ConfigReader.java     Reads config.properties from the classpath
    │   └── pages/SignupPage.java        Page object (@Step methods, one per signup step)
    └── test
        ├── java/com/qa
        │   ├── factory/DriverFactory.java      Builds the WebDriver for a browser name
        │   ├── listeners/AllureListener.java   Attaches a screenshot on test failure
        │   ├── mail/MailReader.java            Reads the code / mails from Gmail over IMAP
        │   └── tests/
        │       ├── BaseTest.java               Starts/quits the browser per test
        │       └── SignupTest.java
        └── resources
            ├── avatar.png                      Image used by the avatar upload test
            ├── config.example.properties       Template (committed)
            └── config.properties               Your real values (git-ignored)
```

## Setup

1. Install JDK 17+, Maven, Node.js (Allure 3 needs it) and the Chrome and Firefox browsers.
2. Create a Gmail App Password for the mailbox that receives the signup mails:
   - turn on 2-Step Verification on that Google account,
   - open https://myaccount.google.com/apppasswords, create a password named `selenium`,
   - copy the 16 characters without the spaces.
3. Create your local config from the template:

   ```bash
   cp src/test/resources/config.example.properties src/test/resources/config.properties
   ```

4. Edit `src/test/resources/config.properties`:

   ```properties
   signup.url=https://web.openrainbow.net/rb/2.181.0/subscribe
   signup.emailPrefix=your-mailbox+x
   signup.password=Test@Passw0rd123
   signup.existingEmail=your-mailbox+x0@gmail.com
   mail.user=your-mailbox@gmail.com
   mail.appPassword=your-app-password
   ```

   - Each run signs up as `<signup.emailPrefix><millis>@gmail.com`. Gmail delivers every `+` address to the same inbox, so each run gets a fresh address and no counter is needed.
   - `signup.existingEmail` must be an account that is already registered and whose mail arrives in the `mail.user` inbox. Create it once by signing up manually.
   - `config.properties` is git-ignored, so the App Password is never pushed.

## Run the tests

```bash
mvn clean test                                       # Firefox (the default)
mvn clean test -Dbrowser=chrome                      # Chrome
mvn clean test "-Dsuite=testng-cross-browser.xml"    # Chrome and Firefox in one run
mvn test -Dtest=SignupTest#testWrongCode             # a single test
```

Do not pass `-Dbrowser` together with the cross-browser suite: the command line value overrides the suite's browser setting.

## Notes on the tests

- Every test that reaches the code step sends a real email, and the valid signup tests create real accounts. A full run is slow (about 3 to 4 minutes per browser) because it waits for those mails (up to 90 seconds each).
- The page marks the Continue button as disabled in two ways: the real `disabled` attribute on most steps, and only `aria-disabled="true"` on the name step. `SignupPage.isContinueEnabled()` checks both.
- Button ids and the name field ids contain a generated uuid, so buttons are found by their visible text and the name fields by position. If the site is shown in another language, those text locators need updating.
- The avatar upload sends the image path straight to the hidden `<input type="file">`, so the OS file dialog never opens.

## Allure report

Build and open the report in your browser:

```bash
mvn allure:serve
```

Or generate it as files:

```bash
mvn allure:report
```

The report is written to `target/site/allure-maven-plugin/index.html`. Raw results are in `target/allure-results`. Run the tests first, since the report is built from those results. The first run downloads the Allure CLI into `.allure/`.

The report groups tests by Epic, Feature and Story with severity, shows the steps each test performed (the password is recorded without its value), and attaches a screenshot automatically when a test fails.
