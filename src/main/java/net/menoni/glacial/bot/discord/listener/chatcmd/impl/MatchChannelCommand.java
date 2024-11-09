package net.menoni.glacial.bot.discord.listener.chatcmd.impl;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.menoni.glacial.bot.discord.DiscordBot;
import net.menoni.glacial.bot.discord.listener.ChatCommandListener;
import net.menoni.glacial.bot.discord.listener.chatcmd.ChatCommand;
import net.menoni.glacial.bot.jdbc.model.JdbcTeam;
import net.menoni.glacial.bot.service.MatchChannelService;
import net.menoni.glacial.bot.service.MatchService;
import net.menoni.glacial.bot.service.TeamService;
import net.menoni.glacial.bot.util.BracketType;
import net.menoni.glacial.bot.util.DiscordArgUtil;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class MatchChannelCommand implements ChatCommand {

	@Override
	public Collection<String> names() {
		return List.of("matchchannel", "mc");
	}

	@Override
	public Collection<Permission> requiredPermissions() {
		return List.of(Permission.MANAGE_CHANNEL);
	}

	@Override
	public boolean canExecute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, Message message, String alias, String[] args) {
		return ChatCommandListener.requireBotCmdChannel(applicationContext, channel);
	}

	@Override
	public void execute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, Message message, String alias, String[] args) {
		if (args.length < 3) {
			sendHelp(channel, null);
			return;
		}
		boolean primaryBracket = true;
		String roundNumArg = args[0];
		String role1Arg = args[1];
		String role2Arg = args[2];

		if (roundNumArg.length() <= 1) {
			sendHelp(channel, "Provide a bracket type and number (e.g: `a1` or `b1` for primary or secondary bracket)");
			return;
		}

		String roundNumArgFirstCharacter = roundNumArg.substring(0, 1);
		roundNumArg = roundNumArg.substring(1);

		BracketType bracket = BracketType.getBracket(roundNumArgFirstCharacter);
		if (bracket == null) {
			sendHelp(channel, "Invalid bracket type");
			return;
		}
		primaryBracket = bracket.isPrimary();

		int roundNum = -1;
		try {
			roundNum = Integer.parseInt(roundNumArg);
		} catch (NumberFormatException ex) {
			sendHelp(channel, "First argument needs to be a round number (1 and up)");
			return;
		}
		if (!DiscordArgUtil.isRole(role1Arg)) {
			sendHelp(channel, "Second argument needs to be a @ role");
			return;
		}
		if (!DiscordArgUtil.isRole(role2Arg)) {
			sendHelp(channel, "Third argument needs to be a @ role");
			return;
		}

		if (roundNum < 0) {
			sendHelp(channel, "Round number needs to be positive");
			return;
		}

		String teamRole1Id = DiscordArgUtil.getRoleId(role1Arg);
		String teamRole2Id = DiscordArgUtil.getRoleId(role2Arg);

		DiscordBot bot = applicationContext.getBean(DiscordBot.class);
		Role teamRole1 = bot.getRoleById(teamRole1Id);
		Role teamRole2 = bot.getRoleById(teamRole2Id);

		if (teamRole1 == null) {
			sendHelp(channel, "First argument role does not exist");
			return;
		}
		if (teamRole2 == null) {
			sendHelp(channel, "Second argument role does not exist");
			return;
		}

		int roundNumFinal = roundNum;

		MatchChannelService matchChannelService = applicationContext.getBean(MatchChannelService.class);
		matchChannelService.createMatchChannel(primaryBracket, roundNum, teamRole1, teamRole2).whenComplete((matchChannel, error) -> {
			if (error != null) {
				reply(channel, "matchchannel", "Failed to create match channel for %s vs %s (round %s-%d):\n```%s```".formatted(
						teamRole1.getName(), teamRole2.getName(), bracket.name(), roundNumFinal, error.getMessage()
				));
				log.error("Failed to create match channel for %s vs %s (round %s-%d)".formatted(teamRole1.getName(), teamRole2.getName(), bracket.name(), roundNumFinal), error);
				return;
			}
			reply(channel, "matchchannel", "Created <#%s> for <@&%s> vs <@&%s> (round: %s-%d)".formatted(
					matchChannel.getId(),
					teamRole1.getId(),
					teamRole2.getId(),
					bracket.name(),
					roundNumFinal
			));
		});
	}

	@Override
	public Collection<String> help() {
		return List.of(
				"!mc <round-number> <@team-role-1> <@team-role-2> -- manually creates a match channel",
				"round number should be `a1` for primary bracket round 1, and `b1` for secondary bracket round 1"
		);
	}

}
