package com.qa.listeners;

import com.qa.tests.BaseTest;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;

public class AllureListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        if (!(result.getInstance() instanceof BaseTest)) {
            return;
        }
        WebDriver driver = ((BaseTest) result.getInstance()).getDriver();
        if (!(driver instanceof TakesScreenshot)) {
            return;
        }
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Allure.attachment("Failure screenshot", "image/png", new ByteArrayInputStream(png),
                AttachmentOptions.withFileExtension(".png"));
        } catch (RuntimeException e) {
            // browser already gone: skip the screenshot, never mask the real failure
            System.err.println("Could not take failure screenshot: " + e.getMessage());
        }
    }
}
