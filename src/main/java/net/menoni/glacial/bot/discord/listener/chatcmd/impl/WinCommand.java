package net.menoni.glacial.bot.discord.listener.chatcmd.impl;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.menoni.glacial.bot.discord.DiscordBot;
import net.menoni.glacial.bot.discord.listener.chatcmd.ChatCommand;
import net.menoni.glacial.bot.jdbc.model.JdbcMatch;
import net.menoni.glacial.bot.jdbc.model.JdbcTeam;
import net.menoni.glacial.bot.jdbc.model.JdbcTeamSignup;
import net.menoni.glacial.bot.service.MatchService;
import net.menoni.glacial.bot.service.ResultService;
import net.menoni.glacial.bot.service.TeamService;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Slf4j
public class WinCommand implements ChatCommand {
	@Override
	public Collection<String> names() {
		return List.of("win");
	}

	@Override
	public Collection<Permission> requiredPermissions() {
		return List.of();
	}

	@Override
	public boolean canExecute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, Message message, String alias, String[] args) {
		DiscordBot bot = applicationContext.getBean(DiscordBot.class);
		MatchService matchService = applicationContext.getBean(MatchService.class);
		// team lead requirement
		if (!matchService.isMatchChannel(channel.getId())) {
			reply(channel, alias, "Command needs to be executed in a known match channel");
			return false;
		}
		if (member.getRoles().stream().map(Role::getId).noneMatch(id -> Objects.equals(id, bot.getConfig().getTeamLeadRoleId()))) {
			reply(channel, alias, "Only team leads can run this command");
			return false;
		}
		return true;
	}

	@Override
	public void execute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, Message message, String alias, String[] args) {
		DiscordBot bot = applicationContext.getBean(DiscordBot.class);
		TeamService teamService = applicationContext.getBean(TeamService.class);
		MatchService matchService = applicationContext.getBean(MatchService.class);

		JdbcMatch match = matchService.getMatchForChannel(channel.getId());
		if (match == null) {
			reply(channel, alias, "Match not found - <@%s>".formatted(bot.getConfig().getStaffRoleId()));
			return;
		}
		// mark win
		JdbcTeamSignup teamLeadSignup = teamService.getSignupForMember(member);
		if (teamLeadSignup == null) {
			reply(channel, alias, "Could not find your sign-up entry - <@%s>".formatted(bot.getConfig().getStaffRoleId()));
			return;
		}
		if (match.getWinTeamId() != null) {
			reply(channel, alias, "This match has already been reported as complete - ask staff to override results if needed.");
			return;
		}
		match.setWinTeamId(teamLeadSignup.getTeamId());
		matchService.updateMatch(match);

		JdbcTeam winTeam = teamService.getTeamById(teamLeadSignup.getTeamId());
		JdbcTeam loseTeam = teamService.getTeamById(Objects.equals(teamLeadSignup.getTeamId(), match.getFirstTeamId()) ? match.getSecondTeamId() : match.getFirstTeamId());

		ResultService resultService = applicationContext.getBean(ResultService.class);
		resultService.sendResultsMessage(match.getPrimaryBracket(), match.getRoundNumber(), winTeam, loseTeam).whenComplete((msg, throwable) -> {
			if (throwable != null) {
				log.error("Error executing !win for %s in %s-vs-%s".formatted(
						member.getEffectiveName(),
						winTeam.getName(),
						loseTeam.getName()
				), throwable);
				channel.sendMessage("Failed to mark match as won, contact a staff").queue();
				return;
			}
			channel.delete().queue();
		});
	}

	@Override
	public Collection<String> help() {
		return List.of(
				"!win -- marks match as won"
		);
	}
}
