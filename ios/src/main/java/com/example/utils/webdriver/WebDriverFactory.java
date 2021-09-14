package com.example.utils.webdriver;

import static com.example.utils.CommandLineExecutor.exec;
import static com.example.utils.CommonUtilities.isBlank;
import static com.example.utils.CommonUtilities.splitLines;
import static com.example.utils.ConfigManager.getBoolean;
import static com.example.utils.ConfigManager.getInt;
import static com.example.utils.ConfigManager.getLong;
import static com.example.utils.ConfigManager.getString;
import static io.appium.java_client.remote.AndroidMobileCapabilityType.APP_ACTIVITY;
import static io.appium.java_client.remote.AndroidMobileCapabilityType.APP_PACKAGE;
import static io.appium.java_client.remote.AndroidMobileCapabilityType.APP_WAIT_ACTIVITY;
import static io.appium.java_client.remote.AndroidMobileCapabilityType.APP_WAIT_PACKAGE;
import static io.appium.java_client.remote.AndroidMobileCapabilityType.AUTO_GRANT_PERMISSIONS;
import static io.appium.java_client.remote.AndroidMobileCapabilityType.DONT_STOP_APP_ON_RESET;
import static io.appium.java_client.remote.AndroidMobileCapabilityType.NATIVE_WEB_SCREENSHOT;
import static io.appium.java_client.remote.AndroidMobileCapabilityType.RESET_KEYBOARD;
import static io.appium.java_client.remote.AndroidMobileCapabilityType.SYSTEM_PORT;
import static io.appium.java_client.remote.AndroidMobileCapabilityType.UNICODE_KEYBOARD;
import static io.appium.java_client.remote.IOSMobileCapabilityType.AUTO_ACCEPT_ALERTS;
import static io.appium.java_client.remote.IOSMobileCapabilityType.AUTO_DISMISS_ALERTS;
import static io.appium.java_client.remote.IOSMobileCapabilityType.BUNDLE_ID;
import static io.appium.java_client.remote.IOSMobileCapabilityType.CONNECT_HARDWARE_KEYBOARD;
import static io.appium.java_client.remote.IOSMobileCapabilityType.NATIVE_WEB_TAP;
import static io.appium.java_client.remote.IOSMobileCapabilityType.SEND_KEY_STRATEGY;
import static io.appium.java_client.remote.IOSMobileCapabilityType.SHOW_IOS_LOG;
import static io.appium.java_client.remote.IOSMobileCapabilityType.SHOW_XCODE_LOG;
import static io.appium.java_client.remote.IOSMobileCapabilityType.USE_NEW_WDA;
import static io.appium.java_client.remote.IOSMobileCapabilityType.USE_PREBUILT_WDA;
import static io.appium.java_client.remote.IOSMobileCapabilityType.XCODE_ORG_ID;
import static io.appium.java_client.remote.MobileCapabilityType.APP;
import static io.appium.java_client.remote.MobileCapabilityType.AUTOMATION_NAME;
import static io.appium.java_client.remote.MobileCapabilityType.AUTO_WEBVIEW;
import static io.appium.java_client.remote.MobileCapabilityType.DEVICE_NAME;
import static io.appium.java_client.remote.MobileCapabilityType.FULL_RESET;
import static io.appium.java_client.remote.MobileCapabilityType.NEW_COMMAND_TIMEOUT;
import static io.appium.java_client.remote.MobileCapabilityType.NO_RESET;
import static io.appium.java_client.remote.MobileCapabilityType.PLATFORM_VERSION;
import static io.appium.java_client.remote.MobileCapabilityType.UDID;
import static org.openqa.selenium.remote.CapabilityType.BROWSER_NAME;
import static org.openqa.selenium.remote.CapabilityType.PLATFORM_NAME;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.example.utils.CommandLineResponse;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.IOSMobileCapabilityType;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class WebDriverFactory {

	private WebDriverFactory() {
	}

	public static WebDriver create(@NonNull String deviceId) {
		return create(deviceId, ADBUtilities.getConnectedDevices().containsKey(deviceId));
	}

	public static WebDriver create(@NonNull String deviceId, boolean isAndroid) {
		return create(deviceId, isAndroid, false);
	}

	public static WebDriver create(@NonNull String deviceId, boolean isAndroid, boolean isWeb) {

		DesiredCapabilities capabilities = MobileCapabilitiesFactory.getCapabilities(deviceId, isAndroid, isWeb);
		URL url = AppiumServiceFactory.startAppiumService(deviceId, isAndroid, capabilities);
		AppiumDriver<MobileElement> driver = isAndroid ? new AndroidDriver<>(url, capabilities)
				: new IOSDriver<>(url, capabilities);
		log.info("webdriver for {} device {} => {}", isAndroid ? "android" : "ios", deviceId, driver);
		return driver;
	}
}

@Slf4j
class MobileCapabilitiesFactory {

	private static final String PRINT_PAGE_SOURCE_ON_FIND_FAILURE_CAPABILITY = "printPageSourceOnFindFailure";
	private static final String ENABLE_PERFORMANCE_LOGGING_CAPABILITY = "enablePerformanceLogging";
	private static final String DEFAULT_XCODE_VERSION = "9.3";

	private static final AtomicInteger IOS_WDA_PORT = new AtomicInteger(getInt("appium.ios.port.wda_agent"));
	private static final AtomicInteger ANDROID_SYSTEM_PORT = new AtomicInteger(
			getInt("appium.android.port.system_port"));
	private static final String XCODE_VERSION = getLocalIOSSDKVersion();

	private MobileCapabilitiesFactory() {
	}

	public static DesiredCapabilities getCapabilities(@NonNull String deviceId, boolean isAndroid, boolean isWeb) {
		return isAndroid ? getCapabilitiesForAndroid(deviceId, isWeb) : getCapabilitiesForIOs(deviceId, isWeb);
	}

	private static String getIOSAppPath(boolean isRealDevice) {

		String appPath = getString("appium.ios.app_path");
		if (isBlank(appPath)) {
			return null;
		}
		if (!appPath.startsWith("http")) {
			File file = new File(appPath.trim());
			if (file.getName().endsWith(".zip") || (isRealDevice && file.getName().endsWith(".ipa"))
					|| (!isRealDevice && file.getName().endsWith(".app"))) {
				return file.getAbsolutePath();
			}
			if (!isRealDevice) {
				File app = new File(file.getParent(),
						file.getName().substring(0, file.getName().lastIndexOf(".")) + ".app");
				if (!app.exists()) {
					log.error("file '{}' not found", app.getAbsolutePath());
				} else {
					return app.getAbsolutePath();
				}
			} else {
				File app = new File(file.getParent(),
						file.getName().substring(0, file.getName().lastIndexOf(".")) + ".ipa");
				if (!app.exists()) {
					log.error("file '{}' not found", app.getAbsolutePath());
				} else {
					return app.getAbsolutePath();
				}
			}
		} else {
			return appPath;
		}
		return null;
	}

	private static DesiredCapabilities getCapabilitiesForIOs(@NonNull final String deviceId, boolean isWeb) {

		DesiredCapabilities capabilities = new DesiredCapabilities();
		boolean isRealDevice = IOSUtilities.getConnectedRealDevices().containsKey(deviceId);

		capabilities.setCapability(UDID, deviceId);
		capabilities.setCapability(DEVICE_NAME, IOSUtilities.getConnectedDevices().get(deviceId));
		capabilities.setCapability(PLATFORM_NAME, org.openqa.selenium.Platform.IOS);
		if (isWeb) {
			capabilities.setCapability(BROWSER_NAME, BrowserType.SAFARI);
		} else {
			boolean isReInstallApp = getBoolean("appium.reinstall_app");
			capabilities.setCapability(FULL_RESET, isReInstallApp);
			capabilities.setCapability(NO_RESET, !isReInstallApp);

			String appPath = getIOSAppPath(isRealDevice);

			if (isReInstallApp && isBlank(appPath)) {
				throw new RuntimeException(
						"'appium.ios.app_path' is required when 'appium.reinstall_app' is set to true");
			}
			if (isBlank(appPath)) {
				String bundleId = getString("appium.ios.bundle_id");
				if (!isBlank(bundleId)) {
					capabilities.setCapability(BUNDLE_ID, bundleId);
				} else {
					throw new RuntimeException(
							"'appium.ios.bundle_id' is required when 'appium.ios.app_path' is not set");
				}
			}
			capabilities.setCapability(APP, appPath);
			capabilities.setCapability(AUTO_WEBVIEW, getBoolean("appium.auto_webview"));
		}
		capabilities.setCapability(NEW_COMMAND_TIMEOUT, getLong("appium.new_command.timeout"));
		capabilities.setCapability(AUTOMATION_NAME, getString("appium.ios.automation_name"));
		capabilities.setCapability(CapabilityType.LOGGING_PREFS, getLogPreferences());
		capabilities.setCapability(NATIVE_WEB_TAP, true);
		capabilities.setCapability(AUTO_ACCEPT_ALERTS, getBoolean("appium.ios.auto_accept_alerts"));
		capabilities.setCapability(AUTO_DISMISS_ALERTS, getBoolean("appium.ios.auto_dismiss_alerts"));
		capabilities.setCapability(SEND_KEY_STRATEGY, getString("appium.ios.send_keys_strategy"));
		capabilities.setCapability(USE_NEW_WDA, getBoolean("appium.ios.use_new_wda"));
		capabilities.setCapability(USE_PREBUILT_WDA, getBoolean("appium.ios.use_prebuilt_wda"));
		capabilities.setCapability(SHOW_IOS_LOG, getBoolean("appium.ios.logs"));
		capabilities.setCapability(SHOW_XCODE_LOG, getBoolean("appium.ios.xcode_logs"));
		capabilities.setCapability(CONNECT_HARDWARE_KEYBOARD, getBoolean("appium.ios.connect_hardware_keyboard"));

		capabilities.setCapability(PRINT_PAGE_SOURCE_ON_FIND_FAILURE_CAPABILITY, false);
		capabilities.setCapability(PLATFORM_VERSION, XCODE_VERSION);
		capabilities.setCapability("useJSONSource", true);
		capabilities.setCapability(IOSMobileCapabilityType.WDA_LOCAL_PORT, IOS_WDA_PORT.getAndIncrement());

		if (isRealDevice) {
			String xcodeOrgId = getString("appium.ios.xcode_org_id");
			if (!isBlank(xcodeOrgId)) {
				capabilities.setCapability(XCODE_ORG_ID, xcodeOrgId);
			}
			String xcodeSigningId = getString("appium.ios.xcode_signing_id");
			if (!isBlank(xcodeSigningId)) {
				capabilities.setCapability(IOSMobileCapabilityType.XCODE_SIGNING_ID, xcodeOrgId);
			}
		}
		capabilities.setCapability(ENABLE_PERFORMANCE_LOGGING_CAPABILITY, isRealDevice);
		capabilities.setCapability(IOSMobileCapabilityType.SAFARI_INITIAL_URL, "https://www.google.com");
		return capabilities;

	}

	private static DesiredCapabilities getCapabilitiesForAndroid(@NonNull final String deviceId, boolean isWeb) {

		DesiredCapabilities capabilities = new DesiredCapabilities();
		if (isWeb) {
			capabilities.setCapability(BROWSER_NAME, BrowserType.CHROME);
		} else {
			boolean isReInstallApp = getBoolean("appium.reinstall_app");
			capabilities.setCapability(FULL_RESET, isReInstallApp);
			capabilities.setCapability(NO_RESET, !isReInstallApp);

			String appPath = getString("appium.android.app_path");
			if (!isBlank(appPath)) {
				if (!appPath.startsWith("http")) {
					File appFile = new File(appPath.trim());
					if (!appFile.exists()) {
						throw new RuntimeException(String.format(
								"unable to find '%s' file to install on device '[%s = %s]'",
								appFile.getAbsolutePath(), deviceId, ADBUtilities.getConnectedDevices().get(deviceId)));
					}
					capabilities.setCapability(APP, appFile.getAbsolutePath());
				} else {
					capabilities.setCapability(APP, appPath.trim());
				}
			}
			if (isReInstallApp && isBlank(appPath)) {
				throw new RuntimeException(
						"'appium.android.app_path' is required when 'appium.reinstall_app' is set to true");
			}

			String appPackage = getString("appium.android.app_package");
			if (appPackage != null) {
				capabilities.setCapability(APP_PACKAGE, appPackage);
				if (getBoolean("appium.android.wipe_user_data")) {
					ADBUtilities.clearUserData(deviceId, appPackage);
				}
			}
			String appWaitPackage = getString("appium.android.app_wait_package");
			if (appWaitPackage != null) {
				capabilities.setCapability(APP_WAIT_PACKAGE, appWaitPackage);
			} else if (appPackage != null) {
				capabilities.setCapability(APP_WAIT_PACKAGE, appPackage);
			}
			String appActivity = getString("appium.android.app_activity");
			if (appActivity != null) {
				capabilities.setCapability(APP_ACTIVITY, appActivity);
				capabilities.setCapability(APP_WAIT_ACTIVITY, appActivity);
			}
			String appWaitActivity = getString("appium.android.app_wait_activity");
			if (appWaitActivity != null) {
				capabilities.setCapability(APP_WAIT_ACTIVITY, appWaitActivity);
			} else if (appActivity != null) {
				capabilities.setCapability(APP_WAIT_ACTIVITY, appActivity);
			}
			capabilities.setCapability(AUTO_WEBVIEW, getBoolean("appium.auto_webview"));
		}
		capabilities.setCapability(DEVICE_NAME, ADBUtilities.getConnectedDevices().get(deviceId));
		capabilities.setCapability(UNICODE_KEYBOARD, getBoolean("appium.android.use_appium_keyboard"));
		capabilities.setCapability(RESET_KEYBOARD, true);
		capabilities.setCapability("autoLaunch", getBoolean("appium.autolaunch"));
		capabilities.setCapability(AUTO_GRANT_PERMISSIONS, getBoolean("appium.android.grant_all_permissions"));
		capabilities.setCapability(UDID, deviceId);
		capabilities.setCapability(PLATFORM_NAME, org.openqa.selenium.Platform.ANDROID);
		capabilities.setCapability(NEW_COMMAND_TIMEOUT, getLong("appium.new_command.timeout"));
		capabilities.setCapability(DEVICE_NAME, ADBUtilities.getConnectedDevices().get(deviceId));
		capabilities.setCapability(NATIVE_WEB_SCREENSHOT, true);
		capabilities.setCapability(AUTOMATION_NAME, getString("appium.android.automation_name"));
		capabilities.setCapability(DONT_STOP_APP_ON_RESET, true);
		capabilities.setCapability(CapabilityType.LOGGING_PREFS, getLogPreferences());
		capabilities.setCapability("clearDeviceLogsOnStart", true);
		capabilities.setCapability(SYSTEM_PORT, ANDROID_SYSTEM_PORT.getAndIncrement());
		capabilities.setCapability(AndroidMobileCapabilityType.SKIP_DEVICE_INITIALIZATION,
				getBoolean("appium.android.skip_device_init"));
		capabilities.setCapability("skipServerInstallation", getBoolean("appium.android.skip_server_init"));
		capabilities.setCapability(ENABLE_PERFORMANCE_LOGGING_CAPABILITY, true);
		capabilities.setCapability(PRINT_PAGE_SOURCE_ON_FIND_FAILURE_CAPABILITY, false);
		capabilities.setCapability("ignoreHiddenApiPolicyError", true);
		return capabilities;
	}

	private static LoggingPreferences getLogPreferences() {
		LoggingPreferences logPrefs = new LoggingPreferences();
		logPrefs.enable(LogType.BROWSER, Level.ALL);
		logPrefs.enable(LogType.CLIENT, Level.ALL);
		logPrefs.enable(LogType.DRIVER, Level.ALL);
		logPrefs.enable(LogType.PERFORMANCE, Level.ALL);
		logPrefs.enable(LogType.PROFILER, Level.ALL);
		logPrefs.enable(LogType.SERVER, Level.ALL);
		logPrefs.enable("logcat", Level.ALL);
		logPrefs.enable("bugreport", Level.ALL);
		return logPrefs;
	}

	private static String getLocalIOSSDKVersion() {
		CommandLineResponse response = exec("xcodebuild -showsdks | grep sdk | awk '{print $NF}' | head -1");
		if (response != null && response.getExitCode() == 0) {
			List<String> split = splitLines(response.getStdOut());
			return split == null || split.isEmpty() ? DEFAULT_XCODE_VERSION
					: split.get(0).trim().replaceAll("[^0-9\\.]", "");
		}
		return null;
	}
}