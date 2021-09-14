package com.example.android;

import static org.testng.Assert.assertNotNull;

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

import com.example.utils.webdriver.ADBUtilities;
import com.google.common.io.Files;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;

public class SampleAndroidTest {

	private AppiumDriverLocalService service;
	private AndroidDriver<?> driver;

	@BeforeClass(alwaysRun = true)
	public void initDriver() {

		// creating appium session programmatically
		service = AppiumDriverLocalService.buildDefaultService();
		service.start();

		// creating android driver with appium session url and capabilities
		driver = new AndroidDriver<>(service.getUrl(), getCapabilities());

		// setting implicit wait to wait before failing to find an element
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
	}

	@AfterClass(alwaysRun = true)
	public void tearDownDriver() {

		// closing the app
		if (driver != null) {
			driver.closeApp();
		}
		// terminating the appium session
		if (service != null && service.isRunning()) {
			service.stop();
		}
	}

	@Test
	public void youtubeSearch() throws IOException {

		// asserting the presence of an element in the current visible screen
		assertNotNull(driver.findElement(By.xpath("//*[contains(@resource-id, 'contextual_menu_anchor')]")));

		// find an element in the current visible screen and clicking on that element
		driver.findElement(By.xpath("//*[@content-desc = 'Search']")).click();

		// find an element in the current visible screen and typing text in that element
		driver.findElement(By.id("com.google.android.youtube:id/search_edit_text")).sendKeys("Appium");

		// asserting the presence of an element in the current visible screen
		assertNotNull(driver.findElement(By.xpath("//*[contains(@resource-id, 'text')]")));

		// capturing screenshot of the current visible screen
		Files.write(driver.getScreenshotAs(OutputType.BYTES), new File("android-test.jpg"));
	}

	private DesiredCapabilities getCapabilities() {
		Optional<String> deviceId = ADBUtilities.getConnectedDevices().keySet().stream().findFirst();
		if (!deviceId.isPresent()) {
			throw new RuntimeException("please connect 1 or more android phones");
		}
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, Platform.ANDROID);
		capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ANDROID_UIAUTOMATOR2);
		capabilities.setCapability(MobileCapabilityType.UDID, deviceId.get());
		capabilities.setCapability(AndroidMobileCapabilityType.APP_PACKAGE, "com.google.android.youtube");
		capabilities.setCapability(AndroidMobileCapabilityType.APP_ACTIVITY,
				"com.google.android.apps.youtube.app.WatchWhileActivity");
		return capabilities;
	}

}
