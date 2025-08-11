package net.menoni.glacial.nations.bot.discord;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.menoni.commons.util.LoggerTextFormat;
import net.menoni.glacial.nations.bot.menoni.model.GncMember;
import net.menoni.glacial.nations.bot.service.MemberService;
import net.menoni.glacial.nations.bot.service.TeamService;
import net.menoni.jda.commons.discord.AbstractDiscordBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DiscordBot extends AbstractDiscordBot<DiscordBotConfig> {
	private final Logger logger = LoggerFactory.getLogger(DiscordBot.class);

	private static final Set<Permission> PERMISSIONS = Set.of(
			Permission.MANAGE_ROLES,
			Permission.MANAGE_CHANNEL
	);

	private static final List<GatewayIntent> INTENTS = List.of(
			GatewayIntent.GUILD_MEMBERS,
			GatewayIntent.GUILD_MESSAGES,
			GatewayIntent.MESSAGE_CONTENT
	);

	private static final List<CacheFlag> ENABLED_CACHES = List.of();
	private static final List<CacheFlag> DISABLED_CACHES = List.of(
			CacheFlag.VOICE_STATE,
			CacheFlag.EMOJI,
			CacheFlag.STICKER,
			CacheFlag.CLIENT_STATUS,
			CacheFlag.SCHEDULED_EVENTS,
			CacheFlag.ACTIVITY,
			CacheFlag.ONLINE_STATUS
	);
	private static final List<String> REQUIRED_SCOPES = List.of("applications.commands");

	public DiscordBot(
			DiscordBotConfig config,
			AutowireCapableBeanFactory autowireCapableBeanFactory,
			boolean testMode
	) throws InterruptedException {
		super(
				"GNC",
				config,
				autowireCapableBeanFactory,
				testMode,
				PERMISSIONS,
				INTENTS,
				ENABLED_CACHES,
				DISABLED_CACHES,
				REQUIRED_SCOPES
		);
	}

	public Role getMemberRole() {
		return getRoleById(this.getConfig().getMemberRoleId());
	}

	public Role getPlayerRole() {
		return getRoleById(this.getConfig().getPlayerRoleId());
	}

	public Role getTeamLeadRole() {
		return getRoleById(this.getConfig().getTeamLeadRoleId());
	}

	public TextChannel getTeamsChannel() {
		return getTextChannelById(this.getConfig().getTeamsChannelId());
	}

	@Override
	protected void onReady() {
		new Thread(() -> {
			try {
				Thread.sleep(1000L);
				MemberService memberService = getAutowireCapableBeanFactory().getBean(MemberService.class);
				this.withGuild(g -> g.loadMembers().onSuccess(members -> {
					List<GncMember> botMembers = memberService.listMembers(getGuildId());
					Role memberRole = getMemberRole();
					Role playerRole = getPlayerRole();
					Role teamLeadRole = getTeamLeadRole();
					for (Member member : members) {
						GncMember foundMember = botMembers.stream().filter(b -> Objects.equals(b.getMember().getUser().getId(), member.getId())).findAny().orElse(null);
						if (foundMember != null) {
							this.ensurePlayerRole(member, foundMember, memberRole, playerRole, teamLeadRole);
						}
					}
				}));
			} catch (InterruptedException e) {
				logger.error("Await failed", e);
			}
		}).start();
	}

	public void ensurePlayerRole(Member discordMember, GncMember botMember, Role memberRole, Role playerRole, Role teamLeadRole) {
		getAutowireCapableBeanFactory().getBean(TeamService.class).ensurePlayerRoles(discordMember, botMember, memberRole, playerRole, teamLeadRole);
	}

	public void logAdminChannel(String text, Object... args) {
		this.withGuild(g -> {
			String txt = LoggerTextFormat.fillArgs(text, args);
			if (this.getConfig().getAdminChannelId() != null) {
				TextChannel tc = g.getTextChannelById(this.getConfig().getAdminChannelId());
				if (tc != null) {
					tc.sendMessage(txt).queue();
					return;
				}
			}

			logger.warn("[missing admin channel] {}", txt);
		});
	}

}
