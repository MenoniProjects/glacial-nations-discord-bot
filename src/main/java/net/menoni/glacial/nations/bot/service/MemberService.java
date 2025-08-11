package net.menoni.glacial.nations.bot.service;

import lombok.extern.slf4j.Slf4j;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcTeamMember;
import net.menoni.glacial.nations.bot.jdbc.repository.TeamMemberRepository;
import net.menoni.glacial.nations.bot.menoni.model.GncMember;
import net.menoni.ws.client.MenoniWsClient;
import net.menoni.ws.common.model.discord.WsDiscordMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class MemberService {

	@Autowired
	private MenoniWsClient ws;
	@Autowired
	private TeamMemberRepository teamMemberRepository;

	public GncMember get(String guildId, String userId) {
		try {
			WsDiscordMember discordMember = this.ws.getDiscordDomain().getMemberService().getMemberById(guildId, userId);
			if (discordMember == null) {
				return null;
			}
			JdbcTeamMember teamMember = this.teamMemberRepository.getById(userId);
			return new GncMember(discordMember, teamMember);
		} catch (Exception e) {
			log.warn("Failed to load member: {}/{}", guildId, userId);
			return null;
		}
	}

	public List<GncMember> listMembers(String guildId) {
		try {
			List<WsDiscordMember> guildMembers = this.ws.getDiscordDomain().getMemberService().getGuildMembers(guildId);
			List<JdbcTeamMember> teamMembers = this.teamMemberRepository.listMembers();
			List<GncMember> res = new ArrayList<>();
			for (WsDiscordMember guildMember : guildMembers) {
				JdbcTeamMember teamMember = teamMembers.stream().filter(tm -> Objects.equals(tm.getDiscordId(), guildMember.getUser().getId())).findAny().orElse(null);
				res.add(new GncMember(guildMember, teamMember));
			}
			return res;
		} catch (Exception e) {
			log.warn("Failed to load members: {}", guildId);
			return null;
		}
	}

	public void deleteTeamMembership(String userId) {
		this.teamMemberRepository.deleteMember(userId);
	}

	public void saveTeamMembership(JdbcTeamMember teamMember) {
		this.teamMemberRepository.saveMember(teamMember);
	}

}
