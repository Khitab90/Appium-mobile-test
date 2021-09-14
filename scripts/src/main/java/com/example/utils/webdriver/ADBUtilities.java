package com.example.utils.webdriver;

import static com.example.utils.CommandLineExecutor.exec;
import static com.example.utils.CommonUtilities.findLocalExecutable;
import static com.example.utils.CommonUtilities.getMatches;
import static com.example.utils.CommonUtilities.isBlank;
import static com.example.utils.CommonUtilities.splitLines;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.ArrayUtils;

import com.example.utils.CommandLineResponse;
import com.example.utils.ConfigManager;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ADBUtilities {

	private static final Map<String, String> CONNECTED_ANDROID_DEVICES = new ConcurrentHashMap<>();
	private static String ADB_EXECUTABLE_PATH = null;

	private ADBUtilities() {
	}

	/**
	 * Get deviceModel for the given deviceID using ADB commands
	 *
	 * @return {@link String}
	 */
	public static String getDeviceModel() {
		return getDeviceModel(null);
	}

	/**
	 * Get deviceModel for the given deviceID using ADB commands
	 *
	 * @param deviceId {@link String}
	 * @return {@link String}
	 */
	public static String getDeviceModel(final String deviceId) {

		if (isBlank(deviceId)) {
			return null;
		}
		String model = getDeviceProperty(deviceId, "ro.product.model");
		log.info("device model of deviceId '{}' : {}", isBlank(deviceId) ? "" : deviceId.trim(), model);
		return model;
	}

	/**
	 * Get device property using ADB command.
	 *
	 * @param property {@link String}
	 * @return {@link String}
	 */
	public static String getDeviceProperty(@NonNull final String property) {
		return getDeviceProperty(null, property);
	}

	/**
	 * Get device property using ADB command.
	 *
	 * @param deviceId {@link String}
	 * @param property {@link String}
	 * @return {@link String}
	 */
	public static String getDeviceProperty(final String deviceId, @NonNull final String property) {

		String[] args = getArguments(deviceId, String.format("shell getprop %s", property.trim()));
		CommandLineResponse response = exec(String.join(" ", args));
		if (response == null || isBlank(response.getStdOut())) {
			return null;
		}
		String resp = splitLines(response.getStdOut()).get(0);
		return isBlank(resp) ? null : resp.trim();
	}

	/**
	 * Clear user data for the app identified by the given package
	 *
	 * @param appPackage
	 * @return {@link Boolean}
	 */
	public static boolean clearUserData(@NonNull final String appPackage) {
		return clearUserData(null, appPackage);
	}

	/**
	 * Clear user data for the app identified by the given package for the given
	 * device
	 *
	 * @param deviceId   {@link String}
	 * @param appPackage {@link String}
	 * @return {@link Boolean}
	 */
	public static boolean clearUserData(final String deviceId, @NonNull final String appPackage) {
		String[] args = getArguments(deviceId, String.format("shell pm clear %s", appPackage));
		CommandLineResponse response = exec(String.join(" ", args));
		if (response != null && response.getExitCode() == 0) {
			return response.getStdOut().toLowerCase().contains("success");
		}
		return false;
	}

	/**
	 * Check if the given app identified by the given package is installed on the
	 * android devices
	 *
	 * @param appPackage
	 * @return {@link Boolean}
	 */
	public static boolean isAppInstalled(@NonNull final String appPackage) {
		return clearUserData(null, appPackage);
	}

	/**
	 * Check if the given app identified by the given package is installed on the
	 * android devices identified by the given id
	 *
	 * @param deviceId   {@link String}
	 * @param appPackage {@link String}
	 * @return {@link Boolean}
	 */
	public static boolean isAppInstalled(final String deviceId, @NonNull final String appPackage) {
		String[] args = getArguments(deviceId, String.format("shell pm path '%s' | wc -l", appPackage));
		CommandLineResponse response = exec(String.join(" ", args));
		if (response != null && response.getExitCode() == 0) {
			return 1 == Integer.parseInt(response.getStdOut());
		}
		return false;
	}

	/**
	 * Get the installed app version identified by the given app package name
	 *
	 * @param appPackage
	 * @return {@link String}
	 */
	public static String getAppVersion(@NonNull final String appPackage) {
		return getAppVersion(null, appPackage);
	}

	/**
	 * Get the installed app version identified by the given app package name
	 *
	 * @param deviceId   {@link String}
	 * @param appPackage {@link String}
	 * @return {@link String}
	 */
	public static String getAppVersion(final String deviceId, @NonNull final String appPackage) {
		String[] args = getArguments(deviceId,
				String.format("shell dumpsys package %s | grep -iE '(versionName=)([0-9\\.]+)'", appPackage));
		CommandLineResponse response = exec(String.join(" ", args));
		if (response != null && response.getExitCode() == 0) {
			String[] split = response.getStdOut().split("=");
			return split != null && split.length >= 2 ? split[1].trim().replaceAll("[^\\d\\.]+", "") : null;
		}
		return null;
	}

	public static void killApp(@NonNull final String appPackage) {
		killApp(null, appPackage);
	}

	public static void killApp(final String deviceId, @NonNull final String appPackage) {
		exec(String.join(" ", getArguments(deviceId, String.format("am force-stop %s", appPackage))));
	}

	/**
	 * Get the arguments for adb shell
	 *
	 * @param deviceId {@link String}
	 * @param args     {@link String}[]
	 * @return {@link String}[]
	 */
	private static String[] getArguments(final String deviceId, final String... args) {

		if (args == null || args.length == 0) {
			return null;
		}
		return isBlank(deviceId)
				? ArrayUtils.addAll(new String[] { getADBExecutable() }, args)
				: ArrayUtils.addAll(new String[] { getADBExecutable(), "-s", deviceId.trim() }, args);
	}

	public static File getAndroidHome() {
		String value = ConfigManager.getString("ANDROID_HOME");
		if (isBlank(value)) {
			value = ConfigManager.getString("ANDROID_SDK_HOME");
		}
		if (isBlank(value)) {
			value = ConfigManager.getString("android.home");
		}
		if (!isBlank(value)) {
			File file = new File(value.trim());
			if (file.exists() && file.isDirectory()) {
				return file;
			}
		}
		return _getADBExecutable().getParentFile().getParentFile();
	}

	private static File _getADBExecutable() {
		File file = findLocalExecutable("adb");
		return file != null && file.exists() ? file : null;
	}

	public static String getADBExecutable() {
		if (ADB_EXECUTABLE_PATH == null) {
			try {
				File adb = _getADBExecutable();
				if (adb != null) {
					return ADB_EXECUTABLE_PATH = adb.getAbsolutePath();
				}
				File androidHome = getAndroidHome();
				if (androidHome != null) {
					File file = Paths.get(androidHome.getAbsolutePath(), "platform-tools", "adb").toFile();
					if (file != null && file.exists()) {
						return ADB_EXECUTABLE_PATH = file.getAbsolutePath();
					}
				}
			} finally {
				log.info("android debug bridge (adb) file located at => {}", ADB_EXECUTABLE_PATH);
			}
		}
		return ADB_EXECUTABLE_PATH;
	}

	/**
	 * Get all the connected android devices.
	 *
	 * @return {@link Map}&lt;{@link String}, {@link String}&gt;
	 */
	public static Map<String, String> getConnectedDevices() {

		if (!CONNECTED_ANDROID_DEVICES.isEmpty()) {
			return CONNECTED_ANDROID_DEVICES;
		}
		CommandLineResponse response = exec(String.join(" ", getArguments(null, "devices -l")));
		if (response == null || isBlank(response.getStdOut())) {
			return CONNECTED_ANDROID_DEVICES;
		}
		List<String> devices = splitLines(response.getStdOut());
		if (devices != null && devices.size() > 1) {
			devices.subList(1, devices.size()).forEach(str -> {
				String[] split = str.split("\\s{2,}");
				if (split != null && split.length == 2) {
					List<String> tmp = getMatches(split[1], "\\b(?<=model:).*?(?=\\s+)\\b");
					if (tmp != null && !tmp.isEmpty()) {
						CONNECTED_ANDROID_DEVICES.put(split[0].trim(),
								tmp.get(0).trim().replace("__", "").replace("_", " ").trim());
					}
				}
			});
			if (CONNECTED_ANDROID_DEVICES.isEmpty()) {
				log.error("no connected android devices");
			} else {
				log.debug("connected android devices with model: {}", CONNECTED_ANDROID_DEVICES);
			}
		}
		return CONNECTED_ANDROID_DEVICES;
	}

	/**
	 * Uninstall android app from device
	 *
	 * @param appPackage {@link String}
	 * @return {@link Boolean}
	 */
	public static boolean uninstallApp(final String appPackage) {
		return uninstallApp(null, appPackage);
	}

	/**
	 * Uninstall android app from device
	 *
	 * @param appPackage {@link String}
	 * @param deviceId   {@link String}
	 * @return {@link Boolean}
	 */
	public static boolean uninstallApp(final String deviceId, final String appPackage) {

		if (isBlank(appPackage)) {
			log.error("appPackage should not be empty or null!");
			return false;
		}
		CommandLineResponse response = exec(String.join(" ", getArguments(deviceId, "uninstall " + appPackage.trim())));
		boolean result = response != null && response.getStdOut().toLowerCase().contains("success");
		log.info("uninstalling app package '{}'{} successful ? {}", appPackage.trim(),
				isBlank(deviceId) ? "" : " from device '" + deviceId.trim() + "'", result);
		return result;
	}

	/**
	 * Install android app on device
	 *
	 * @param appPath {@link String}
	 * @return {@link Boolean}
	 */
	public static boolean installApp(final String appPath) {
		return installApp(null, appPath);
	}

	/**
	 * Install android app on device
	 *
	 * @param appPath  {@link String}
	 * @param deviceId {@link String}
	 * @return {@link Boolean}
	 */
	public static boolean installApp(final String deviceId, final String appPath) {

		if (isBlank(appPath)) {
			log.error("appPath should not be empty or null!");
			return false;
		}
		File file = new File(appPath);
		if (!file.exists()) {
			log.error(String.format("app file '%s' does not exist", file.getAbsolutePath()));
			return false;
		}
		CommandLineResponse response = exec(String.join(" ", getArguments(deviceId, "install", appPath.trim())));
		boolean result = response != null && response.getStdOut().toLowerCase().contains("success");
		log.info(String.format("installing app '%s'%s successful ? %s", appPath.trim(),
				isBlank(deviceId) ? "" : " on device '" + deviceId.trim() + "'", result));
		return result;
	}

	/**
	 * Put app to background
	 */
	public static void putAppToBackground() {
		putAppToBackground(null);
	}

	/**
	 * Put app to background
	 *
	 * @param deviceId {@link String}
	 */
	public static void putAppToBackground(String deviceId) {
		exec(String.join(" ", getArguments(deviceId, "shell input keyevent 3")));
	}

	/**
	 * Resume app from background
	 *
	 * @param appPackage {@link String}
	 */
	public static void resumeApp(String appPackage) {
		resumeApp(null, appPackage);
	}

	/**
	 * Resume app from background
	 *
	 * @param deviceId   {@link String}
	 * @param appPackage {@link String}
	 */
	public static void resumeApp(String deviceId, String appPackage) {
		exec(String.join(" ", getArguments(deviceId,
				String.format("shell monkey -p %s -c android.intent.category.LAUNCHER 1", appPackage))));
	}

	/**
	 * Hide Keyboard
	 */
	public static void hideKeyboard() {
		hideKeyboard(null);
	}

	/**
	 * Hide Keyboard
	 *
	 * @param deviceId {@link String}
	 */
	public static void hideKeyboard(String deviceId) {
		exec(String.join(" ", getArguments(deviceId, "shell input keyevent 111")));
	}
}
