package net.menoni.glacial.nations.bot.util;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.menoni.glacial.nations.bot.discord.DiscordBot;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcTeam;
import net.menoni.glacial.nations.bot.menoni.model.GncMember;
import net.menoni.glacial.nations.bot.service.MemberService;
import net.menoni.glacial.nations.bot.service.TeamService;
import net.menoni.jda.commons.util.DiscordArgUtil;
import org.springframework.context.ApplicationContext;

public class GNCUtil {

	public static String getTeamCaptainId(MemberService memberService, String guildId, JdbcTeam team) throws Exception {
		if (team == null) {
			return null;
		}
		GncMember captain = memberService.getTeamCaptain(guildId, team.getId());
		if (captain == null) {
			return null;
		}
		return captain.getMenoniMember().getUser().getId();
	}

	public static record TeamAndCaptain(
			Role teamRole,
			JdbcTeam team,
			GncMember captain
	) {
		public String display() {
			if (this.team != null) {
				return this.team.getName();
			}
			return this.captain.getMenoniMember().getEffectiveName();
		}

		public String mention() {
			if (this.team != null) {
				return "<@&%s>".formatted(this.team.getDiscordRoleId());
			}
			return "<@%s>".formatted(this.captain.getMenoniMember().getUser().getId());
		}
	}

	public static TeamAndCaptain resolveTeamAndCaptain(ApplicationContext applicationContext, String arg) throws Exception {
		if (!DiscordArgUtil.isRole(arg) && !DiscordArgUtil.isUser(arg)) {
			return null;
		}
		DiscordBot bot = applicationContext.getBean(DiscordBot.class);
		MemberService memberService = applicationContext.getBean(MemberService.class);
		arg = DiscordArgUtil.getNudeId(arg);
		Role role = bot.getRoleById(arg);
		if (role != null) {
			TeamService teamService = applicationContext.getBean(TeamService.class);
			JdbcTeam team = teamService.getTeamByRoleId(role.getId());
			if (team == null) {
				return null;
			}
			GncMember captain = memberService.getTeamCaptain(bot.getGuildId(), team.getId());
			if (captain == null) {
				return null;
			}
			return new TeamAndCaptain(
					role,
					team,
					captain
			);
		}
		Member member = bot.getMemberById(arg);
		if (member == null) {
			return null;
		}
		GncMember captain = memberService.get(bot.getGuildId(), member.getId(), true);
		if (captain == null) {
			return null;
		}
		return new TeamAndCaptain(
				null,
				null,
				captain
		);
	}

}
