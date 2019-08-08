package com.winterhaven_mc.util;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;


@SuppressWarnings("unused")
public interface SoundConfiguration {


	/**
	 * Load sound configuration
	 */
	void reload();


	/**
	 * Play sound effect for player
	 *
	 * @param sender  the command sender (player) to play sound
	 * @param soundId the sound identifier enum member
	 */
	void playSound(final CommandSender sender, final Enum<?> soundId);


	/**
	 * Play sound effect for location
	 *
	 * @param location the location at which to play sound
	 * @param soundId  the sound identifier enum member
	 */
	void playSound(final Location location, final Enum<?> soundId);

}
