package net.menoni.glacial.nations.bot.discord.command.chat;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.menoni.glacial.nations.bot.discord.command.ChatCommandSupport;
import net.menoni.glacial.nations.bot.service.TeamService;
import net.menoni.jda.commons.discord.chatcommand.ChatCommand;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.List;

public class RefreshTeamsCommand implements ChatCommand {
	@Override
	public Collection<String> names() {
		return List.of("refreshteams");
	}

	@Override
	public Collection<Permission> requiredPermissions() {
		return List.of(Permission.MANAGE_ROLES);
	}

	@Override
	public boolean canExecute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, boolean silent) {
		return ChatCommandSupport.requireBotCmdChannel(applicationContext, channel, silent);
	}

	@Override
	public String shortHelpText() {
		return "Refresh teams message";
	}

	@Override
	public boolean execute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, Message message, String alias, String[] args) {
		applicationContext.getBean(TeamService.class).updateTeamsMessage();
		reply(channel, alias, "Refreshing teams message");
		return true;
	}

	@Override
	public Collection<String> help() {
		return List.of(
				"!refreshteams -- refreshes message in teams channel"
		);
	}
}
