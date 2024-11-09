package net.menoni.glacial.bot.util;

import java.util.regex.Pattern;

public class DiscordFormattingUtil {

	private static final String UNDERSCORE = Pattern.quote("_");
	private static final String ASTERISK = Pattern.quote("*");

	public static String escapeFormatting(String input) {
		input = input.replaceAll(UNDERSCORE, "\\\\_");
		input = input.replaceAll(ASTERISK, "\\\\*");
		return input;
	}

}
