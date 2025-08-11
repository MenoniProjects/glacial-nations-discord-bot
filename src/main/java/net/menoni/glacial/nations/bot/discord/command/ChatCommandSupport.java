package net.menoni.glacial.nations.bot.discord.command;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.menoni.glacial.nations.bot.config.Constants;
import net.menoni.glacial.nations.bot.discord.DiscordBot;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcMatch;
import net.menoni.glacial.nations.bot.service.MatchService;
import net.menoni.ws.discord.command.ChatCommandRequirement;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Objects;

public class ChatCommandSupport {

	public static final ChatCommandRequirement REQUIRE_BOT_CHANNEL = ChatCommandRequirement.createRequireChannelDynamic(applicationContext -> {
		DiscordBot bot = applicationContext.getBean(DiscordBot.class);
		if (bot.getConfig().getCmdChannelId() == null) {
			return null;
		}
		return new ChatCommandRequirement.RequiredChannel(bot.getConfig().getCmdChannelId(), "bot-cmd");
	});

	public static final ChatCommandRequirement REQUIRE_BOT_CHANNEL_AND_MASTER_ADMIN = new ChatCommandRequirement(new ChatCommandRequirement.Requirement() {
		@Override
		public boolean canExecute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, boolean silent) {
			if (!REQUIRE_BOT_CHANNEL.check(applicationContext, channel, member, silent)) {
				return false;
			}
			if (!member.getId().equals(Constants.USER_ID_DEV)) {
				if (!silent) {
					channel.sendMessage("This command can only be executed by <@%s>".formatted(Constants.USER_ID_DEV)).setAllowedMentions(List.of()).queue();
				}
				return false;
			}
			return true;
		}
	});

	public static boolean requireBotCmdChannel(ApplicationContext applicationContext, GuildMessageChannelUnion channel, boolean silent) {
		DiscordBot bot = applicationContext.getBean(DiscordBot.class);
		boolean allow = Objects.equals(channel.getId(), bot.getConfig().getCmdChannelId());
		if (!allow) {
			if (!silent) {
				channel.sendMessage("This command can only be executed in the bot-cmd channel").queue();
			}
		}
		return allow;
	}

	public static boolean requireBotCmdChannelOrMatchChannel(ApplicationContext applicationContext, GuildMessageChannelUnion channel) {
		DiscordBot bot = applicationContext.getBean(DiscordBot.class);
		boolean allow = Objects.equals(channel.getId(), bot.getConfig().getCmdChannelId());
		if (allow) {
			return true;
		}

		MatchService matchService = applicationContext.getBean(MatchService.class);
		JdbcMatch matchForChannel = matchService.getMatchForChannel(channel.getId());
		if (matchForChannel != null) {
			return true;
		}

		channel.sendMessage("This command can only be executed in the bot-cmd channel or in match channels").queue();
		return false;
	}

}
