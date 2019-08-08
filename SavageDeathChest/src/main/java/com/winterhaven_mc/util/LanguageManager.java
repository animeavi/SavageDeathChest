package com.winterhaven_mc.util;

import org.bukkit.configuration.Configuration;


@SuppressWarnings("unused")
public interface LanguageManager {

	/**
	 * Load messages from yaml file into a Configuration object
	 *
	 * @return Configuration object for configured language file
	 */
	Configuration loadMessages();

}
