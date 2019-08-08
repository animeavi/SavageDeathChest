package com.winterhaven_mc.util;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


/**
 * Abstract class that implements methods for handling custom messages
 *
 * @param <E> Enum of MessageId constants
 */
@SuppressWarnings({"unused"})
public abstract class AbstractMessageManager<E extends Enum<E>> implements Listener {

	// reference to main class
	protected final JavaPlugin plugin;

	// reference to MultiverseCore
	private final MultiverseCore mvCore;

	// cooldown hash map
	private final EnumMap<E, Map<UUID, Long>> messageCooldownMap;

	// message file helper
	private final LanguageManager languageManager;

	// configuration object for messages
	protected Configuration messages;


	/**
	 * Constructor for class
	 *
	 * @param plugin         reference to plugin main class
	 * @param MessageIdClass MessageIdClass Enum
	 */
	protected AbstractMessageManager(final JavaPlugin plugin, final Class<E> MessageIdClass) {

		// create pointer to main class
		this.plugin = plugin;

		// get reference to Multiverse-Core if installed
		mvCore = (MultiverseCore) plugin.getServer().getPluginManager().getPlugin("Multiverse-Core");

		// initialize messageCooldownMap
		this.messageCooldownMap = new EnumMap<>(MessageIdClass);

		// instantiate language manager
		this.languageManager = new YamlLanguageManager(plugin);

		// load messages from file
		this.messages = languageManager.loadMessages();

		// register events in this class
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	/**
	 * Event handler for PlayerQuitEvent;
	 * removes player from message cooldown map on logout
	 *
	 * @param event the event handled by this method
	 */
	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent event) {

		// remove player from message cooldown map
		this.removePlayerCooldown(event.getPlayer());
	}


	/**
	 * Reload custom messages file
	 */
	public final void reload() {

		// reload messages
		this.messages = languageManager.loadMessages();
	}


	/**
	 * Add entry to message cooldown map
	 *
	 * @param messageId the message id to use as a key in the cooldown map
	 * @param entity    the entity whose uuid will be added as a key to the cooldown map
	 * @throws NullPointerException if parameter is null
	 */
	private void putMessageCooldown(final E messageId, final Entity entity) {

		// check for null parameters
		Objects.requireNonNull(messageId);
		Objects.requireNonNull(entity);

		// create new HashMap with entity UUID as key
		Map<UUID, Long> tempMap = new ConcurrentHashMap<>();

		// put current time in HashMap with entity UUID as key
		tempMap.put(entity.getUniqueId(), System.currentTimeMillis());

		// put HashMap in cooldown map with messageId as key
		messageCooldownMap.put(messageId, tempMap);
	}


	/**
	 * get entry from message cooldown map
	 *
	 * @param messageId the message identifier for which retrieve cooldown time
	 * @param entity    the entity for whom to retrieve cooldown time
	 * @return cooldown expire time
	 * @throws NullPointerException if parameter is null
	 */
	private long getMessageCooldown(final E messageId, final Entity entity) {

		// check for null parameters
		Objects.requireNonNull(messageId);
		Objects.requireNonNull(entity);

		// check if messageId is in message cooldown hashmap
		if (messageCooldownMap.containsKey(messageId)) {

			// check if messageID is in entity's cooldown hashmap
			if (messageCooldownMap.get(messageId).containsKey(entity.getUniqueId())) {

				// return cooldown time
				return messageCooldownMap.get(messageId).get(entity.getUniqueId());
			}
		}
		return 0L;
	}


	/**
	 * check if player message is in cooldown map
	 *
	 * @param recipient player being sent message
	 * @param messageId message id of message being sent
	 * @return true if player message is not in cooldown map, false if it is
	 * @throws NullPointerException if parameter is null
	 */
	private boolean isCooled(final CommandSender recipient, final E messageId) {

		// check for null parameters
		Objects.requireNonNull(recipient);
		Objects.requireNonNull(messageId);

		// if recipient is entity...
		if (recipient instanceof Entity) {

			// cast recipient to Entity
			Entity entity = (Entity) recipient;

			// get message cooldown time remaining
			long lastDisplayed = getMessageCooldown(messageId, entity);

			// get message repeat delay
			long messageRepeatDelay = getRepeatDelay(messageId);

			// if message has repeat delay value and was displayed to player more recently, do nothing and return
			//noinspection RedundantIfStatement
			if (lastDisplayed > System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(messageRepeatDelay)) {
				return false;
			}
		}
		return true;
	}


	/**
	 * Remove player from message cooldown map
	 *
	 * @param entity the entity (player) to be removed from message cooldown map
	 */
	private void removePlayerCooldown(final Entity entity) {

		// if entity is null or does not have UUID, do nothing and return
		if (entity == null || entity.getUniqueId() == null) {
			return;
		}

		// iterate through all cooldown map keys
		for (E messageId : messageCooldownMap.keySet()) {

			// remove entity UUID from cooldown map
			messageCooldownMap.get(messageId).remove(entity.getUniqueId());
		}
	}


	/**
	 * Check if message is enabled
	 *
	 * @param messageId message identifier to check
	 * @return true if message is enabled, false if not
	 * @throws NullPointerException if parameter is null
	 */
	private boolean isEnabled(final E messageId) {

		// check for null parameter
		Objects.requireNonNull(messageId);

		return messages.getBoolean("MESSAGES." + messageId.toString() + ".enabled");
	}


	/**
	 * get message repeat delay from language file
	 *
	 * @param messageId message identifier to retrieve message delay
	 * @return int message repeat delay in seconds
	 * @throws NullPointerException if parameter is null
	 */
	private long getRepeatDelay(final E messageId) {

		// check for null parameter
		Objects.requireNonNull(messageId);

		return messages.getLong("MESSAGES." + messageId.toString() + ".repeat-delay");
	}


	/**
	 * get message text from language file
	 *
	 * @param messageId message identifier to retrieve message text
	 * @return String message text, or empty string if no message string found
	 * @throws NullPointerException if parameter is null
	 */
	private String getMessage(final E messageId) {

		// check for null parameter
		Objects.requireNonNull(messageId);

		String string = messages.getString("MESSAGES." + messageId.toString() + ".string");

		if (string == null) {
			string = "";
		}

		return ChatColor.translateAlternateColorCodes('&', string);
	}


	/**
	 * Get item name from language specific messages file, with translated color codes
	 *
	 * @return String ITEM_NAME, or empty string if key not found
	 */
	public String getItemName() {

		String string = messages.getString("ITEM_INFO.ITEM_NAME");

		if (string == null) {
			string = "";
		}

		return ChatColor.translateAlternateColorCodes('&', string);
	}


	/**
	 * Get configured plural item name from language file
	 *
	 * @return the formatted plural display name of an item, or empty string if key not found
	 */
	public final String getItemNamePlural() {

		String string = messages.getString("ITEM_INFO.ITEM_NAME_PLURAL");

		if (string == null) {
			string = "";
		}

		return ChatColor.translateAlternateColorCodes('&', string);
	}


	/**
	 * Get item lore from language specific messages file, with translated color codes
	 *
	 * @return List of strings, one string for each line of lore, or empty list if key not found
	 */
	public List<String> getItemLore() {

		List<String> configLore = messages.getStringList("ITEM_INFO.ITEM_LORE");
		List<String> coloredLore = new ArrayList<>();
		for (String line : configLore) {
			coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
		}
		return coloredLore;
	}


	/**
	 * Get spawn display name from language file
	 *
	 * @return the formatted display name for the world spawn, or empty string if key not found
	 */
	public String getSpawnDisplayName() {

		String string = messages.getString("ITEM_INFO.SPAWN_DISPLAY_NAME");

		if (string == null) {
			string = "";
		}

		return ChatColor.translateAlternateColorCodes('&', string);
	}


	/**
	 * Get home display name from language file
	 *
	 * @return the formatted display name for home, or empty string if key not found
	 */
	public String getHomeDisplayName() {

		String string = messages.getString("ITEM_INFO.HOME_DISPLAY_NAME");

		if (string == null) {
			string = "";
		}

		return ChatColor.translateAlternateColorCodes('&', string);
	}


	/**
	 * get current world name of message recipient, using Multiverse alias if available
	 *
	 * @param recipient player to fetch world name
	 * @return String containing recipient world name
	 * @throws NullPointerException if parameter is null
	 */
	public final String getWorldName(final CommandSender recipient) {

		// check for null parameter
		Objects.requireNonNull(recipient);

		// declare recipient world
		World world;

		// if sender is entity, set worldName to entity world name
		if (recipient instanceof Entity) {
			world = ((Entity) recipient).getWorld();
		}
		else {
			// otherwise, use server first world
			world = recipient.getServer().getWorlds().get(0);
		}

		// set result string to world name
		String resultString = world.getName();

		// if Multiverse is enabled, use Mulitverse world alias if available
		if (mvCore != null && mvCore.isEnabled()) {

			// get Multiverse world object
			MultiverseWorld mvWorld = mvCore.getMVWorldManager().getMVWorld(world);

			// if Multiverse alias is not null or empty, set world name to alias if set
			if (mvWorld != null && mvWorld.getAlias() != null && !mvWorld.getAlias().isEmpty()) {
				resultString = mvWorld.getAlias();
			}
		}

		// return resultString
		return resultString;
	}


	/**
	 * Get world name for location, using Multiverse alias if available
	 *
	 * @param location the location used to retrieve world name
	 * @return bukkit world name or multiverse alias as String
	 * @throws NullPointerException if passed location is null
	 */
	public final String getWorldName(final Location location) {

		// check for null parameter
		Objects.requireNonNull(location);

		// declare resultString with world name for location
		String resultString = location.getWorld().getName();

		// if Multiverse is enabled, use Mulitverse world alias if available
		if (mvCore != null && mvCore.isEnabled()) {

			// get Multiverse world object
			MultiverseWorld mvWorld = mvCore.getMVWorldManager().getMVWorld(location.getWorld());

			// if Multiverse alias is not null or empty, set world name to alias if set
			if (mvWorld != null && mvWorld.getAlias() != null && !mvWorld.getAlias().isEmpty()) {
				resultString = mvWorld.getAlias();
			}
		}

		// return resultString
		return resultString;
	}


	/**
	 * Encode string with color codes to create non-visible DisplayName prefix
	 *
	 * @param passedString string to encode with color codes
	 * @return encoded string
	 * @throws NullPointerException if parameter is null
	 */
	public String createHiddenString(final String passedString) {

		// check for null parameter
		Objects.requireNonNull(passedString);

		StringBuilder hidden = new StringBuilder();
		for (char c : passedString.toCharArray())
			hidden.append(ChatColor.COLOR_CHAR + "").append(c);
		return hidden.toString();
	}


	/**
	 * create map of replacement strings with defaults
	 *
	 * @param recipient message recipient
	 * @return Map of replacement strings
	 * @throws NullPointerException if parameter is null
	 */
	abstract protected Map<String, String> getDefaultReplacements(final CommandSender recipient);


	/**
	 * Send message to player
	 *
	 * @param recipient    player receiving message
	 * @param messageId    message identifier in messages file
	 * @param replacements map of string replacements
	 * @throws NullPointerException if parameter is null
	 */
	protected void sendMessage(final CommandSender recipient,
							   final E messageId,
							   final Map<String, String> replacements) {

		// check for null parameters
		Objects.requireNonNull(recipient);
		Objects.requireNonNull(messageId);
		Objects.requireNonNull(replacements);

		// if message is not enabled in messages file, do nothing and return
		if (!isEnabled(messageId)) {
			return;
		}

		// if message is not cooled, do nothing and return
		if (!isCooled(recipient, messageId)) {
			return;
		}

		// get message from file
		String message = getMessage(messageId);

		// for each entry in replacements, replace key substring with value substring in message string
		if (message.contains("%")) {
			for (Map.Entry<String, String> entry : replacements.entrySet()) {
				message = message.replace(entry.getKey(), entry.getValue());
			}
		}

		// send message to player
		recipient.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

		// if message repeat delay value is greater than zero, add entry to messageCooldownMap
		if (getRepeatDelay(messageId) > 0) {
			if (recipient instanceof Entity) {
				putMessageCooldown(messageId, (Entity) recipient);
			}
		}
	}


	/**
	 * Format the time string with days, hours, minutes and seconds as necessary
	 *
	 * @param duration a time duration in milliseconds
	 * @return formatted time string
	 */
	public String getTimeString(final long duration) {
		return getTimeString(duration, TimeUnit.SECONDS);
	}


	/**
	 * Format the time string with days, hours, minutes and seconds as necessary, to the granularity passed
	 *
	 * @param duration a time duration in milliseconds
	 * @param timeUnit the time granularity to display (days | hours | minutes | seconds)
	 * @return formatted time string
	 * @throws NullPointerException if parameter is null
	 */
	public String getTimeString(long duration, final TimeUnit timeUnit) {

		// check for null parameter
		Objects.requireNonNull(timeUnit);

		// if duration is negative, return unlimited time string
		if (duration < 0) {

			String string = this.messages.getString("TIME_STRINGS.UNLIMITED");

			if (string == null) {
				string = "unlimited";
			}

			return ChatColor.translateAlternateColorCodes('&', string);
		}

		// return string if less than 1 of passed timeUnit remains
		String lessString = this.messages.getString("TIME_STRINGS.LESS_THAN_ONE");
		if (lessString == null) {
			lessString = "< 1";
		}
		if (timeUnit.equals(TimeUnit.DAYS)
				&& TimeUnit.MILLISECONDS.toDays(duration) < 1) {
			return lessString + " " + this.messages.getString("TIME_STRINGS.DAY");
		}
		if (timeUnit.equals(TimeUnit.HOURS)
				&& TimeUnit.MILLISECONDS.toHours(duration) < 1) {
			return lessString + " " + this.messages.getString("TIME_STRINGS.HOUR");
		}
		if (timeUnit.equals(TimeUnit.MINUTES)
				&& TimeUnit.MILLISECONDS.toMinutes(duration) < 1) {
			return lessString + " " + this.messages.getString("TIME_STRINGS.MINUTE");
		}
		if (timeUnit.equals(TimeUnit.SECONDS)
				&& TimeUnit.MILLISECONDS.toSeconds(duration) < 1) {
			return lessString + " " + this.messages.getString("TIME_STRINGS.SECOND");
		}


		StringBuilder timeString = new StringBuilder();

		int days = (int) TimeUnit.MILLISECONDS.toDays(duration);
		int hours = (int) TimeUnit.MILLISECONDS.toHours(duration % TimeUnit.DAYS.toMillis(1));
		int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(duration % TimeUnit.HOURS.toMillis(1));
		int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(duration % TimeUnit.MINUTES.toMillis(1));

		String dayString = this.messages.getString("TIME_STRINGS.DAY");
		if (dayString == null) {
			dayString = "day";
		}
		String dayPluralString = this.messages.getString("TIME_STRINGS.DAY_PLURAL");
		if (dayPluralString == null) {
			dayPluralString = "days";
		}
		String hourString = this.messages.getString("TIME_STRINGS.HOUR");
		if (hourString == null) {
			hourString = "hour";
		}
		String hourPluralString = this.messages.getString("TIME_STRINGS.HOUR_PLURAL");
		if (hourPluralString == null) {
			hourPluralString = "hours";
		}
		String minuteString = this.messages.getString("TIME_STRINGS.MINUTE");
		if (minuteString == null) {
			minuteString = "minute";
		}
		String minutePluralString = this.messages.getString("TIME_STRINGS.MINUTE_PLURAL");
		if (minutePluralString == null) {
			minutePluralString = "minutes";
		}
		String secondString = this.messages.getString("TIME_STRINGS.SECOND");
		if (secondString == null) {
			secondString = "second";
		}
		String secondPluralString = this.messages.getString("TIME_STRINGS.SECOND_PLURAL");
		if (secondPluralString == null) {
			secondPluralString = "seconds";
		}

		if (days > 1) {
			timeString.append(days);
			timeString.append(' ');
			timeString.append(dayPluralString);
			timeString.append(' ');
		}
		else if (days == 1) {
			timeString.append(days);
			timeString.append(' ');
			timeString.append(dayString);
			timeString.append(' ');
		}

		if (timeUnit.equals(TimeUnit.HOURS)
				|| timeUnit.equals(TimeUnit.MINUTES)
				|| timeUnit.equals(TimeUnit.SECONDS)) {
			if (hours > 1) {
				timeString.append(hours);
				timeString.append(' ');
				timeString.append(hourPluralString);
				timeString.append(' ');
			}
			else if (hours == 1) {
				timeString.append(hours);
				timeString.append(' ');
				timeString.append(hourString);
				timeString.append(' ');
			}
		}

		if (timeUnit.equals(TimeUnit.MINUTES) || timeUnit.equals(TimeUnit.SECONDS)) {
			if (minutes > 1) {
				timeString.append(minutes);
				timeString.append(' ');
				timeString.append(minutePluralString);
				timeString.append(' ');
			}
			else if (minutes == 1) {
				timeString.append(minutes);
				timeString.append(' ');
				timeString.append(minuteString);
				timeString.append(' ');
			}
		}

		if (timeUnit.equals(TimeUnit.SECONDS)) {
			if (seconds > 1) {
				timeString.append(seconds);
				timeString.append(' ');
				timeString.append(secondPluralString);
			}
			else if (seconds == 1) {
				timeString.append(seconds);
				timeString.append(' ');
				timeString.append(secondString);
			}
		}

		return ChatColor.translateAlternateColorCodes('&', timeString.toString().trim());
	}

}
