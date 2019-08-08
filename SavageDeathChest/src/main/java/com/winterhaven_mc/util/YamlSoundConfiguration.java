package com.winterhaven_mc.util;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;


@SuppressWarnings("unused")
public class YamlSoundConfiguration implements SoundConfiguration {

	// reference to plugin main class
	private final JavaPlugin plugin;

	// ConfigAccessor for sounds file
	private YamlConfiguration sounds;

	// default sound file name
	private final String soundFileName = "sounds.yml";

	// Set of valid sound enum names as strings
	private static final Set<String> validSoundNames = new HashSet<>();


	/**
	 * Class constructor
	 *
	 * @param plugin reference to plugin main class
	 */
	public YamlSoundConfiguration(final JavaPlugin plugin) {

		// set reference to main class
		this.plugin = plugin;

		// instantiate custom sound manager
		this.sounds = new YamlConfiguration();

		// populate valid sound names set
		for (Sound s : Sound.values()) {
			validSoundNames.add(s.name());
		}

		// load sounds
		this.reload();
	}


	/**
	 * Load sound configuration from yaml file
	 */
	@Override
	public final void reload() {

		// reinstall sound file if necessary; this will not overwrite an existing file
		// note: manually check for file existence to prevent log message when file already exists
		if (!new File(plugin.getDataFolder() + File.separator + soundFileName).exists()) {
			plugin.saveResource(soundFileName, false);
		}

		// get file object for sound file
		File soundFile = new File(plugin.getDataFolder() + File.separator + soundFileName);

		// create new YamlConfiguration object
		YamlConfiguration newSoundConfig = new YamlConfiguration();

		// try to load sound file into new YamlConfiguration object
		try {
			newSoundConfig.load(soundFile);
			plugin.getLogger().info("Sound file " + soundFileName + " successfully loaded.");
		}
		catch (FileNotFoundException e) {
			plugin.getLogger().severe("Sound file " + soundFileName + " does not exist.");
		}
		catch (IOException e) {
			plugin.getLogger().severe("Sound file " + soundFileName + " could not be read.");
		}
		catch (InvalidConfigurationException e) {
			plugin.getLogger().severe("Sound file " + soundFileName + " is not valid yaml.");
		}

		// Set defaults to embedded resource file

		// get input stream reader for embedded resource file
		Reader defaultConfigStream = new InputStreamReader(plugin.getResource(soundFileName), StandardCharsets.UTF_8);

		// load embedded resource stream into Configuration object
		Configuration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);

		// set Configuration object as defaults for messages configuration
		newSoundConfig.setDefaults(defaultConfig);

		// set class field to new YamlConfiguration object
		this.sounds = newSoundConfig;
	}


	/**
	 * Play sound effect for player
	 *
	 * @param sender  the command sender (player) to play sound
	 * @param soundId the sound identifier enum member
	 */
	@Override
	public final void playSound(final CommandSender sender, final Enum<?> soundId) {

		// if sound effects are configured false, do nothing and return
		if (!plugin.getConfig().getBoolean("sound-effects")) {
			return;
		}

		// if sender is not a player do nothing and return
		if (!(sender instanceof Player)) {
			return;
		}

		// cast sender to player
		Player player = (Player) sender;

		// if sound is set to enabled in sounds file
		if (sounds.getBoolean(soundId.toString() + ".enabled")) {

			// get player only setting from config file
			boolean playerOnly = sounds.getBoolean(soundId.toString() + ".player-only");

			// get sound name from config file
			String soundName = sounds.getString(soundId.toString() + ".sound");

			// get sound volume from config file
			float volume = (float) sounds.getDouble(soundId.toString() + ".volume");

			// get sound pitch from config file
			float pitch = (float) sounds.getDouble(soundId.toString() + ".pitch");

			// check that sound name is valid
			if (validSoundNames.contains(soundName)) {

				// if sound is set player only, use player.playSound()
				if (playerOnly) {
					player.playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch);
				}
				// else use world.playSound() so other players in vicinity can hear
				else {
					player.getWorld().playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch);
				}
			}
			else {
				plugin.getLogger().warning("An error occurred while trying to play the sound '"
						+ soundName + "'. You probably need to update the sound name in your "
						+ soundFileName + " file.");
			}
		}
	}


	/**
	 * Play sound effect for location
	 *
	 * @param location the location at which to play sound
	 * @param soundId  the sound identifier enum member
	 */
	@Override
	public final void playSound(final Location location, final Enum<?> soundId) {

		// if location is null, do nothing and return
		if (location == null) {
			return;
		}

		// if sound effects are configured false, do nothing and return
		if (!plugin.getConfig().getBoolean("sound-effects")) {
			return;
		}

		// if sound is set to enabled in sounds file
		if (sounds.getBoolean(soundId.toString() + ".enabled")) {

			// get sound name from config file
			String soundName = sounds.getString(soundId.toString() + ".sound");

			// get sound volume from config file
			float volume = (float) sounds.getDouble(soundId.toString() + ".volume");

			// get sound pitch from config file
			float pitch = (float) sounds.getDouble(soundId.toString() + ".pitch");

			// check that sound name is valid
			if (validSoundNames.contains(soundName)) {

				// else use world.playSound() so other players in vicinity can hear
				location.getWorld().playSound(location, Sound.valueOf(soundName), volume, pitch);
			}
			else {
				plugin.getLogger().warning("An error occurred while trying to play the sound '"
						+ soundName + "'. You probably need to update the sound name in your "
						+ soundFileName + " file.");
			}
		}
	}

}
