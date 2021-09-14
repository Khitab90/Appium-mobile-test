package com.example.utils.webdriver;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomStringUtils;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.example.Platform;
import com.example.utils.CommonUtilities;
import com.example.utils.ConfigManager;
import com.example.utils.ProcessUtils;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.AndroidServerFlag;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.appium.java_client.service.local.flags.IOSServerFlag;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AppiumServiceFactory {

	private static AtomicInteger appiumPort;
	private static AtomicInteger bootstrapPort;
	private static AtomicInteger chromeDriverPort;
	private static AtomicInteger webkitProxyPort;

	private static File appiumExecPath;
	private static File nodeExecPath;

	private static final Map<String, AppiumDriverLocalService> LOCAL_APPIUM_SERVICE = new ConcurrentHashMap<>();

	private static final Map<String, String> ENV = new HashMap<>();
	private static final File APPIUM_LOG_DIR = Paths.get("logs", "appium").toFile();

	private AppiumServiceFactory() {
	}

	static {
		setPorts();
		setEnvironment();
		setExecutables();
	}

	private static void setEnvironment() {
		ENV.put("JAVA_HOME", System.getProperty("java.home").replace("jre", ""));
		File androidHome = ADBUtilities.getAndroidHome();
		if (!androidHome.exists()) {
			throw new RuntimeException(
					"neither 'ANDROID_HOME' or 'ANDROID_SDK_HOME' is found nor 'android.sdk.home' argument is found");
		}
		ENV.put("ANDROID_HOME", androidHome.getAbsolutePath());
		ENV.put("PATH",
				Platform.CURRENT_PLATFORM != Platform.WINDOWS
						? System.getenv("PATH") + ":/usr/local/bin:" + System.getenv("HOME") + "/.linuxbrew/bin"
						: System.getenv("Path") == null ? System.getenv("PATH") : System.getenv("Path"));
	}

	private static void setExecutables() {
		appiumExecPath = CommonUtilities.findLocalExecutable("appium");
		nodeExecPath = CommonUtilities.findLocalExecutable("node");
	}

	private static void setPorts() {
		appiumPort = new AtomicInteger(ConfigManager.getInt("appium.port"));
		bootstrapPort = new AtomicInteger(ConfigManager.getInt("appium.port.bootstrap"));
		chromeDriverPort = new AtomicInteger(ConfigManager.getInt("appium.port.chrome_driver"));
		webkitProxyPort = new AtomicInteger(ConfigManager.getInt("appium.port.webkit_proxy"));
	}

	/**
	 * Start Appium service for the given deviceId
	 *
	 * @param deviceId  {@link String}
	 * @param isAndroid {@link Boolean}
	 * @return {@link URL} - service url
	 */
	public static URL startAppiumService(@NonNull final String deviceId, final boolean isAndroid,
			@NonNull DesiredCapabilities capabilities) {

		File logFile = new File(APPIUM_LOG_DIR.getPath(),
				deviceId + "_" + RandomStringUtils.randomAlphabetic(10) + ".log");

		int _appiumPort = appiumPort.getAndIncrement();
		int _bootstrapPort = bootstrapPort.getAndIncrement();
		int _chromeDriverPort = chromeDriverPort.getAndIncrement();

		ProcessUtils.killProcessListeningAtPort(_appiumPort);
		ProcessUtils.killProcessListeningAtPort(_bootstrapPort);

		AppiumServiceBuilder builder = new AppiumServiceBuilder().usingPort(_appiumPort).withEnvironment(ENV)
				.withAppiumJS(appiumExecPath).usingDriverExecutable(nodeExecPath)
				.withArgument(GeneralServerFlag.LOG_LEVEL, "error:debug").withArgument(GeneralServerFlag.ASYNC_TRACE)
				.withArgument(GeneralServerFlag.DEBUG_LOG_SPACING).withArgument(GeneralServerFlag.RELAXED_SECURITY)
				.withArgument(GeneralServerFlag.SESSION_OVERRIDE).withLogFile(logFile);

		if (isAndroid) {
			ProcessUtils.killProcessListeningAtPort(_chromeDriverPort);
			Integer _chromeDriverVersion = getAppVersion(deviceId, "com.android.chrome");
			if (_chromeDriverVersion != null) {
				builder.withArgument(AndroidServerFlag.CHROME_DRIVER_EXECUTABLE,
						ChromeDriverExecutableUtils.getChromeDriverExecutable(_chromeDriverVersion.toString())
								.getAbsolutePath());
			}
			builder.withArgument(AndroidServerFlag.BOOTSTRAP_PORT_NUMBER, String.valueOf(_bootstrapPort))
					.withArgument(AndroidServerFlag.CHROME_DRIVER_PORT, String.valueOf(_chromeDriverPort));
		} else {
			if (IOSUtilities.getConnectedRealDevices().containsKey(deviceId)) {
				int _proxyPort = webkitProxyPort.getAndIncrement();
				ProcessUtils.killProcessListeningAtPort(_proxyPort);
				builder.withArgument(IOSServerFlag.WEBKIT_DEBUG_PROXY_PORT, String.valueOf(_proxyPort))
						.withArgument(() -> "--webdriveragent-port",
								capabilities.getCapability("wdaLocalPort").toString());
			}
		}
		AppiumDriverLocalService service = builder.build();
		LOCAL_APPIUM_SERVICE.put(deviceId, service);

		if (service != null && !service.isRunning()) {
			try {
				service.start();
				log.info("appium session started for device '{}' at {} and writing logs to '{}'", deviceId,
						service.getUrl(), logFile);
				return service.getUrl();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static void stopLocalAppiumService(@NonNull String deviceId) {
		if (LOCAL_APPIUM_SERVICE.containsKey(deviceId) && LOCAL_APPIUM_SERVICE.get(deviceId).isRunning()) {
			log.info("device '{}' ::: stopping appium service running on {}", deviceId,
					LOCAL_APPIUM_SERVICE.get(deviceId).getUrl());
			LOCAL_APPIUM_SERVICE.get(deviceId).stop();
			LOCAL_APPIUM_SERVICE.remove(deviceId);
		}
	}

	private static Integer getAppVersion(final String deviceId, @NonNull final String appPackage) {
		String version = ADBUtilities.getAppVersion(deviceId, appPackage);
		if (CommonUtilities.isBlank(version)) {
			log.error("'{}' app version on {} is not found", appPackage, deviceId);
			return null;
		}
		int majorVersion = getMajorVersion(version);
		log.debug("device '{}' ::: '{}' app => actual version '{}' and major version '{}'", deviceId, appPackage,
				version, majorVersion);
		return majorVersion;
	}

	private static Integer getMajorVersion(@NonNull String version) {
		String _version = version.trim();
		String majorVersion = _version.substring(0, _version.indexOf('.'));
		return Integer.parseInt(majorVersion);
	}

}
