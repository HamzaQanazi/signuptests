package com.qa.factory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Simple Factory: the only place that knows how to build each browser.
 * To support a new browser, add one entry to BROWSERS.
 */
public final class DriverFactory {

    private static final Map<String, Supplier<WebDriver>> BROWSERS = Map.of(
        "chrome", ChromeDriver::new,
        "firefox", FirefoxDriver::new
    );

    private DriverFactory() {
    }

    public static WebDriver create(String browser) {
        Supplier<WebDriver> maker = BROWSERS.get(browser.trim().toLowerCase(Locale.ROOT));
        if (maker == null) {
            throw new IllegalArgumentException(
                "Unsupported browser '" + browser + "': use one of " + new TreeSet<>(BROWSERS.keySet()));
        }
        return maker.get();
    }
}
