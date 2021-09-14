package com.example.utils;

import static com.example.utils.CommonUtilities.getResources;
import static com.example.utils.CommonUtilities.isBlank;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ConfigManager {

	private static final String DEFAULT_CONFIG_FILE = "default.properties";
	private static final String CONFIG_FILE = "config.properties";

	private static final Properties PROPS = new Properties();
	private static final String ENV = getString("env");

	private ConfigManager() {
	}

	private static void loadProperties() {
		loadProperties(DEFAULT_CONFIG_FILE);
		loadProperties(CONFIG_FILE);
	}

	private static void loadProperties(String confFile) {
		List<URL> files = getResources(confFile);
		if (files != null && !files.isEmpty()) {
			try (InputStream stream = files.get(0).openStream()) {
				PROPS.load(stream);
			} catch (IOException e) {
				log.error("unable to load properties file '{}'", confFile);
				e.printStackTrace();
			}
		}
	}

	public static String getString(@NonNull String key) {
		return getString(key, null);
	}

	public static String getString(@NonNull String propertyKey, String defaultValue) {

		if (PROPS.isEmpty()) {
			synchronized (ConfigManager.class) {
				if (PROPS.isEmpty()) {
					loadProperties();
				}
			}
		}
		String key = propertyKey.trim();
		String keyWithEnv = String.format("%s.%s", ENV, key);

		String value = null;

		if (!isBlank(ENV)) {
			value = System.getenv(keyWithEnv);
			if (value != null) {
				return value.trim();
			}
		}
		value = System.getenv(key);
		if (value != null) {
			return value.trim();
		}
		if (!isBlank(ENV)) {
			value = System.getProperty(keyWithEnv, PROPS.getProperty(keyWithEnv));
		}
		return isBlank(value) ? System.getProperty(key, PROPS.getProperty(key, defaultValue)) : value;
	}

	public static int getInt(@NonNull String key) {
		return getInt(key, 0);
	}

	public static int getInt(@NonNull String key, int defaultValue) {
		try {
			return Integer.parseInt(getString(key, String.valueOf(defaultValue)));
		} catch (Exception e) {
			return 0;
		}
	}

	public static float getFloat(@NonNull String key) {
		return getFloat(key, 0);
	}

	public static float getFloat(@NonNull String key, float defaultValue) {
		try {
			return Float.parseFloat(getString(key, String.valueOf(defaultValue)));
		} catch (Exception e) {
			return 0;
		}
	}

	public static long getLong(@NonNull String key) {
		return getLong(key, 0);
	}

	public static long getLong(@NonNull String key, long defaultValue) {
		try {
			return Long.parseLong(getString(key, String.valueOf(defaultValue)));
		} catch (Exception e) {
			return 0;
		}
	}

	public static double getDouble(@NonNull String key) {
		return getDouble(key, 0);
	}

	public static double getDouble(@NonNull String key, double defaultValue) {
		try {
			return Double.parseDouble(getString(key, String.valueOf(defaultValue)));
		} catch (Exception e) {
			return 0;
		}
	}

	public static boolean getBoolean(@NonNull String key) {
		return getBoolean(key, false);
	}

	public static boolean getBoolean(@NonNull String key, boolean defaultValue) {
		try {
			return Boolean.parseBoolean(getString(key, String.valueOf(defaultValue)));
		} catch (Exception e) {
			return false;
		}
	}

	public static List<String> getList(@NonNull String key) {
		return getList(key, ",");
	}

	public static List<String> getList(@NonNull String key, String delimiter) {
		try {
			String value = getString(key);
			return isBlank(value) ? Collections.emptyList()
					: Arrays.stream(value.split(delimiter)).map(String::trim).collect(Collectors.toList());
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	public static List<Integer> getIntList(@NonNull String key) {
		return getIntList(key, ",");
	}

	public static List<Integer> getIntList(@NonNull String key, String delimiter) {
		return getList(key, delimiter).stream().map(Integer::parseInt).collect(Collectors.toList());
	}

	public static List<Float> getFloatList(@NonNull String key) {
		return getFloatList(key, ",");
	}

	public static List<Float> getFloatList(@NonNull String key, String delimiter) {
		return getList(key, delimiter).stream().map(Float::parseFloat).collect(Collectors.toList());
	}

	public static List<Long> getLongList(@NonNull String key) {
		return getLongList(key, ",");
	}

	public static List<Long> getLongList(@NonNull String key, String delimiter) {
		return getList(key, delimiter).stream().map(Long::parseLong).collect(Collectors.toList());
	}

	public static List<Double> getDoubleList(@NonNull String key) {
		return getDoubleList(key, ",");
	}

	public static List<Double> getDoubleList(@NonNull String key, String delimiter) {
		return getList(key, delimiter).stream().map(Double::parseDouble).collect(Collectors.toList());
	}

	public static List<Boolean> getBooleanList(@NonNull String key) {
		return getBooleanList(key, ",");
	}

	public static List<Boolean> getBooleanList(@NonNull String key, String delimiter) {
		return getList(key, delimiter).stream().map(Boolean::parseBoolean).collect(Collectors.toList());
	}

}