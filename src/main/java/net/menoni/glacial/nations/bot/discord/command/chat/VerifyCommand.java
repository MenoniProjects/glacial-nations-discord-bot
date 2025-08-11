package net.menoni.glacial.nations.bot.discord.command.chat;

import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.menoni.glacial.nations.bot.service.GSheetService;
import net.menoni.glacial.nations.bot.util.BracketType;
import net.menoni.jda.commons.discord.chatcommand.ChatCommand;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Slf4j
public class VerifyCommand implements ChatCommand {
	@Override
	public Collection<String> names() {
		return List.of("verify");
	}

	@Override
	public Collection<Permission> requiredPermissions() {
		return List.of(Permission.MANAGE_ROLES);
	}

	@Override
	public String shortHelpText() {
		return "Verify a bracket";
	}

	@Override
	public boolean canExecute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, boolean silent) {
		return true;
	}

	@Override
	public boolean execute(ApplicationContext applicationContext, GuildMessageChannelUnion channel, Member member, Message message, String alias, String[] args) {
		if (args.length < 1) {
			sendHelp(channel, "Missing bracket identifier");
			return true;
		}
		BracketType bracket = BracketType.getBracket(args[0]);
		if (bracket == null) {
			sendHelp(channel, "Invalid bracket type: " + args[0]);
			return true;
		}

		GSheetService sheetService = applicationContext.getBean(GSheetService.class);
		try {
			List<String> verifyResult = sheetService.verifySheet(bracket.isPrimary());
			reply(channel, alias, "%s Bracket\n%s".formatted(
					bracket.getDisplayName(),
					String.join("\n", verifyResult)
			));
		} catch (IOException | CsvException e) {
			log.error("Failed to verify sheet for bracket (%s)".formatted(bracket), e);
			reply(channel, alias, "Failed to verify sheet for bracket (%s): %s".formatted(bracket, e.getMessage()));
		}
		return true;
	}

	@Override
	public Collection<String> help() {
		return List.of(
				"!verify a -- verify primary bracket",
				"!verify b -- verify secondary bracket"
		);
	}
}
