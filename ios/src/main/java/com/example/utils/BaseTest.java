package com.example.utils;

import static com.example.utils.ConfigManager.getBoolean;
import static com.example.utils.ConfigManager.getString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.ITestAnnotation;
import org.testng.annotations.Listeners;
import org.testng.xml.XmlSuite.ParallelMode;
import org.testng.xml.XmlTest;

import com.example.Channel;
import com.example.listener.RetryAnalyzer;
import com.example.utils.webdriver.ADBUtilities;
import com.example.utils.webdriver.AppiumServiceFactory;
import com.example.utils.webdriver.IOSUtilities;
import com.example.utils.webdriver.WebDriverFactory;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidStartScreenRecordingOptions;
import io.appium.java_client.ios.IOSStartScreenRecordingOptions;
import io.appium.java_client.ios.IOSStartScreenRecordingOptions.VideoQuality;
import io.appium.java_client.screenrecording.CanRecordScreen;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Listeners(BaseTest.class)
public class BaseTest implements ITestListener {

	private static final Channel CHANNEL = Channel.getChannel();
	private static final String IOS = Channel.NATIVE_IOS.toString();
	private static final String ANDROID = Channel.NATIVE_ANDROID.toString();

	private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();
	private static final ThreadLocal<String> CURRENT_RUNNING_DEVICE = new ThreadLocal<>();
	private static final String VIDEOS_DIRECTORY = getString("ui.videos.dir");
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy-HH-mm-SS");

	private static final List<String> DEVICES = new Vector<>();

	@BeforeSuite(alwaysRun = true)
	public final void onBeforeSuite(ITestContext context) {
		int connectedDevices = getConnectedDevicesCount();
		if (connectedDevices == 0) {
			throw new RuntimeException(
					String.format("please attach %s phones to the machine to run tests", getDeviceType()));
		} else {
			log.info("connected {} phones => {}", getDeviceType(), DEVICES);
		}
		setThreadCount(context, connectedDevices);
	}

	private int getConnectedDevicesCount() {
		if (DEVICES.isEmpty()) {
			if (CHANNEL == Channel.NATIVE_ANDROID || CHANNEL == Channel.MOBILE_WEB) {
				DEVICES.addAll(ADBUtilities.getConnectedDevices().keySet());
			}
			if (CHANNEL == Channel.NATIVE_IOS || CHANNEL == Channel.MOBILE_WEB) {
				DEVICES.addAll(IOSUtilities.getConnectedDevices().keySet());
			}
		}
		return DEVICES.size();
	}

	private void setThreadCount(ITestContext context, int connectedDevices) {
		context.getCurrentXmlTest().setThreadCount(connectedDevices);
		context.getCurrentXmlTest().getSuite().setDataProviderThreadCount(connectedDevices);
		context.getCurrentXmlTest().getSuite().setThreadCount(connectedDevices);
	}

	@AfterSuite(alwaysRun = true)
	public final void onAfterSuite(ITestContext context) {
		DRIVER.remove();
		CURRENT_RUNNING_DEVICE.remove();
		DEVICES.clear();

		if (ConfigManager.getInt("retry.count", 0) > 0) {
			removeDuplicateTestCases(context);
		}
	}

	private boolean isEqual(ITestResult resA, ITestResult resB) {

		if (resA == null && resB == null) {
			return true;
		}
		if (resA == null || resB == null) {
			return false;
		}
		if (resA.getClass().equals(resB.getClass()) && resA.getMethod().equals(resB.getMethod())
				&& resA.getStatus() == resB.getStatus() && resA.getParameters() != null
				&& resA.getParameters().length > 0 && resB.getParameters() != null && resB.getParameters().length > 0
				&& resA.getParameters().length == resB.getParameters().length) {
			for (int i = 0; i < resA.getParameters().length; i++) {
				if (resA.getParameters() != null && resB.getParameters() != null
						&& resA.getParameters()[i] != null && resB.getParameters()[i] != null
						&& !resA.getParameters()[i].equals(resB.getParameters()[i])) {
					return false;
				}
			}
		}
		return true;
	}

	private void removeDuplicateTestCases(ITestContext context) {

		context.getSkippedTests().getAllResults().forEach(result -> {
			if (RetryAnalyzer.getRetriedtests().contains(result)) {
				context.getSkippedTests().removeResult(result.getMethod());
			}
		});
		context.getFailedTests().getAllMethods().forEach(method -> {
			Set<ITestResult> results = context.getFailedTests().getResults(method);
			if (results != null && !results.isEmpty()) {
				ITestResult[] arr = results.toArray(new ITestResult[results.size()]);
				for (int i = 0; i < arr.length; i++) {
					for (int j = i + 1; j < arr.length; j++) {
						if (isEqual(arr[i], arr[j])) {
							context.getFailedTests().removeResult(arr[j]);
						}
					}
				}
			}
		});
	}

	@BeforeClass(alwaysRun = true)
	public final void beforeClass(ITestContext testContext, XmlTest xmlTest) {
		ParallelMode mode = getParallelMode(testContext, xmlTest);
		if (mode == null || mode == ParallelMode.NONE || mode == ParallelMode.CLASSES) {
			synchronized (BaseTest.class) {
				createWebDriver();
			}
		}
	}

	private void startVideoRecording() {
		if (CURRENT_RUNNING_DEVICE.get() == null) {
			return;
		}
		if (!isEmulatorOrSimulator()) {
			log.warn("screen recording is only for emulators/simulators");
			return;
		}
		if (DRIVER.get() != null && getBoolean("ui.record_video")) {
			CanRecordScreen recorder = (CanRecordScreen) getDriver();
			if (DRIVER.get() instanceof AndroidDriver) {
				recorder.startRecordingScreen(
						new AndroidStartScreenRecordingOptions().withVideoSize("1000x1000")
								.withTimeLimit(Duration.ofMinutes(10)));
			} else {
				recorder.startRecordingScreen(new IOSStartScreenRecordingOptions()
						.withVideoQuality(VideoQuality.MEDIUM).withTimeLimit(Duration.ofMinutes(10)));
			}
		}

	}

	private boolean isEmulatorOrSimulator() {
		return CURRENT_RUNNING_DEVICE.get() != null && CURRENT_RUNNING_DEVICE.get().contains("-");
	}

	private void stopVideoRecording(File file, boolean save) {
		if (CURRENT_RUNNING_DEVICE.get() == null) {
			return;
		}
		if (!isEmulatorOrSimulator()) {
			log.warn("screen recording is only for emulators/simulators");
			return;
		}
		if (DRIVER.get() != null && getBoolean("ui.record_video")) {
			CanRecordScreen recorder = (CanRecordScreen) getDriver();
			if (!save) {
				recorder.stopRecordingScreen();
			} else {
				String data = recorder.stopRecordingScreen();
				try {
					if (file.getParentFile() != null && !file.getParentFile().exists()) {
						file.getParentFile().mkdirs();
					}
					FileUtils.writeByteArrayToFile(file, Base64.getDecoder().decode(data));
					try (InputStream stream = new FileInputStream(file)) {
						Allure.addAttachment(file.getName(), "video/mp4", stream, ".mp4");
					}
					log.info("video of execution saved to => {}", file.getAbsolutePath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private ParallelMode getParallelMode(ITestContext testContext, XmlTest xmlTest) {
		ParallelMode mode = testContext.getCurrentXmlTest().getParallel();
		if (mode == null) {
			mode = xmlTest.getSuite().getParallel();
		}
		return mode;
	}

	@AfterClass(alwaysRun = true)
	public final void afterClass(ITestContext testContext, XmlTest xmlTest) {
		ParallelMode mode = getParallelMode(testContext, xmlTest);
		if (mode == null || mode == ParallelMode.NONE || mode == ParallelMode.CLASSES) {
			synchronized (BaseTest.class) {
				removeWebDriver();
			}
		}
	}

	private void removeWebDriver() {
		String device = CURRENT_RUNNING_DEVICE.get();
		if (device != null) {
			if (DRIVER.get() instanceof AppiumDriver) {
				((AppiumDriver<?>) DRIVER.get()).closeApp();
				AppiumServiceFactory.stopLocalAppiumService(device);
				DEVICES.add(device);
				DRIVER.remove();
				CURRENT_RUNNING_DEVICE.remove();
			}
		} else {
			log.error("unable to get the current runnning device");
		}
	}

	@BeforeMethod(alwaysRun = true)
	public final void beforeMethod(ITestContext testContext, XmlTest xmlTest) {
		ParallelMode mode = testContext.getCurrentXmlTest().getParallel();
		if (mode == ParallelMode.METHODS) {
			synchronized (BaseTest.class) {
				createWebDriver();
			}
		}
		startVideoRecording();
	}

	@AfterMethod(alwaysRun = true)
	public final void afterMethod(ITestContext testContext, XmlTest xmlTest) {
		ParallelMode mode = testContext.getCurrentXmlTest().getParallel();
		if (mode == ParallelMode.METHODS) {
			synchronized (BaseTest.class) {
				removeWebDriver();
			}
		}
	}

	private void createWebDriver() {

		String deviceId = null;
		String mobileWebOS = getString("ui.mobile_web.os");
		if (mobileWebOS != null) {
			mobileWebOS = mobileWebOS.trim().toLowerCase();
		}
		Boolean isAndroid = null;

		if (CHANNEL != Channel.MOBILE_WEB) {
			if (DEVICES.isEmpty()) {
				throw new RuntimeException(
						String.format("please connect %s phones to the machine and re-run the tests",
								CHANNEL.toString()));
			} else {
				isAndroid = CHANNEL == Channel.NATIVE_ANDROID;
				deviceId = DEVICES.remove(0);
			}
		} else {
			if (Channel.NATIVE_ANDROID.toString().equals(mobileWebOS)) {
				Collection<String> keys = ADBUtilities.getConnectedDevices().keySet();
				Optional<String> optional = DEVICES.stream().filter(keys::contains).findAny();
				if (optional.isPresent()) {
					deviceId = optional.get();
					isAndroid = true;
				} else {
					throw new RuntimeException("please connect android phones to the machine and re-run the tests");
				}
			} else if (Channel.NATIVE_IOS.toString().equals(mobileWebOS)) {
				Collection<String> keys = IOSUtilities.getConnectedDevices().keySet();
				Optional<String> optional = DEVICES.stream().filter(keys::contains).findAny();
				if (optional.isPresent()) {
					deviceId = optional.get();
					isAndroid = false;
				} else {
					throw new RuntimeException("please connect ios phones to the machine and re-run the tests");
				}
			} else {
				if (DEVICES.isEmpty()) {
					throw new RuntimeException(
							"please connect android or ios phones to the machine and re-run the tests");
				} else {
					deviceId = DEVICES.remove(0);
					isAndroid = ADBUtilities.getConnectedDevices().containsKey(deviceId);
				}
			}
		}

		if (deviceId == null) {
			throw new RuntimeException(String.format("please connect %s phones to the machine and re-run the tests",
					CHANNEL == Channel.NATIVE_ANDROID ? ANDROID
							: (CHANNEL == Channel.NATIVE_IOS ? IOS : "android or ios")));
		}
		CURRENT_RUNNING_DEVICE.set(deviceId);

		WebDriver driver = WebDriverFactory.create(CURRENT_RUNNING_DEVICE.get(), isAndroid,
				CHANNEL == Channel.MOBILE_WEB);
		if (driver == null) {
			throw new RuntimeException(
					String.format("error occurred while creating webdriver for %s device '%s'",
							getDeviceType(isAndroid), deviceId));
		}
		if (CHANNEL == Channel.MOBILE_WEB) {
			driver.get(getString("ui.base_url"));
		}
		DRIVER.set(driver);
	}

	private String getDeviceType() {
		return getDeviceType(CHANNEL == Channel.NATIVE_ANDROID);
	}

	private String getDeviceType(boolean isAndroid) {
		return CHANNEL == Channel.MOBILE_WEB ? "android / ios" : (isAndroid ? ANDROID : IOS);
	}

	protected final WebDriver getDriver() {
		return DRIVER.get();
	}

	protected final String getDeviceId() {
		return CURRENT_RUNNING_DEVICE.get();
	}

	@Override
	public void onTestStart(ITestResult result) {
		log.info("************************* starting test '{} # {}' *************************",
				result.getTestClass().getRealClass().getName(), result.getMethod().getMethodName());
	}

	@Override
	public void onTestFailure(ITestResult result) {
		log.error("************************* test '{} # {}' failed *************************",
				result.getTestClass().getRealClass().getName(), result.getMethod().getMethodName());
		captureScreenshot("failed", result);
		stopVideoRecording(getFile("failed", "mp4", VIDEOS_DIRECTORY, result), true);

	}

	@Override
	public void onTestSuccess(ITestResult result) {
		log.info("************************* test '{} # {}' succeeded *************************",
				result.getTestClass().getRealClass().getName(), result.getMethod().getMethodName());
		stopVideoRecording(getFile("success", "mp4", VIDEOS_DIRECTORY, result),
				!getBoolean("ui.record_video.only_on_failure"));
	}

	private File getFile(String prefix, String extension, String baseDirectory, ITestResult result) {
		String name = String.format("%s_%s_%s_%s.%s", prefix, result.getTestClass().getRealClass().getSimpleName(),
				result.getMethod().getMethodName(), DATE_FORMAT.format(new Date()), extension).toLowerCase();
		return new File(baseDirectory, name);
	}

	private void captureScreenshot(String prefix, ITestResult result) {

		if (DRIVER.get() == null) {
			return;
		}

		File file = getFile(prefix, "jpg", getString("ui.screenshots.dir"), result);
		if (file.getParentFile() != null && !file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		byte[] out = ((TakesScreenshot) DRIVER.get()).getScreenshotAs(OutputType.BYTES);
		try {
			FileUtils.writeByteArrayToFile(file, out);
			String type = CHANNEL == Channel.MOBILE_WEB ? "html" : "xml";
			File source = new File(file.getAbsolutePath().replace("jpg", type));
			FileUtils.write(source, DRIVER.get().getPageSource(), "utf-8");

			log.info("screenshot saved to '{}'", file.getAbsolutePath());
			log.info("page source saved to '{}'", source.getAbsolutePath());

			try (InputStream stream = new FileInputStream(file)) {
				Allure.addAttachment(file.getName(), "image/jpg", stream, ".jpg");
			}
			try (InputStream stream = new FileInputStream(source)) {
				Allure.addAttachment(source.getName(), "text/" + type, stream, "." + type);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		log.error("************************* test '{} # {}' skipped *************************",
				result.getTestClass().getRealClass().getName(), result.getMethod().getMethodName());
		captureScreenshot("skipped", result);
		stopVideoRecording(getFile("skipped", "mp4", VIDEOS_DIRECTORY, result), true);

	}

	@SuppressWarnings("rawtypes")
	public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
		if (annotation.getRetryAnalyzerClass() == null) {
			annotation.setRetryAnalyzer(RetryAnalyzer.class);
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
