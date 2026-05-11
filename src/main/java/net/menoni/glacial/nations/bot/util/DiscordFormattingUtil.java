package net.menoni.glacial.nations.bot.util;

import net.dv8tion.jda.api.utils.MarkdownSanitizer;

public class DiscordFormattingUtil {

	public static String escapeFormatting(String input) {
		return MarkdownSanitizer.escape(input);
	}

}
