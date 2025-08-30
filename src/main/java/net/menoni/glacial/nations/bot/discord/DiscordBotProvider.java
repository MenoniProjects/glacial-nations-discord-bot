package net.menoni.glacial.nations.bot.discord;

import net.menoni.glacial.nations.bot.discord.command.ChatCommandSupport;
import net.menoni.glacial.nations.bot.discord.command.chat.*;
import net.menoni.glacial.nations.bot.discord.command.impl.ImportSignupsCommandHandler;
import net.menoni.glacial.nations.bot.discord.command.impl.ParseMatchDumpCommandHandler;
import net.menoni.glacial.nations.bot.script.ScriptExecutor;
import net.menoni.jda.commons.discord.chatcommand.ChatCommandListener;
import net.menoni.jda.commons.discord.command.DiscordBotCommandHandler;
import net.menoni.ws.discord.command.chat.DiscordUserCommand;
import net.menoni.ws.discord.command.chat.NadeoMapCommand;
import net.menoni.ws.discord.command.chat.NadeoPlayerCommand;
import net.menoni.ws.discord.command.chat.TmxCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class DiscordBotProvider {

	@Autowired
	private DiscordBotConfig config;
	@Autowired
	private ScriptExecutor scriptExecutor;

	@Bean
	@ConditionalOnProperty(value = "jda.bot.startup-test", havingValue = "false")
	public DiscordBot discordBot(AutowireCapableBeanFactory beanFactory) throws InterruptedException {
		boolean testMode = false;
		if (scriptExecutor.getScript() != null) {
			testMode = true;
		}
		return new DiscordBot(this.config, beanFactory, testMode);
	}

	@Bean
	@ConditionalOnProperty(value = "jda.bot.startup-test", havingValue = "true")
	public DiscordBot discordBotOffline(AutowireCapableBeanFactory beanFactory) throws InterruptedException {
		return new DiscordBot(this.config, beanFactory, true);
	}

	@Bean
	public DiscordBotCommandHandler<DiscordBot> discordBotCommandHandler(DiscordBot bot) {
		return new DiscordBotCommandHandler<>(bot, Set.of(
				new ImportSignupsCommandHandler(bot),
				new ParseMatchDumpCommandHandler(bot)
		));
	}

	@Bean
	public ChatCommandListener chatCommandListener(
			DiscordBot bot,
			ApplicationContext applicationContext
	) {
		return new ChatCommandListener(
				bot,
				applicationContext,
				List.of(
						// jda commands
						new DiscordUserCommand(ChatCommandSupport.REQUIRE_BOT_CHANNEL_AND_MASTER_ADMIN),
						new NadeoMapCommand(
								ChatCommandSupport.REQUIRE_BOT_CHANNEL_AND_MASTER_ADMIN,
								null, null, null, null
//                                CustomEmote.MEDAL_AUTHOR,
//                                CustomEmote.MEDAL_GOLD,
//                                CustomEmote.MEDAL_SILVER,
//                                CustomEmote.MEDAL_BRONZE
						),
						new NadeoPlayerCommand(ChatCommandSupport.REQUIRE_BOT_CHANNEL_AND_MASTER_ADMIN),
						new TmxCommand(ChatCommandSupport.REQUIRE_BOT_CHANNEL_AND_MASTER_ADMIN),
						// gnc commands
						new DebugCommand(),
						new EventsExportCommand(),
						new ForceWinCommand(),
						new MatchChannelCommand(),
						new MissingPlayersCommand(),
						new RefreshTeamsCommand(),
						new StartRoundCommand(),
						new VerifyCommand()
//						new WinCommand()
				)
		);
	}

}
