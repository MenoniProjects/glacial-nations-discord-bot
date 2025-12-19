package net.menoni.glacial.nations.bot.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.menoni.commons.util.TemporaryValue;
import net.menoni.glacial.nations.bot.config.Constants;
import net.menoni.glacial.nations.bot.discord.DiscordBot;
import net.menoni.glacial.nations.bot.discord.emote.StandardEmoji;
import net.menoni.glacial.nations.bot.event.MatchCompletedEvent;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcMatch;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcMatchMap;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcTeam;
import net.menoni.glacial.nations.bot.menoni.model.GncMember;
import net.menoni.glacial.nations.bot.util.BracketType;
import net.menoni.jda.commons.util.JDAUtil;
import net.menoni.ws.discord.event.PickBanCompletedEvent;
import net.menoni.ws.discord.service.PickBanService;
import net.menoni.ws.discord.service.support.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MatchChannelService extends ListenerAdapter {

	private static final String BTN_WIN = "btn:win";
	private static final String BTN_PICKBAN = "btn:pickban";

	@Autowired
	private DiscordBot bot;

	@Autowired
	private TeamService teamService;
	@Autowired
	private MatchService matchService;
	@Autowired
	private MemberService memberService;
	@Autowired
	private MatchMapService matchMapService;
	@Autowired
	private PickBanService pickBanService;
	@Autowired
	private ResultService resultService;

	@Getter
	private final TemporaryValue<String> pinnedMessageText = TemporaryValue.empty(Duration.ofMinutes(10L));

	@PostConstruct
	public void _init() {
		this.bot.addEventListener(this);
	}

	public CompletableFuture<TextChannel> createMatchChannel(boolean primaryBracket, int roundNum, Role teamRole1, Role teamRole2, String captain1DiscordId, String captain2DiscordId) {
		String pinnedMessage = pinnedMessageText.getValue();
		if (pinnedMessage == null) {
			return CompletableFuture.failedFuture(new IllegalStateException("No pinned message set"));
		}
		CompletableFuture<TextChannel> future = new CompletableFuture<>();
		Category category = bot.applyGuild(g -> g.getCategoryById(bot.getConfig().getMatchesCategoryId(primaryBracket)), null);
		if (category == null) {
			future.completeExceptionally(new Exception("Match channel category not found"));
			return future;
		}

		GncMember captain1;
		GncMember captain2;

		try {
			captain1 = memberService.get(this.bot.getGuildId(), captain1DiscordId, true);
			captain2 = memberService.get(this.bot.getGuildId(), captain2DiscordId, true);
		} catch (Exception e) {
			return CompletableFuture.failedFuture(e);
		}


		Member cm1 = this.bot.getMemberById(captain1.getMenoniMember().getUser().getId());
		Member cm2 = this.bot.getMemberById(captain2.getMenoniMember().getUser().getId());

		ChannelAction<TextChannel> createAction = category
				.createTextChannel(factorChannelName(primaryBracket, roundNum, teamRole1, teamRole2, captain1.getMenoniMember().getEffectiveName(), captain2.getMenoniMember().getEffectiveName(), false))
				.addRolePermissionOverride(category.getGuild().getIdLong(), null, List.of(Permission.VIEW_CHANNEL)) // not public
				.addPermissionOverride(bot.getBotRole(), List.of(Permission.VIEW_CHANNEL), null);
		if (teamRole1 != null) {
			createAction = createAction.addPermissionOverride(teamRole1, List.of(Permission.VIEW_CHANNEL), null);
		} else {
			createAction = createAction.addPermissionOverride(cm1, List.of(Permission.VIEW_CHANNEL), null);
		}
		if (teamRole2 != null) {
			createAction = createAction.addPermissionOverride(teamRole2, List.of(Permission.VIEW_CHANNEL), null);
		} else {
			createAction = createAction.addPermissionOverride(cm2, List.of(Permission.VIEW_CHANNEL), null);
		}

		Role adminRole = bot.getRoleById(bot.getConfig().getAdminRoleId());
		Role staffRole = bot.getRoleById(bot.getConfig().getStaffRoleId());
		Role casterRole = bot.getRoleById(bot.getConfig().getCasterRoleId());

		createAction = applyRoleViewAccess(createAction, adminRole);
		createAction = applyRoleViewAccess(createAction, staffRole);
		createAction = applyRoleViewAccess(createAction, casterRole);

		int roundNumFinal = roundNum;

		JdbcTeam team1 = teamRole1 != null ? teamService.getTeamByRoleId(teamRole1.getId()) : null;
		JdbcTeam team2 = teamRole2 != null ? teamService.getTeamByRoleId(teamRole2.getId()) : null;

		Long team1Id = team1 != null ? team1.getId() : null;
		Long team2Id = team2 != null ? team2.getId() : null;

		JDAUtil.queueAndWaitConsume(
				createAction,
				channel -> {
					JDAUtil.queueAndWaitConsume(
							channel.sendMessage(factorPinnedMessageText(pinnedMessage)).setComponents(ActionRow.of(
									factorChannelActionRow(null)
							)),
							message -> {
								matchService.setMatchChannel(primaryBracket, roundNumFinal, team1Id, team2Id, captain1DiscordId, captain2DiscordId, channel.getId(), message.getId());
								JDAUtil.queueAndWait(message.pin());
								future.complete(channel);
							}, ex -> {
								matchService.setMatchChannel(primaryBracket, roundNumFinal, team1Id, team2Id, captain1DiscordId, captain2DiscordId, channel.getId(), null);
								future.completeExceptionally(ex);
							}
					);
				}, future::completeExceptionally
		);
		return future;
	}

	private void updateChannelName(TextChannel channel, JdbcMatch match) throws Exception {
		JdbcTeam team1 = teamService.getTeamById(match.getFirstTeamId());
		JdbcTeam team2 = teamService.getTeamById(match.getSecondTeamId());

		Role r1 = team1 != null ? this.bot.getRoleById(team1.getDiscordRoleId()) : null;
		Role r2 = team2 != null ? this.bot.getRoleById(team2.getDiscordRoleId()) : null;

		String captain1Name = memberService.get(this.bot.getGuildId(), match.getFirstCaptainId(), true).getMenoniMember().getEffectiveName();
		String captain2Name = memberService.get(this.bot.getGuildId(), match.getSecondCaptainId(), true).getMenoniMember().getEffectiveName();

		JDAUtil.queueAndWait(channel.getManager().setName(factorChannelName(
				match.getPrimaryBracket(),
				match.getRoundNumber(),
				r1,
				r2,
				captain1Name,
				captain2Name,
				match.getWinIndex() != null
		)));
	}

	private void updatePinnedMessage(JdbcMatch match) {
		TextChannel channel = this.bot.getTextChannelById(match.getMatchChannelId());
		if (channel == null) {
			return;
		}
		JDAUtil.queueAndWait(channel.editMessageComponentsById(
				match.getPinnedMessageId(),
				ActionRow.of(
						factorChannelActionRow(match)
				)
		));
	}

	private Collection<? extends ActionRowChildComponent> factorChannelActionRow(JdbcMatch match) {
		boolean picksBansCompleted = false;
		String winTeamName = null;
		if (match != null) {
			if (match.getWinIndex() != null) {
				JdbcTeam winTeam = teamService.getTeamById(match.getWinIndex() == 1 ? match.getFirstTeamId() : match.getSecondTeamId());
				if (winTeam != null) {
					winTeamName = winTeam.getName();
				} else {
					try {
						GncMember captain = memberService.get(this.bot.getGuildId(), match.getWinIndex() == 1 ? match.getFirstCaptainId() : match.getSecondCaptainId(), true);
						if (captain != null) {
							winTeamName = captain.getMenoniMember().getEffectiveName();
						} else {
							winTeamName = "Team: " + match.getWinIndex();
						}
					} catch (Exception e) {
						winTeamName = "Team: " + match.getWinIndex();
					}
				}
			}
			if (winTeamName == null) {
				List<JdbcMatchMap> matchMaps = matchMapService.getMatchMaps(match.getId());
				if (matchMaps != null && matchMaps.size() == 3) {
					picksBansCompleted = true;
				}
			}
		}
		if (winTeamName != null) {
			return List.of(Button.secondary("_noop", "Winner: %s".formatted(winTeamName)).asDisabled());
		} else if (picksBansCompleted) {
			return List.of(Button.success(BTN_WIN, "Win"));
		} else {
			return List.of(Button.primary(BTN_PICKBAN, "Pick/Ban"));
		}
	}

	public PickBanOrder constructPickBanOrder(PickBanUser pbu1, PickBanUser pbu2) {
		return PickBanOrder.builder(pbu1, pbu2)
				.maps(
						new PickBanMap("map1", "Green map (XLRB)"),
						new PickBanMap("map2", "Blue map (Thounej)"),
						new PickBanMap("map3", "Red map (Doogie)")
				)
				.player1(PickBanType.PICK)
				.player2(PickBanType.PICK)
				.includeRemainingMap(true)
				.build();
	}

	@EventListener
	public void onPickBanComplete(PickBanCompletedEvent event) {
		String identifier = event.session().getIdentifier();
		if (!identifier.startsWith("m-")) {
			log.warn("invalid match identified (no m-)");
			return;
		}
		identifier = identifier.substring(2);
		JdbcMatch m = null;
		try {
			Long id = Long.parseLong(identifier);
			m = this.matchService.getMatchById(id);
		} catch (NumberFormatException e) {
			log.warn("failed to parse number from {}", identifier);
			return;
		}

		if (m == null) {
			log.warn("match not found by id: {}", identifier);
			return;
		}

		List<PickBanResultMap> res = new ArrayList<>(event.session().getResult());
		List<JdbcMatchMap> matchMaps = new ArrayList<>();
		for (int i = 0; i < res.size(); i++) {
			PickBanResultMap r = res.get(i);
			matchMaps.add(new JdbcMatchMap(
					m.getId(),
					i,
					r.map().id(),
					r.map().name(),
					MatchMapType.fromPick(r.type()),
					r.pickedBy()
			));
		}
		log.info("Updating match maps");
		this.matchMapService.setMatchMaps(m.getId(), matchMaps);
		this.updatePinnedMessage(m);
	}

	@EventListener
	public void onMatchComplete(MatchCompletedEvent event) {
		String matchChannelId = event.match().getMatchChannelId();
		TextChannel channel = this.bot.getTextChannelById(matchChannelId);
		if (channel == null) {
			return;
		}
		try {
			this.updateChannelName(channel, event.match());
		} catch (Exception e) {
			log.error("failed to update channel", e);
		}
	}

	private String factorPinnedMessageText(String pinnedMessageContent) {
		String r = pinnedMessageContent;
		if (!r.isBlank()) {
			r += "\n-# -----\n";
		}
		r += "1. Use **Pick/Ban** button\n";
		r += "2. Captain of winner team use **Win** button\n";
		return r;
	}

	private String factorChannelName(boolean primaryBracket, int roundNum, Role teamRole1, Role teamRole2, String captain1Name, String captain2Name, boolean completed) {
		String t1 = (teamRole1 != null ? teamRole1.getName() : captain1Name).toLowerCase().replaceAll(Pattern.quote(" "), "-");
		String t2 = (teamRole2 != null ? teamRole2.getName() : captain2Name).toLowerCase().replaceAll(Pattern.quote(" "), "-");
		String extra = "";
		if (completed) {
			extra = StandardEmoji.WHITE_CHECK_MARK.print() + Constants.CHANNEL_EMOTE_DIVIDER;
		}
		return "%s%s%d-%s-%s".formatted(extra, BracketType.getBracket(primaryBracket).getCharacter(), roundNum, t1, t2);
	}

	private ChannelAction<TextChannel> applyRoleViewAccess(ChannelAction<TextChannel> action, Role role) {
		if (role != null) {
			return action.addPermissionOverride(role, List.of(Permission.VIEW_CHANNEL), null);
		}
		return action;
	}

	@Override
	public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
		try {
			if (event.getComponentId().equals(BTN_WIN)) {
				this._onWinButton(event);
			} else if (event.getComponentId().equals(BTN_PICKBAN)) {
				this._onPickBanButton(event);
			}
		} catch (Exception e) {
			log.error("Failed to handle event", e);
		}
	}

	private void _onWinButton(ButtonInteractionEvent event) throws Exception {
		String channelId = event.getChannel().getId();
		JdbcMatch match = matchService.getMatchForChannel(channelId);
		if (match == null) {
			JDAUtil.queueAndWait(event.reply("Match not found for the current channel"));
			return;
		}
		if (match.getWinIndex() != null) {
			JDAUtil.queueAndWait(event.reply("Match result already submitted - ask an admin to override"));
			return;
		}

		String memberId = event.getUser().getId();
		GncMember gncMember = this.memberService.get(this.bot.getGuildId(), memberId, false);
		if (gncMember == null) {
			JDAUtil.queueAndWait(event.reply("Could not load your user data"));
			return;
		}
		if (!Objects.equals(gncMember.getMenoniMember().getUser().getId(), match.getFirstCaptainId()) && !Objects.equals(gncMember.getMenoniMember().getUser().getId(), match.getSecondCaptainId())) {
			if (gncMember.getTeamMember() != null) {
				if (!gncMember.getTeamMember().getCaptain()) {
					JDAUtil.queueAndWait(event.reply("<@%s> tried to submit a win but is not a team captain".formatted(gncMember.getMenoniMember().getUser().getId())));
					return;
				}
			}
		}

		int winIndex = Objects.equals(gncMember.getMenoniMember().getUser().getId(), match.getFirstCaptainId()) ? 1 : 2;
		matchService.setMatchWinner(match, winIndex);

		JdbcTeam winTeam = teamService.getTeamById(winIndex == 1 ? match.getFirstTeamId() : match.getSecondTeamId());
		JdbcTeam loseTeam = teamService.getTeamById(winIndex == 2 ? match.getFirstTeamId() : match.getSecondTeamId());

		if (winTeam != null && loseTeam != null) {
			resultService.sendResultsMessage(match.getPrimaryBracket(), match.getRoundNumber(), winTeam, loseTeam).whenComplete((msg, throwable) -> {
				if (throwable != null) {
					log.error("Error executing win-button for %s in %s-vs-%s".formatted(
							event.getUser().getEffectiveName(),
							winTeam.getName(),
							loseTeam.getName()
					), throwable);
					event.getChannel().sendMessage("Failed to mark match as won, contact a staff").queue();
				}
			});
		}

		JDAUtil.queueAndWait(event.reply("<@%s> marked this match as won by: %s".formatted(
				event.getUser().getId(),
				winTeam != null ? winTeam.getName() : event.getUser().getEffectiveName()
		)).setAllowedMentions(List.of()));
		this.updatePinnedMessage(match);
	}

	private void _onPickBanButton(ButtonInteractionEvent event) {
		String channelId = event.getChannel().getId();
		JdbcMatch match = matchService.getMatchForChannel(channelId);
		if (match == null) {
			JDAUtil.queueAndWait(event.reply("Match not found for the current channel"));
			return;
		}

		JdbcTeam team1 = teamService.getTeamById(match.getFirstTeamId());
		JdbcTeam team2 = teamService.getTeamById(match.getSecondTeamId());

		Member m1 = this.bot.getMemberById(match.getFirstCaptainId());
		Member m2 = this.bot.getMemberById(match.getSecondCaptainId());

		PickBanUser u1 = PickBanUser.of(m1, team1 != null ? team1.getName() : null);
		PickBanUser u2 = PickBanUser.of(m2, team2 != null ? team2.getName() : null);

		JDAUtil.queueAndWait(event.deferEdit());

		this.pickBanService.startPickBan(
				event.getChannel().asGuildMessageChannel(),
				"m-%d".formatted(match.getId()),
				this.constructPickBanOrder(u1, u2)
		);
	}

}
