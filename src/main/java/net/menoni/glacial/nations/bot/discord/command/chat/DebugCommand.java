package net.menoni.glacial.nations.bot.discord.command.chat;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.menoni.glacial.nations.bot.config.Constants;
import net.menoni.glacial.nations.bot.service.MatchChannelService;
import net.menoni.jda.commons.discord.chatcommand.ChatCommand;
import net.menoni.jda.commons.util.DiscordArgUtil;
import net.menoni.ws.discord.service.PickBanService;
import net.menoni.ws.discord.service.support.PickBanOrder;
import net.menoni.ws.discord.service.support.PickBanUser;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Slf4j
public class DebugCommand implements ChatCommand {
	@Override
	public Collection<String> names() {
		return List.of("debug");
	}

	@Override
	public Collection<Permission> requiredPermissions() {
		return List.of(Permission.ADMINISTRATOR);
	}

	@Override
	public String shortHelpText() {
		return "Debug utilities";
	}

	@Override
	public boolean canExecute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, boolean silent) {
		return Objects.equals(Constants.USER_ID_DEV, member.getId());
	}

	@Override
	public boolean execute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, Message message, String alias, String[] args) {
		if (args.length == 0) {
			sendHelp(channel, "Provide a sub-command");
			return true;
		}

		if (args[0].equalsIgnoreCase("pickban")) {
			this._exec_pickban(applicationContext, channel, member, message, alias, args);
		} else {
			reply(channel, alias, "Invalid sub-command: `%s`".formatted(args[0]));
		}

		return true;
	}

	private void _exec_pickban(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, Message message, String alias, String[] args) {
		if (args.length < 3) {
			reply(channel, alias, "Provide 2 discord users for pick-ban");
			return;
		}
		String id1 = DiscordArgUtil.getUserId(args[1]);
		String id2 = DiscordArgUtil.getUserId(args[2]);

		if (id1 == null) {
			reply(channel, alias, "First discord user invalid argument");
			return;
		}
		if (id2 == null) {
			reply(channel, alias, "Second discord user invalid argument");
			return;
		}

		String matchId = "t-" + (System.currentTimeMillis() / 1000);

		Member m1 = channel.getGuild().getMemberById(id1);
		Member m2 = channel.getGuild().getMemberById(id2);

		if (m1 == null) {
			reply(channel, alias, "First discord user not found");
			return;
		}
		if (m2 == null) {
			reply(channel, alias, "Second discord user not found");
			return;
		}

		if (m1.getId().equals(m2.getId())) {
			reply(channel, alias, "Discord users should be 2 different users");
			return;
		}

		PickBanUser pbu1 = PickBanUser.of(m1, "Team 1");
		PickBanUser pbu2 = PickBanUser.of(m2, "Team 2");

		MatchChannelService matchChannelService = applicationContext.getBean(MatchChannelService.class);
		PickBanOrder order = matchChannelService.constructPickBanOrder(pbu1, pbu2);
		if (order == null) {
			reply(channel, alias, "No pick-ban order could be constructed");
			return;
		}

		PickBanService pickBanService = applicationContext.getBean(PickBanService.class);
		pickBanService.startPickBan(
				channel,
				matchId,
				order
		);
	}

	@Override
	public Collection<String> help() {
		return List.of(
				"!debug pickban <user1> <user2> -- Create test pick-ban"
		);
	}
}
