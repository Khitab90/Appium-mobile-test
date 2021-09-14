package com.example.ios;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.example.utils.webdriver.IOSUtilities;
import com.google.common.io.Files;

import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.IOSMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;

public class SampleIOSTest {

	private AppiumDriverLocalService service;
	private IOSDriver<?> driver;

	@BeforeClass(alwaysRun = true)
	public void initDriver() {
		service = AppiumDriverLocalService.buildDefaultService();
		service.start();
		driver = new IOSDriver<>(service.getUrl(), getCapabilities());
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

		// asserting the presence of an element in the current visible screen
		driver.findElement(By.xpath("//XCUIElementTypeSearchField")).click();
		driver.findElement(By.xpath("//XCUIElementTypeSearchField")).sendKeys("Appium");
		Files.write(driver.getScreenshotAs(OutputType.BYTES), new File("ios-test.jpg"));
	}

	private DesiredCapabilities getCapabilities() {
		Optional<String> deviceId = IOSUtilities.getConnectedDevices().keySet().stream().findFirst();
		if (!deviceId.isPresent()) {
			throw new RuntimeException("please connect 1 or more ios phones");
		}
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, Platform.IOS);
		capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.IOS_XCUI_TEST);
		capabilities.setCapability(MobileCapabilityType.UDID, deviceId.get());
		capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "iphone simulator");
		capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, "14");
		capabilities.setCapability(IOSMobileCapabilityType.BUNDLE_ID, "com.apple.MobileAddressBook");
		return capabilities;
	}

}
