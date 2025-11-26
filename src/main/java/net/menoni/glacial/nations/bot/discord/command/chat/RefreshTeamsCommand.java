package net.menoni.glacial.nations.bot.discord.command.chat;

import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.menoni.glacial.nations.bot.discord.command.ChatCommandSupport;
import net.menoni.glacial.nations.bot.service.SignupSheetService;
import net.menoni.glacial.nations.bot.service.TeamService;
import net.menoni.jda.commons.discord.chatcommand.ChatCommand;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Slf4j
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
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("sheet")) {
				SignupSheetService signupSheetService = applicationContext.getBean(SignupSheetService.class);
				try {
					reply(channel, alias, "Running manual import");
					List<String> res = signupSheetService.runManualSignupSheetImport();
					reply(channel, alias, "Result:\n" + String.join("\n", res));
				} catch (IOException | CsvException e) {
					reply(channel, alias, "Error: " + e.getMessage());
					log.error("Failed to manually import signups", e);
				}
				return true;
			}
		}
		applicationContext.getBean(TeamService.class).updateTeamsMessage();
		reply(channel, alias, "Refreshing teams message");
		return true;
	}

	@Override
	public Collection<String> help() {
		return List.of(
				"!refreshteams -- refreshes message in teams channel",
				"!refreshteams sheet -- refresh teams from sheet"
		);
	}
}
