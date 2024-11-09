package net.menoni.glacial.bot.discord.listener.chatcmd.impl;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.menoni.glacial.bot.discord.listener.ChatCommandListener;
import net.menoni.glacial.bot.discord.listener.chatcmd.ChatCommand;
import net.menoni.glacial.bot.jdbc.model.JdbcMatch;
import net.menoni.glacial.bot.jdbc.model.JdbcTeam;
import net.menoni.glacial.bot.service.MatchService;
import net.menoni.glacial.bot.service.ResultService;
import net.menoni.glacial.bot.service.TeamService;
import net.menoni.glacial.bot.util.BracketType;
import net.menoni.glacial.bot.util.DiscordArgUtil;
import net.menoni.glacial.bot.util.DiscordRoleUtil;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.List;

public class ForceWinCommand implements ChatCommand {
	@Override
	public Collection<String> names() {
		return List.of("forcewin");
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
			sendHelp(channel, "Not enough arguments");
			return;
		}

		boolean primaryBracket = true;
		String roundNumberArg = args[0];
		String winTeamArg = args[1];
		String loseTeamArg = args[2];

		String roundNumArgFirstCharacter = roundNumberArg.substring(0, 1);
		roundNumberArg = roundNumberArg.substring(1);

		BracketType bracket = BracketType.getBracket(roundNumArgFirstCharacter);
		if (bracket == null) {
			sendHelp(channel, "Invalid bracket type");
			return;
		}
		primaryBracket = bracket.isPrimary();

		int roundNumber = -1;
		try {
			roundNumber = Integer.parseInt(roundNumberArg);
		} catch (NumberFormatException ex) {
			reply(channel, alias, "Invalid round number argument, first argument needs to be a (positive) round number");
			return;
		}
		if (!DiscordArgUtil.isRole(winTeamArg)) {
			reply(channel, alias, "Second argument needs to be a team @");
			return;
		}
		if (!DiscordArgUtil.isRole(loseTeamArg)) {
			reply(channel, alias, "Third argument needs to be a team @");
			return;
		}

		String winRoleId = DiscordArgUtil.getRoleId(winTeamArg);
		String loseRoleId = DiscordArgUtil.getRoleId(loseTeamArg);

		TeamService teamService = applicationContext.getBean(TeamService.class);
		JdbcTeam winTeam = teamService.getTeamByRoleId(winRoleId);
		JdbcTeam loseTeam = teamService.getTeamByRoleId(loseRoleId);

		if (winTeam == null) {
			reply(channel, alias, "Team not found for win-team argument");
			return;
		}
		if (loseTeam == null) {
			reply(channel, alias, "Team not found for lose-team argument");
			return;
		}

		MatchService matchService = applicationContext.getBean(MatchService.class);
		JdbcMatch match = matchService.getMatchExact(primaryBracket, roundNumber, winTeam.getId(), loseTeam.getId());

		if (match != null) {
			match.setWinTeamId(winTeam.getId());
			matchService.updateMatch(match);
		}

		ResultService resultService = applicationContext.getBean(ResultService.class);
		resultService.sendResultsMessage(primaryBracket, roundNumber, winTeam, loseTeam);
		reply(channel, alias, "Sending result message");
	}

	@Override
	public Collection<String> help() {
		return List.of(
				"!forcewin <round-number> <@win-team> <@lose-team> -- forcibly reports a result",
				"round number should be `a1` for primary bracket round 1, and `b1` for secondary bracket round 1"
		);
	}
}
