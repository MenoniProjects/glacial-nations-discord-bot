package net.menoni.glacial.bot.discord.listener.chatcmd.impl;

import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.menoni.glacial.bot.discord.listener.ChatCommandListener;
import net.menoni.glacial.bot.discord.listener.chatcmd.ChatCommand;
import net.menoni.glacial.bot.service.GSheetService;
import net.menoni.glacial.bot.util.BracketType;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Slf4j
public class StartRoundCommand implements ChatCommand {
	@Override
	public Collection<String> names() {
		return List.of("startround");
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
		if (args.length <= 0) {
			sendHelp(channel, "Not enough arguments");
			return;
		}

		boolean primaryBracket = true;
		String roundNumArg = args[0];

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

		GSheetService sheetService = applicationContext.getBean(GSheetService.class);
		try {
			List<String> matchesForRoundResult = sheetService.createMatchChannelsForRound(primaryBracket, roundNum);
			reply(channel, alias, "%s - round %d\n%s".formatted(
					primaryBracket ? "primary" : "secondary",
					roundNum,
					String.join("\n", matchesForRoundResult)
			));
		} catch (IOException | CsvException e) {
			reply(channel, alias, "Reading GNC sheet failed: " + e.getMessage());
			log.error("Failed reading GNC sheet", e);
		}
	}

	@Override
	public Collection<String> help() {
		return List.of(
				"!startround <round-number> -- starts a next round, creating all channels that can be made"
		);
	}
}
