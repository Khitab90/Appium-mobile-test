package com.example.utils.po;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.LoadableComponent;

import com.example.Channel;
import com.example.utils.CommonUtilities;
import com.example.utils.ConfigManager;
import com.example.utils.webdriver.WebDriverWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.appium.java_client.MobileBy;
import lombok.NonNull;

public class AbstractBasePage<T extends AbstractBasePage<T>> extends LoadableComponent<T> {

	protected final WebDriver driver;
	protected final String deviceId;
	protected final WebDriverWrapper utils;

	protected static final Channel CHANNEL = Channel.getChannel();

	@Override
	protected void load() {

	}

	@Override
	protected void isLoaded() throws Error {
	}

	private static final Map<Class<?>, Map<String, String>> LOCATORS = new ConcurrentHashMap<>();

	protected AbstractBasePage(WebDriver driver, String deviceId, String locatorFile) {

		this.driver = driver;
		this.deviceId = deviceId;
		this.utils = new WebDriverWrapper(driver, deviceId);

		if (!LOCATORS.containsKey(this.getClass())) {
			LOCATORS.put(this.getClass(), getLocators(locatorFile));
		}

	}

	private Map<String, String> getLocators(String locatorFile) {

		File file = new File(ConfigManager.getString("ui.locators.dir"), locatorFile);
		if (!file.exists()) {
			throw new RuntimeException(String.format("file '%s' does not exists", file.getAbsolutePath()));
		}
		try {
			Map<String, Map<String, String>> map = new ObjectMapper().readValue(file,
					new TypeReference<Map<String, Map<String, String>>>() {
					});
			Map<String, String> props = new ConcurrentHashMap<>();
			map.forEach((k, v) -> v.forEach((key, value) -> {
				if (key.equalsIgnoreCase(CHANNEL.toString())) {
					props.put(k + "." + key, value);
				}
			}));
			return props;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected By getLocator(String name, Object... args) {
		Map<String, String> locator = LOCATORS.get(this.getClass());
		if (locator == null) {
			throw new RuntimeException(
					String.format("unable to fetch locators for class '%s'", this.getClass().getName()));
		}
		String loc = locator.get(name + "." + CHANNEL.toString());
		if (CommonUtilities.isBlank(loc)) {
			throw new RuntimeException(
					String.format("unable to find the locator '%s' for channel '%s'", name, CHANNEL.toString()));
		}
		return getBy(loc, args);
	}

	private By getBy(@NonNull String str, Object... args) {

		int index = str.indexOf("=");

		String type = str.substring(0, index).trim().toLowerCase().replace("_", "");
		String value = String.format(str.substring(index + 1).trim(), args);

		switch (type) {
		case "xpath":
			return By.xpath(value);
		case "css":
			return By.cssSelector(value);
		case "id":
			return By.id(value);
		case "tag":
			return By.tagName(value);
		case "class":
			return By.className(value);
		case "link":
			return By.linkText(value);
		case "partiallink":
			return By.partialLinkText(value);
		case "name":
			return By.name(value);
		case "accessibilityid":
			return MobileBy.AccessibilityId(value);
		case "iosnspredicate":
			return MobileBy.iOSNsPredicateString(value);
		case "iosclasschain":
			return MobileBy.iOSClassChain(value);
		default:
			return null;
		}
	}

	protected final void sleep(long seconds) {
		try {
			TimeUnit.SECONDS.sleep(seconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
