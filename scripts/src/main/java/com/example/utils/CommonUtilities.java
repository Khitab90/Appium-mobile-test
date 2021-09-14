package com.example.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.example.Platform;
import com.google.common.io.CharStreams;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CommonUtilities {

	private CommonUtilities() {
	}

	public static boolean isBlank(String str) {
		return str == null || str.trim().isEmpty();
	}

	public static String toString(@NonNull String url) {
		try {
			return toString(new URL(url));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String toString(@NonNull URL url) {

		try {
			return toString(url.openStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.error("unable to read stream from url '{}', url");
		return null;
	}

	public static String toString(@NonNull InputStream stream) {
		try (Reader reader = new InputStreamReader(stream)) {
			return CharStreams.toString(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<String> getMatches(@NonNull String text, @NonNull String regex) {
		return getMatches(text, regex, Pattern.DOTALL);
	}

	public static List<String> getMatches(@NonNull String text, @NonNull String regex, int flag) {
		if (isBlank(text) || isBlank(regex)) {
			return Collections.emptyList();
		}
		Pattern pattern = Pattern.compile(regex, flag);
		Matcher matcher = pattern.matcher(text);

		List<String> matches = new ArrayList<>();
		while (matcher.find()) {
			matches.add(matcher.group());
		}
		return matches;
	}

	public static List<List<String>> getMatchedGroups(@NonNull String text, @NonNull String regex) {
		Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(text);
		List<List<String>> list = new ArrayList<>();
		while (matcher.find()) {
			list.add(IntStream.range(1, matcher.groupCount() + 1).mapToObj(matcher::group)
					.collect(Collectors.toList()));
		}
		return list;
	}

	public static List<String> splitLines(@NonNull String str) {
		return Arrays.asList(str.split("\r?\n"));
	}

	public static File findLocalExecutable(String name) {
		String cmd = (Platform.CURRENT_PLATFORM == Platform.WINDOWS ? "where " : "which ") + name;
		CommandLineResponse response = CommandLineExecutor.exec(cmd);
		String path = response.getExitCode() == 0 ? CommonUtilities.splitLines(response.getStdOut().trim()).get(0) : null;
		return path != null ? new File(path) : null;
	}

	public static List<URL> getResources(@NonNull String file) {
		try {
			return Collections.list(Thread.currentThread().getContextClassLoader().getResources(file.trim()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}
}
