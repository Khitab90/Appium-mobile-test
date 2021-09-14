package com.example.utils.webdriver;

import static com.example.utils.CommandLineExecutor.exec;
import static com.example.utils.CommonUtilities.getMatches;
import static com.example.utils.CommonUtilities.splitLines;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.example.Platform;
import com.example.utils.CommandLineResponse;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class IOSUtilities {

	private static final Map<String, String> EMULATED_DEVICES = new ConcurrentHashMap<>();
	private static final Map<String, String> REAL_DEVICES = new ConcurrentHashMap<>();
	private static final Map<String, String> ALL_DEVICES = new ConcurrentHashMap<>();

	private static final boolean IS_MAC = Platform.CURRENT_PLATFORM == Platform.MACINTOSH;

	private IOSUtilities() {
	}

	/**
	 * Get all connected real devices and emulated devices
	 *
	 * @return {@link Map}&lt;&lt;{@link String}, {@link String}&gt;&gt;
	 */
	public static Map<String, String> getConnectedDevices() {

		if (!IS_MAC) {
			return Collections.emptyMap();
		}

		if (ALL_DEVICES.isEmpty()) {
			synchronized (IOSUtilities.class) {
				if (ALL_DEVICES.isEmpty()) {
					ALL_DEVICES.putAll(getConnectedSimulators());
					ALL_DEVICES.putAll(getConnectedRealDevices());
				}
			}
		}
		return ALL_DEVICES;
	}

	/**
	 * Get all booted emulated iOS devices
	 *
	 * @return {@link Map}&lt;&lt;{@link String}, {@link String}&gt;&gt;
	 */
	public static Map<String, String> getConnectedSimulators() {

		if (!IS_MAC) {
			return Collections.emptyMap();
		}

		if (!EMULATED_DEVICES.isEmpty()) {
			return EMULATED_DEVICES;
		}
		if (EMULATED_DEVICES.isEmpty()) {
			String cmd = "xcrun simctl list devices | grep -i booted";
			CommandLineResponse response = exec(cmd);
			if (response.getExitCode() == 0) {
				List<String> lines = splitLines(response.getStdOut());
				if (lines != null && !lines.isEmpty()) {
					lines.forEach(str -> {
						List<String> txt = getMatches(str.trim(), "(?<=\\().*?(?=\\))");
						String id = txt.size() >= 2 ? txt.get(txt.size() - 2).trim() : "";
						EMULATED_DEVICES.put(id, str.substring(0, str.indexOf('(')).trim());
					});
				}
			}
			if (EMULATED_DEVICES.isEmpty()) {
				log.debug("no connected emulated ios devices");
			} else {
				log.info("connected emulated ios devices => {}", EMULATED_DEVICES);
			}
		}
		return EMULATED_DEVICES;
	}

	/**
	 * Installing ios app onto real devices and simulators
	 *
	 * @param appPath {@link String}
	 */
	public static void installApp(@NonNull String appPath) {
		installApp(null, appPath);
	}

	/**
	 * Installing ios app onto real devices and simulators
	 *
	 * @param deviceId {@link String}
	 * @param appPath  {@link String}
	 */
	public static void installApp(String deviceId, @NonNull String appPath) {
		if (!IS_MAC) {
			return;
		}
		if (!getConnectedSimulators().isEmpty()) {
			Collection<String> devices = deviceId == null ? getConnectedSimulators().keySet()
					: (getConnectedSimulators().containsKey(deviceId) ? Arrays.asList(deviceId)
							: Collections.emptyList());
			devices.forEach(key -> {
				String cmd = String.format("xcrun simctl install %s '%s'", key, appPath);
				CommandLineResponse response = exec(cmd);
				if (response != null && response.getExitCode() == 0) {
					log.info("installing app '{}' from simulator with id '{}' successful", appPath, key);
				} else {
					log.debug("error occurred while installing app '{}' from simulator with id '{}' => {}", appPath,
							key, response);
				}
			});
		}
		if (!getConnectedRealDevices().isEmpty()) {
			Collection<String> devices = deviceId == null ? getConnectedRealDevices().keySet()
					: (getConnectedRealDevices().containsKey(deviceId) ? Arrays.asList(deviceId)
							: Collections.emptyList());
			devices.forEach(key -> {
				String cmd = String.format("ideviceinstaller -u %s -i '%s'", key, appPath);
				CommandLineResponse response = exec(cmd);
				if (response != null && response.getExitCode() == 0) {
					log.info("installing app '{}' from device with id '{}' successful", appPath, key);
				} else {
					log.error("error occurred while installing app '{}' from device with id '{}' => {}", appPath, key,
							response);
				}
			});
		}
	}

	/**
	 * Uninstalling ios app from real devices and simulators
	 *
	 * @param appPath {@link String}
	 */
	public static void uninstallApp(@NonNull String appPath) {
		uninstallApp(null, appPath);
	}

	/**
	 * Installing ios app from real devices and simulators
	 *
	 * @param deviceId {@link String}
	 * @param bundleId {@link String}
	 */
	public static void uninstallApp(String deviceId, @NonNull String bundleId) {

		if (!IS_MAC) {
			return;
		}

		if (!getConnectedSimulators().isEmpty()) {
			Collection<String> devices = deviceId == null ? getConnectedSimulators().keySet()
					: (getConnectedSimulators().containsKey(deviceId) ? Arrays.asList(deviceId)
							: Collections.emptyList());
			devices.forEach(key -> {
				String cmd = String.format("xcrun simctl uninstall %s '%s'", key, bundleId);
				CommandLineResponse response = exec(cmd);
				if (response != null && response.getExitCode() == 0) {
					log.info("uninstalling app '{}' from simulator with id '{}' successful", bundleId, key);
				} else {
					log.error("error occurred while uninstalling app '{}' from simulator with id '{}' => {}", bundleId,
							key, response);
				}
			});
		}
		if (!getConnectedRealDevices().isEmpty()) {
			Collection<String> devices = deviceId == null ? getConnectedRealDevices().keySet()
					: (getConnectedRealDevices().containsKey(deviceId) ? Arrays.asList(deviceId)
							: Collections.emptyList());

			devices.forEach((key) -> {
				String cmd = String.format("ideviceinstaller -u %s -U '%s'", key, bundleId);
				CommandLineResponse response = exec(cmd);
				if (response != null && response.getExitCode() == 0) {
					log.info("uninstalling app '{}' from device with id '{}' successful", bundleId, key);
				} else {
					log.debug("error occurred while uninstalling app '{}' from device with id '{}' => {}", bundleId,
							key, response);
				}
			});
		}
	}

	/**
	 * Get all connected real iOS devices
	 *
	 * @return {@link Map}&lt;&lt;{@link String}, {@link String}&gt;&gt;
	 */
	public static Map<String, String> getConnectedRealDevices() {
		if (!IS_MAC) {
			return Collections.emptyMap();
		}
		if (!REAL_DEVICES.isEmpty()) {
			return REAL_DEVICES;
		}
		if (REAL_DEVICES.isEmpty()) {
			String cmd = "idevice_id -l";
			CommandLineResponse response = exec(cmd);
			if (response.getExitCode() == 0 && !response.getStdOut().trim().isEmpty()) {
				List<String> lines = splitLines(response.getStdOut());
				if (lines != null && !lines.isEmpty()) {
					for (String str : lines) {
						REAL_DEVICES.put(str.trim(), "Not Known");
					}
				}
			}
			if (REAL_DEVICES.isEmpty()) {
				log.debug("no connected real ios devices");
			} else {
				log.info("connected real ios devices => {}", REAL_DEVICES);
			}
		}
		return REAL_DEVICES;
	}

}
