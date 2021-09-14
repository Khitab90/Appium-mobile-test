package com.example.mobile_web;

import static org.testng.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.example.utils.webdriver.ADBUtilities;
import com.example.utils.webdriver.IOSUtilities;
import com.google.common.io.Files;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;

public class SampleMobileTest {

	private AppiumDriverLocalService service;
	private AppiumDriver<?> driver;

	@BeforeClass(alwaysRun = true)
	public void initDriver() {
		service = AppiumDriverLocalService.buildDefaultService();
		service.start();

		boolean isAndroid = false;
		Optional<String> deviceId = IOSUtilities.getConnectedDevices().keySet().stream().findFirst();
		if (!deviceId.isPresent()) {
			deviceId = ADBUtilities.getConnectedDevices().keySet().stream().findFirst();
			isAndroid = true;
		}
		if (!deviceId.isPresent()) {
			throw new RuntimeException("please connect 1 or more android or ios phones");
		}
		driver = isAndroid ? new AndroidDriver<>(service.getUrl(), getCapabilities(isAndroid, deviceId.get()))
				: new IOSDriver<>(service.getUrl(), getCapabilities(isAndroid, deviceId.get()));
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
	}

	@AfterClass(alwaysRun = true)
	public void tearDownDriver() {
		if (driver != null) {
			driver.closeApp();
		}
		if (service != null && service.isRunning()) {
			service.stop();
		}
	}

	@Test
	public void youtubeSearch() throws IOException {
		driver.get("https://youtube.com");
		assertNotNull(driver.findElement(By.xpath("//*[@aria-label='Action menu']")));
		driver.findElement(By.xpath("//*[@aria-label='Search YouTube' and contains(@class, 'avatar-button')]")).click();
		driver.findElement(By.name("search")).sendKeys("Appium");
		assertNotNull(driver.findElement(By.xpath("//*[@aria-label='Action menu']")));
		Files.write(driver.getScreenshotAs(OutputType.BYTES), new File("mobile-test.jpg"));
	}

	private DesiredCapabilities getCapabilities(boolean isAndroid, String deviceId) {

		DesiredCapabilities capabilities = new DesiredCapabilities();
		if (isAndroid) {
			capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, Platform.ANDROID);
			capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ANDROID_UIAUTOMATOR2);
			capabilities.setCapability(MobileCapabilityType.UDID, deviceId);
			capabilities.setCapability(MobileCapabilityType.BROWSER_NAME, BrowserType.CHROME);
		} else {
			capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, Platform.IOS);
			capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.IOS_XCUI_TEST);
			capabilities.setCapability(MobileCapabilityType.UDID, deviceId);
			capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "iphone simulator");
			capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, "14");
			capabilities.setCapability(MobileCapabilityType.BROWSER_NAME, BrowserType.SAFARI);
		}
		return capabilities;
	}

}
