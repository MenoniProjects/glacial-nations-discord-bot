package net.menoni.glacial.nations.bot.discord.command.chat;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.menoni.glacial.nations.bot.discord.command.ChatCommandSupport;
import net.menoni.glacial.nations.bot.service.MatchChannelService;
import net.menoni.glacial.nations.bot.util.BracketType;
import net.menoni.glacial.nations.bot.util.DiscordLinkUtil;
import net.menoni.glacial.nations.bot.util.GNCUtil;
import net.menoni.jda.commons.discord.chatcommand.ChatCommand;
import net.menoni.jda.commons.util.JDAUtil;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
	public boolean canExecute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, boolean silent) {
		return ChatCommandSupport.requireBotCmdChannel(applicationContext, channel, silent);
	}

	@Override
	public String shortHelpText() {
		return "Create match channel manually";
	}

	@Override
	public boolean execute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, Message message, String alias, String[] args) {
		if (args.length == 0) {
			sendHelp(channel, null);
			return true;
		}

		if (args[0].equalsIgnoreCase("start")) {
			this._exec_start(applicationContext, channel, member, message, alias, args);
		} else if (args[0].equalsIgnoreCase("message")) {
			this._exec_msg(applicationContext, channel, member, message, alias, args);
		} else {
			sendHelp(channel, null);
		}

		return true;
	}

	private void _exec_msg(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, Message message, String alias, String[] args) {
		if (args.length < 2) {
			reply(channel, alias, "Not enough arguments");
			return;
		}

		if (!DiscordLinkUtil.isDiscordMessageLink(args[1])) {
			reply(channel, alias, "Invalid message link");
			return;
		}

		String guildId = DiscordLinkUtil.getDiscordMessageLinkGuildId(args[1]);
		String channelId = DiscordLinkUtil.getDiscordMessageLinkChannelId(args[1]);
		String messageId = DiscordLinkUtil.getDiscordMessageLinkMessageId(args[1]);

		if (guildId == null || channelId == null || messageId == null) {
			List<String> fields = new ArrayList<>();
			if (guildId == null) {
				fields.add("guildId");
			}
			if (channelId == null) {
				fields.add("channelId");
			}
			if (messageId == null) {
				fields.add("messageId");
			}
			reply(channel, alias, "Failed to resolve message link (resolve failed of: %s)".formatted(String.join(", ", fields)));
			return;
		}

		if (!guildId.equals(channel.getGuild().getId())) {
			reply(channel, alias, "Message needs to be in this server");
			return;
		}
		TextChannel c = channel.getGuild().getTextChannelById(channelId);
		if (c == null) {
			reply(channel, alias, "Channel not found for message link");
			return;
		}

		Message msg = JDAUtil.queueAndWait(c.retrieveMessageById(messageId));
		if (msg == null) {
			reply(channel, alias, "Message not found for link");
			return;
		}

		MatchChannelService matchChannelService = applicationContext.getBean(MatchChannelService.class);
		matchChannelService.getPinnedMessageText().setValue(msg.getContentRaw());
		reply(channel, alias, "Set pin-message content to %s".formatted(msg.getJumpUrl()));
	}

	private void _exec_start(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, Message message, String alias, String[] args) {
		if (args.length < 4) {
			sendHelp(channel, null);
			return;
		}
		boolean primaryBracket = true;
		String roundNumArg = args[1];
		String role1Arg = args[2];
		String role2Arg = args[3];

		if (roundNumArg.length() <= 1) {
			reply(channel, alias, "Provide a bracket type and number (e.g: `a1` or `b1` for primary or secondary bracket)");
			return;
		}

		String roundNumArgFirstCharacter = roundNumArg.substring(0, 1);
		roundNumArg = roundNumArg.substring(1);

		BracketType bracket = BracketType.getBracket(roundNumArgFirstCharacter);
		if (bracket == null) {
			reply(channel, alias, "Invalid bracket type");
			return;
		}
		primaryBracket = bracket.isPrimary();

		int roundNum = -1;
		try {
			roundNum = Integer.parseInt(roundNumArg);
		} catch (NumberFormatException ex) {
			reply(channel, alias, "First argument needs to be a round number (1 and up)");
			return;
		}

		if (roundNum < 0) {
			reply(channel, alias, "Round number needs to be positive");
			return;
		}

		GNCUtil.TeamAndCaptain tac1;
		GNCUtil.TeamAndCaptain tac2;

		try {
			tac1 = GNCUtil.resolveTeamAndCaptain(applicationContext, role1Arg);
			tac2 = GNCUtil.resolveTeamAndCaptain(applicationContext, role2Arg);
		} catch (Exception e) {
			reply(channel, alias, "Failed to resolve team or captain argument:\n```\n%s\n```".formatted(e.getMessage()));
			return;
		}

		if (tac1 == null) {
			reply(channel, alias, "Failed to find first team or player");
			return;
		}
		if (tac2 == null) {
			reply(channel, alias, "Failed to find second team or player");
			return;
		}

		int roundNumFinal = roundNum;

		MatchChannelService matchChannelService = applicationContext.getBean(MatchChannelService.class);
		matchChannelService.createMatchChannel(primaryBracket, roundNum, tac1.teamRole(), tac2.teamRole(), tac1.captain().getMenoniMember().getUser().getId(), tac2.captain().getMenoniMember().getUser().getId()).whenComplete((matchChannel, error) -> {
			if (error != null) {
				reply(channel, "matchchannel", "Failed to create match channel for %s vs %s (round %s-%d):\n```%s```".formatted(
						tac1.display(), tac2.display(), bracket.name(), roundNumFinal, error.getMessage()
				));
				log.error("Failed to create match channel for %s vs %s (round %s-%d)".formatted(tac1.display(), tac2.display(), bracket.name(), roundNumFinal), error);
				return;
			}
			reply(channel, "matchchannel", "Created <#%s> for %s vs %s (round: %s-%d)".formatted(
					matchChannel.getId(),
					tac1.mention(),
					tac2.mention(),
					bracket.name(),
					roundNumFinal
			), m -> m.setAllowedMentions(List.of()));
		});
	}

	@Override
	public Collection<String> help() {
		return List.of(
				"!mc message <message-link> -- set pinned message contents",
				"!mc start <round-number> <@team-role-1/@captain-user-1> <@team-role-2/@captain-user-1> -- manually creates a match channel",
				"round number should be `a1` for primary bracket round 1, and `b1` for secondary bracket round 1"
		);
	}

}
