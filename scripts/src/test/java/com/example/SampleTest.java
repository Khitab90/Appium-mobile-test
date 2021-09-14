package com.example;

import org.testng.annotations.Test;

import com.example.po.YoutubeHome;
import com.example.utils.BaseTest;

public class SampleTest extends BaseTest {

	@Test
	public void youtubeSearch() {

		YoutubeHome page = new YoutubeHome(getDriver(), getDeviceId());
		page.search("Appium");
	}

}
