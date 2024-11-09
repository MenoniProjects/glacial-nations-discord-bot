package net.menoni.glacial.bot.discord.model;

import net.dv8tion.jda.api.entities.User;

public record SimpleDiscordUser(
	String id,
	String username,
	String globalName
) {

	public static SimpleDiscordUser of(User user) {
		return new SimpleDiscordUser(
				user.getId(),
				user.getName(),
				user.getGlobalName()
		);
	}

	@Override
	public String toString() {
		return "SimpleDiscordUser{" +
				"\n\tid='" + id + '\'' +
				"\n\tusername='" + username + '\'' +
				"\n\tglobalName='" + globalName + '\'' +
				"\n}";
	}
}
