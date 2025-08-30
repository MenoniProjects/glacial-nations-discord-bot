package net.menoni.glacial.nations.bot.discord.command.chat;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.menoni.glacial.nations.bot.discord.command.ChatCommandSupport;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcMatch;
import net.menoni.glacial.nations.bot.service.MatchService;
import net.menoni.glacial.nations.bot.service.ResultService;
import net.menoni.glacial.nations.bot.util.BracketType;
import net.menoni.glacial.nations.bot.util.GNCUtil;
import net.menoni.jda.commons.discord.chatcommand.ChatCommand;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
	public boolean canExecute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, boolean silent) {
		return ChatCommandSupport.requireBotCmdChannel(applicationContext, channel, silent);
	}

	@Override
	public String shortHelpText() {
		return "Forcibly set a match result";
	}

	@Override
	public boolean execute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, Message message, String alias, String[] args) {
		if (args.length < 3) {
			sendHelp(channel, "Not enough arguments");
			return true;
		}

		boolean primaryBracket = true;
		String roundNumberArg = args[0];
		String winArg = args[1];
		String loseArg = args[2];

		String roundNumArgFirstCharacter = roundNumberArg.substring(0, 1);
		roundNumberArg = roundNumberArg.substring(1);

		BracketType bracket = BracketType.getBracket(roundNumArgFirstCharacter);
		if (bracket == null) {
			sendHelp(channel, "Invalid bracket type");
			return true;
		}
		primaryBracket = bracket.isPrimary();

		int roundNumber = -1;
		try {
			roundNumber = Integer.parseInt(roundNumberArg);
		} catch (NumberFormatException ex) {
			reply(channel, alias, "Invalid round number argument, first argument needs to be a (positive) round number");
			return true;
		}

		GNCUtil.TeamAndCaptain tacWin;
		GNCUtil.TeamAndCaptain tacLose;

		try {
			tacWin = GNCUtil.resolveTeamAndCaptain(applicationContext, winArg);
			tacLose = GNCUtil.resolveTeamAndCaptain(applicationContext, loseArg);
		} catch (Exception e) {
			reply(channel, alias, "Failed to resolve team or captain argument:\n```\n%s\n```".formatted(e.getMessage()));
			return true;
		}

		if (tacWin == null) {
			reply(channel, alias, "Failed to find win team/captain");
			return true;
		}
		if (tacLose == null) {
			reply(channel, alias, "Failed to find lose team/captain");
			return true;
		}

		MatchService matchService = applicationContext.getBean(MatchService.class);
		JdbcMatch match = matchService.getMatchExact(primaryBracket, roundNumber, tacWin.captain().getMenoniMember().getUser().getId(), tacLose.captain().getMenoniMember().getUser().getId());

		boolean anyResult = false;
		if (match != null) {
			int winIndex = Objects.equals(tacWin.captain().getMenoniMember().getUser().getId(), match.getFirstCaptainId()) ? 1 : 2;
			matchService.setMatchWinner(match, winIndex);
			reply(channel, alias, "Applying match winner");
			anyResult = true;
		}

		if (tacWin.team() != null && tacLose.team() != null) {
			ResultService resultService = applicationContext.getBean(ResultService.class);
			resultService.sendResultsMessage(primaryBracket, roundNumber, tacWin.team(), tacLose.team());
			reply(channel, alias, "Sending result message");
			anyResult = true;
		}
		if (!anyResult) {
			reply(channel, alias, "Match not found and no teams found to send result message for");
		}
		return true;
	}

	@Override
	public Collection<String> help() {
		return List.of(
				"!forcewin <round-number> <@win-team> <@lose-team> -- forcibly reports a result",
				"round number should be `a1` for primary bracket round 1, and `b1` for secondary bracket round 1"
		);
	}
}
