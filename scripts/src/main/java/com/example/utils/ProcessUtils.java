package com.example.utils;

import com.example.Platform;

public final class ProcessUtils {

	private ProcessUtils() {
	}

	/**
	 * This method is to kill any process listening at the given port
	 * 
	 * @param port {@link Integer}
	 */
	public static void killProcessListeningAtPort(final int port) {
		if (port < 1) {
			return;
		}
		if (Platform.CURRENT_PLATFORM == Platform.WINDOWS) {
			CommandLineResponse res = CommandLineExecutor.exec("netstat -ano | findstr LISTENING | findstr :" + port);
			if (res.getExitCode() == 0) {
				String str = CommonUtilities.splitLines(res.getStdOut()).get(0).replaceAll("//s+", " ");
				String id = str.substring(str.lastIndexOf(" ") + 1);
				CommandLineExecutor.exec(String.format("taskkill /PID %s /F", id));
			}
		} else {
			String command = String
					.format(Platform.CURRENT_PLATFORM == Platform.MACINTOSH ? "lsof -nti:%s | xargs kill -9"
							: "fuser -k %s/tcp", port);
			CommandLineExecutor.exec(command);
		}
	}

	/**
	 * This method is to kill all processes running at the given port
	 * 
	 * @param process {@link String}
	 */
	public static void killProcesses(final String process) {
		if (CommonUtilities.isBlank(process)) {
			return;
		}
		String command = String.format("ps -ef | grep '%s' | awk '{print $2}' | tr -s '\\n' ' ' | xargs kill -9",
				process.trim());
		CommandLineExecutor.exec(command);
	}
}