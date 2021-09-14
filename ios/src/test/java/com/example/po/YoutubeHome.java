package com.example.po;

import org.openqa.selenium.WebDriver;

import com.example.ScrollDirection;
import com.example.utils.po.AbstractBasePage;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YoutubeHome extends AbstractBasePage<YoutubeHome> {

	public YoutubeHome(WebDriver driver, String deviceId) {
		super(driver, deviceId, "home.json");
		this.get();
	}

	@Override
	protected void isLoaded() throws Error {
		utils.waitUntilPresent(getLocator("home_page_load"));
	}

	@Step
	public void search(String query) {
		log.info("search test");
		utils.click(getLocator("search_button"));
		utils.sendKeys(getLocator("search_text"), query);
		utils.waitUntilPresent(getLocator("search_page_load"));
		utils.captureScreenshot("sample");
		utils.scroll(ScrollDirection.UP, 5);
	}

}
