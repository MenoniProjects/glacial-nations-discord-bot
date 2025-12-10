package net.menoni.glacial.nations.bot.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.menoni.glacial.nations.bot.discord.DiscordBot;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcTeam;
import net.menoni.glacial.nations.bot.service.model.GSheetFormat;
import net.menoni.glacial.nations.bot.service.model.GSheetTab;
import net.menoni.glacial.nations.bot.util.BracketType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ResultService implements EventListener {

	@Autowired
	private DiscordBot bot;

	@Autowired
	private GSheetService sheetService;

	@PostConstruct
	public void init() {
		bot.addEventListener(this);
	}

	@Override
	public void onEvent(@NotNull GenericEvent event) {
		if (event instanceof ButtonInteractionEvent buttonInteractionEvent) {
			this.onButtonInteract(buttonInteractionEvent);
		}
	}

	public CompletableFuture<Message> sendResultsMessage(boolean primaryBracket, Integer round, JdbcTeam winTeam, JdbcTeam loseTeam) {
		CompletableFuture<Message> future = new CompletableFuture<>();

		TextChannel resultsChannel = bot.getTextChannelById(bot.getConfig().getResultsChannelId());
		if (resultsChannel == null) {
			log.warn("Not sending #results message as the channel could not be found");
			future.complete(null);
			return future;
		}

		BracketType bracket = BracketType.getBracket(primaryBracket);

		EmbedBuilder embed = new EmbedBuilder();
		setMatchNameTitle(embed, bracket, round);
		embed.setDescription("%s <@&%s> wins against %s <@&%s>".formatted(
				winTeam.getCountryFlagEmote(),
				winTeam.getDiscordRoleId(),
				loseTeam.getCountryFlagEmote(),
				loseTeam.getDiscordRoleId()
		));
		embed.setColor(bracket.getColor());

		resultsChannel.sendMessageEmbeds(embed.build()).setComponents(ActionRow.of(Button.of(
				ButtonStyle.DANGER,
				"process",
				"Not processed"
		))).queue(future::complete, future::completeExceptionally);

		return future;
	}

	private void setMatchNameTitle(EmbedBuilder embed, BracketType bracket, int round) {
		if (round == 99) {
			embed.setTitle("Grand Final", sheetService.getSheetUrl(GSheetTab.PLAY_INS, GSheetFormat.PUBLIC));
		} else if (bracket.isPrimary() && round == 0) {
			embed.setTitle("Play-ins Match", sheetService.getSheetUrl(GSheetTab.PLAY_INS, GSheetFormat.PUBLIC));
		} else {
			embed.setTitle("%s Bracket - Round #%d".formatted(bracket.getDisplayName(), round), sheetService.getSheetUrl(bracket.isPrimary() ? GSheetTab.BRACKET_PRIMARY : GSheetTab.BRACKET_SECONDARY, GSheetFormat.PUBLIC));
		}
	}

	private void onButtonInteract(ButtonInteractionEvent event) {
		if (!Objects.equals(event.getChannelId(), bot.getConfig().getResultsChannelId())) {
			return;
		}
		if (!event.getComponentId().equals("process")) {
			return;
		}
		event.getInteraction().deferEdit().queue();
		if (event.getMember() == null || !event.getMember().hasPermission(Permission.MANAGE_ROLES)) {
			return;
		}
		event.getMessage().editMessage(event.getMessage().getContentRaw()).setComponents().queue();
	}

}
