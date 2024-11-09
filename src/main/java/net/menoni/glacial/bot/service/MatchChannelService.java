package net.menoni.glacial.bot.service;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.menoni.glacial.bot.discord.DiscordBot;
import net.menoni.glacial.bot.jdbc.model.JdbcTeam;
import net.menoni.glacial.bot.util.BracketType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Service
public class MatchChannelService {

	@Autowired
	private DiscordBot bot;

	@Autowired
	private TeamService teamService;
	@Autowired
	private MatchService matchService;

	public CompletableFuture<TextChannel> createMatchChannel(boolean primaryBracket, int roundNum, Role teamRole1, Role teamRole2) {
		CompletableFuture<TextChannel> future = new CompletableFuture<>();
		Category category = bot.applyGuild(g -> g.getCategoryById(bot.getConfig().getMatchesCategoryId(primaryBracket)), null);
		if (category == null) {
			future.completeExceptionally(new Exception("Match channel category not found"));
			return future;
		}

		String t1 = teamRole1.getName().toLowerCase().replaceAll(Pattern.quote(" "), "-");
		String t2 = teamRole2.getName().toLowerCase().replaceAll(Pattern.quote(" "), "-");

		String channelName = "%s-%s".formatted(t1, t2);
		ChannelAction<TextChannel> createAction = category
				.createTextChannel("%s%d-%s".formatted(BracketType.getBracket(primaryBracket).getCharacter(), roundNum, channelName))
				.addRolePermissionOverride(category.getGuild().getIdLong(), null, List.of(Permission.VIEW_CHANNEL)) // not public
				.addPermissionOverride(bot.getBotRole(), List.of(Permission.VIEW_CHANNEL), null)
				.addPermissionOverride(teamRole1, List.of(Permission.VIEW_CHANNEL), null)
				.addPermissionOverride(teamRole2, List.of(Permission.VIEW_CHANNEL), null);

		Role adminRole = bot.getRoleById(bot.getConfig().getAdminRoleId());
		Role staffRole = bot.getRoleById(bot.getConfig().getStaffRoleId());
		Role casterRole = bot.getRoleById(bot.getConfig().getCasterRoleId());

		createAction = applyRoleViewAccess(createAction, adminRole);
		createAction = applyRoleViewAccess(createAction, staffRole);
		createAction = applyRoleViewAccess(createAction, casterRole);

		int roundNumFinal = roundNum;

		JdbcTeam team1 = teamService.getTeamByRoleId(teamRole1.getId());
		JdbcTeam team2 = teamService.getTeamByRoleId(teamRole2.getId());

		createAction.queue(c -> {
			matchService.setMatchChannel(primaryBracket, roundNumFinal, team1.getId(), team2.getId(), c.getId());
			future.complete(c);
		}, future::completeExceptionally);
		return future;
	}

	private ChannelAction<TextChannel> applyRoleViewAccess(ChannelAction<TextChannel> action, Role role) {
		if (role != null) {
			return action.addPermissionOverride(role, List.of(Permission.VIEW_CHANNEL), null);
		}
		return action;
	}

}
