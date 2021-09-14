package com.example.utils;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.example.Platform;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CommandLineExecutor {

	private static final Platform PLATFORM = Platform.CURRENT_PLATFORM;

	private CommandLineExecutor() {
	}

	/**
	 * Run system commands and get response back.
	 *
	 * @param file {@link String}
	 * @param args {@link String}[] containing arguments that are to be passed to
	 *             executable
	 * @return {@link CommandLineResponse}
	 */
	public static CommandLineResponse execFile(final String file, final String... args) {
		if (isEmpty(file)) {
			return null;
		}

		switch (PLATFORM) {
		case LINUX:
		case MACINTOSH:
			return execCommand(mergeArrays(new String[] { "bash", file.trim() }, args));
		case WINDOWS:
			return execCommand(mergeArrays(new String[] { "cmd", "/c", file.trim() }, args));
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] mergeArrays(T[] arrA, T[] arrB) {
		return (T[]) Stream.of(arrA, arrB).flatMap(Stream::of).toArray();
	}

	private static boolean isEmpty(String str) {
		return str == null || str.trim().isEmpty();
	}

	/**
	 * Run system commands and get response back.
	 *
	 * @param command {@link String}
	 * @return {@link CommandLineResponse}
	 */
	public static CommandLineResponse exec(final String command) {
		if (isEmpty(command)) {
			return null;
		}
		if (Platform.CURRENT_PLATFORM == Platform.WINDOWS) {
			return execCommand("cmd", "/c", command);
		}
		return execCommand("bash", "-c", command.trim());
	}

	public static CommandLineResponse execCommand(final String... command) {
		if (command == null || command.length == 0) {
			return null;
		}
		String _cmd = String.join(" ", command);
		log.debug("executing command : {}", _cmd);
		Process process = null;
		try {
			ProcessBuilder builder = new ProcessBuilder(command);
			CommandLineResponse response = new CommandLineResponse();
			if (Platform.CURRENT_PLATFORM != Platform.WINDOWS) {
				Map<String, String> env = builder.environment();
				env.put("PATH", env.get("PATH") + ":/usr/local/bin:" + System.getenv("HOME") + "/.linuxbrew/bin");
				process = builder.start();
			} else {
				Map<String, String> env = builder.environment();
				env.put("PATH", System.getenv("Path") == null ? System.getenv("PATH") : System.getenv("Path"));
				process = Runtime.getRuntime().exec("cmd /C " + String.join(" ", _cmd));
			}
			process.waitFor(60, TimeUnit.SECONDS);
			response.setStdOut(CommonUtilities.toString(process.getInputStream()).trim());
			response.setErrOut(CommonUtilities.toString(process.getErrorStream()).trim());
			response.setExitCode(process.exitValue());
			log.debug("response: {}", response);
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
	}

}