package net.menoni.glacial.bot.discord.listener.chatcmd;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import org.springframework.context.ApplicationContext;

import java.util.Collection;

public interface ChatCommand {

	Collection<String> names();

	Collection<Permission> requiredPermissions();

	void execute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, Message message, String alias, String[] args);

	Collection<String> help();

	default boolean canExecute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, Message message, String alias, String[] args) {
		return true;
	}

	default void reply(GuildMessageChannelUnion channel, String alias, String message) {
		channel.sendMessage("**%s**: %s".formatted(alias, message)).queue();
	}

	default void sendHelp(GuildMessageChannelUnion channel, String errorText) {
		String name = names().stream().findFirst().orElse("?");
		if (errorText != null) {
			errorText = " (%s)".formatted(errorText);
		} else {
			errorText = "";
		}
		channel.sendMessage("**%s** help:%s\n%s".formatted(name, errorText, String.join("\n", help()))).queue();
	}

}
