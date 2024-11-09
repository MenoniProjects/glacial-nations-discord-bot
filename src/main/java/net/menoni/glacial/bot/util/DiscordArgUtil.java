package net.menoni.glacial.bot.util;

import java.util.regex.Pattern;

public class DiscordArgUtil {

	private static final char ZERO = '0';
	private static final char NINE = '9';
	private static final Pattern ROLE_PATTERN = Pattern.compile("<@&([0-9]+)>");

	public static boolean isRole(String arg) {
		return ROLE_PATTERN.matcher(arg).matches();
	}

	public static String getRoleId(String arg) {
		if (!isRole(arg)) {
			return null;
		}
		return getNumericCharacters(arg);
	}

	private static String getNumericCharacters(String arg) {
		StringBuilder sb = new StringBuilder();
		for (char c : arg.toCharArray()) {
			if (c >= ZERO && c <= NINE) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

}
