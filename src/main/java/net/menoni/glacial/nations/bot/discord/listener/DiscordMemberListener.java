package net.menoni.glacial.nations.bot.discord.listener;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.menoni.glacial.nations.bot.discord.DiscordBot;
import net.menoni.glacial.nations.bot.menoni.model.GncMember;
import net.menoni.glacial.nations.bot.service.MemberService;
import net.menoni.glacial.nations.bot.service.TeamService;
import net.menoni.ws.discord.event.MenoniMemberJoinEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DiscordMemberListener {

	@Autowired
	private MemberService memberService;
	@Autowired
	private DiscordBot bot;
	@Autowired
	private TeamService teamService;

	@EventListener
	public void onMemberJoin(MenoniMemberJoinEvent event) throws Exception {
		Role memberRole = bot.getMemberRole();
		Role playerRole = bot.getPlayerRole();
		Role teamLeadRole = bot.getTeamLeadRole();
		log.info("Ensuring new member roles (join): {}/{}", event.menoniMember().getUser().getId(), event.menoniMember().getEffectiveName());
		GncMember gncMember = memberService.get(event.discordMember().getGuild().getId(), event.discordMember().getId(), true);
		teamService.ensurePlayerRoles(event.discordMember(), gncMember, memberRole, playerRole, teamLeadRole);
	}


}
