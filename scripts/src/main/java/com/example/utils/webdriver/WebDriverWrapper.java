package com.example.utils.webdriver;

import static com.example.utils.ConfigManager.getBoolean;
import static com.example.utils.ConfigManager.getInt;
import static com.example.utils.ConfigManager.getString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.example.Channel;
import com.example.ScrollDirection;
import com.google.common.io.Files;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class WebDriverWrapper {

	private static final long MAX_TIMEOUT = getInt("ui.timeout");
	private final WebDriver driver;
	private final String deviceId;
	private static final Channel CHANNEL = Channel.getChannel();

	public WebDriverWrapper(WebDriver driver, String deviceId) {
		this.driver = driver;
		this.deviceId = deviceId;
	}

	public WebElement findElement(@NonNull By locator) {
		return findElement(locator, MAX_TIMEOUT);
	}

	public WebElement findElement(@NonNull By locator, long timeout) {
		List<WebElement> elements = findElements(locator, timeout);
		if (!elements.isEmpty()) {
			WebElement elm = elements.get(0);
			if (CHANNEL == Channel.MOBILE_WEB) {
				((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(false)", elm);
			}
			return elm;
		}
		return null;
	}

	@Step
	public List<WebElement> findElements(@NonNull By locator) {
		return findElements(locator, MAX_TIMEOUT);
	}

	@Step
	public List<WebElement> findElements(@NonNull By locator, long timeout) {
		try {
			return until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator), timeout);
		} catch (Exception e) {
			log.error(String.format("device '%s' ::: error occurred while calling findElements(%s)", deviceId, locator),
					e);
			return Collections.emptyList();
		}
	}

	@Step
	public void click(@NonNull By locator) {
		try {
			WebElement elm = findElement(locator);
			if (elm != null && elm.isEnabled()) {
				elm.click();
			} else {
				log.error("unable to find or click element '{}'", locator);
			}
		} catch (StaleElementReferenceException e) {
			click(locator);
		}
	}

	public boolean isPresent(@NonNull By locator) {
		return isPresent(locator, MAX_TIMEOUT);
	}

	@Step
	public boolean isPresent(@NonNull By locator, long timeout) {
		return null != findElement(locator, timeout);
	}

	@Step
	public String getText(@NonNull By locator) {
		WebElement elm = findElement(locator);
		return elm != null ? elm.getText() : null;
	}

	@Step
	public Document getSource() {
		return Jsoup.parse(driver.getPageSource(), "", Parser.xmlParser());
	}

	@Step
	public void sendKeys(@NonNull By locator, CharSequence... keys) {
		try {
			WebElement elm = findElement(locator);
			if (elm != null) {
				elm.clear();
				elm.click();
				elm.sendKeys(keys);
				if (driver instanceof AndroidDriver) {
					((AndroidDriver<?>) driver).pressKey(new KeyEvent(AndroidKey.ENTER));
					if (!getBoolean("appium.use_appium_keyboard")) {
						((AppiumDriver<?>) driver).hideKeyboard();
					}
				}
			} else {
				log.error("device '{}' ::: unable to find element '{}'", deviceId, locator);
			}
		} catch (StaleElementReferenceException e) {
			sendKeys(locator, keys);
		}
	}

	public <T> T until(@NonNull ExpectedCondition<T> condition) {
		return until(condition, MAX_TIMEOUT);
	}

	@Step
	public <T> T until(@NonNull ExpectedCondition<T> condition, long timeout) {
		return new WebDriverWait(driver, timeout).pollingEvery(Duration.ofMillis(500))
				.ignoreAll(Arrays.asList(StaleElementReferenceException.class, NoSuchElementException.class))
				.until(condition);
	}

	@Step
	public File captureScreenshot(@NonNull String name) {
		File file = new File(getString("ui.screenshots.dir"),
				String.format("%s_%s.jpg", name, System.currentTimeMillis()));
		if (file.getParentFile() != null && !file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		byte[] data = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
		try {
			Files.write(data, file);
			try (InputStream stream = new FileInputStream(file)) {
				Allure.addAttachment(file.getName(), "image/jpg", stream, ".jpg");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("screenshot saved to '{}'", file.getAbsolutePath());

		return file;
	}

	public WebElement scrollUntilPresent(@NonNull By locator, ScrollDirection direction) {

		for (int i = 0; i < 21; i++) {
			try {
				return until(ExpectedConditions.presenceOfElementLocated(locator), 1);
			} catch (Exception e) {
				scroll(direction, 1);
			}
		}
		return null;
	}

	public void scroll(@NonNull ScrollDirection direction) {
		scroll(direction, 1);
	}

	public void scroll(@NonNull ScrollDirection direction, int times) {

		if (times < 1) {
			times = 1;
		}

		Dimension dimension = driver.manage().window().getSize();

		int offset = 500;
		int startX = 0;
		int startY = 0;
		int endX = 0;
		int endY = 0;

		log.debug("device '{}' ::: scrolling {} by {} pixels {} {}", deviceId, direction, offset,
				times, times < 2 ? "time" : "times");

		switch (direction) {
		case UP:
			startX = (int) (dimension.getWidth() * 0.5);
			startY = (int) (dimension.getHeight() * 0.5);
			endX = (int) (dimension.getWidth() * 0.5);
			endY = (int) (dimension.getHeight() * 0.5);
			break;
		case DOWN:
			startX = (int) (dimension.getWidth() * 0.5);
			startY = (int) (dimension.getHeight() * 0.5);
			endX = (int) (dimension.getWidth() * 0.5);
			endY = startY + offset;
			break;
		case LEFT:
			startX = (int) (dimension.getWidth() * 0.5);
			startY = (int) (dimension.getHeight() * 0.5);
			endX = startX - offset;
			endY = (int) (dimension.getHeight() * 0.5);
			break;
		case RIGHT:
			startX = (int) (dimension.getWidth() * 0.5);
			startY = (int) (dimension.getHeight() * 0.5);
			endX = startX + offset;
			endY = (int) (dimension.getHeight() * 0.5);
			break;
		}

		for (int i = 0; i < times; i++) {
			if (CHANNEL == Channel.MOBILE_WEB) {
				((JavascriptExecutor) driver).executeScript("window.scrollTo(arguments[0],arguments[1])", endX, endY);
			} else {
				io.appium.java_client.TouchAction<?> actions = new io.appium.java_client.TouchAction<>(
						(AppiumDriver<?>) driver);
				actions.press(PointOption.point(startX, startY))
						.waitAction(WaitOptions.waitOptions(Duration.ofMillis(100)))
						.moveTo(PointOption.point(endX, endY))
						.release().perform();
			}

		}
	}

	public void waitUntilPresent(By locator) {
		waitUntilPresent(locator, MAX_TIMEOUT);
	}

	public void waitUntilPresent(By locator, long timeout) {
		until(ExpectedConditions.presenceOfElementLocated(locator), timeout);
	}

}