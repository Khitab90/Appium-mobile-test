package com.example.listener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import com.example.utils.ConfigManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RetryAnalyzer implements IRetryAnalyzer {

	private int retryCount = 0;
	private static final int MAX_RETRY_COUNT = ConfigManager.getInt("retry.count", 0);

	private static final Set<ITestResult> RETRIED_TESTS = new HashSet<>();

	public static Set<ITestResult> getRetriedtests() {
		return RETRIED_TESTS;
	}

	/**
	 * Retrying failed tests
	 *
	 * @param result {@link ITestResult}
	 * @return {@link Boolean}
	 */
	@Override
	public boolean retry(ITestResult result) {

		if (retryCount < MAX_RETRY_COUNT) {
			log.info("retrying test {} with status {} ::: parameters {} for {} time(s)", result.getName(),
					getResultStatusName(result.getStatus()),
					(result.getParameters() == null || result.getParameters().length == 0 ? ""
							: " with params " + Arrays.deepToString(result.getParameters())),
					retryCount + 1);
			retryCount++;
			RETRIED_TESTS.add(result);
			return true;
		} else {
			retryCount = 0;
		}
		return false;
	}

	/**
	 * Status of test
	 *
	 * @param status Integer
	 * @return {@link String}
	 */
	public String getResultStatusName(int status) {

		switch (status) {
		case ITestResult.FAILURE:
			return "FAILURE";
		case ITestResult.SKIP:
			return "SKIPPED";
		case ITestResult.SUCCESS:
			return "SUCCESS";
		default:
			return null;
		}
	}
}
