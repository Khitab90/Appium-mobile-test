package com.example;

import com.example.utils.CommonUtilities;
import com.example.utils.ConfigManager;

public enum Channel {

	NATIVE_ANDROID("android"), NATIVE_IOS("ios"), MOBILE_WEB("mobile");

	private final String name;

	Channel(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	public static Channel getChannel() {
		return getChannel(ConfigManager.getString("ui.channel"));
	}

	public static Channel getChannel(final String name) {
		if (CommonUtilities.isBlank(name)) {
			return MOBILE_WEB;
		}
		for (Channel channel : values()) {
			if (channel.toString().equals(name)) {
				return channel;
			}
		}
		return MOBILE_WEB;
	}
}
