package net.menoni.glacial.nations.bot.service;

import lombok.extern.slf4j.Slf4j;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcTeamMember;
import net.menoni.glacial.nations.bot.jdbc.repository.TeamMemberRepository;
import net.menoni.glacial.nations.bot.menoni.model.GncMember;
import net.menoni.ws.client.MenoniWsClient;
import net.menoni.ws.common.model.discord.WsDiscordMember;
import net.menoni.ws.discord.service.base.AbstractServerMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class MemberService extends AbstractServerMemberService<GncMember> {

	@Autowired
	private TeamMemberRepository teamMemberRepository;

	@Override
	protected GncMember _loadData(WsDiscordMember member) throws Exception {
		JdbcTeamMember teamMember = this.teamMemberRepository.getById(member.getUser().getId());
		return new GncMember(member, teamMember);
	}

	@Override
	protected List<GncMember> _loadData(List<WsDiscordMember> member) throws Exception {
		List<JdbcTeamMember> teamMembers = this.teamMemberRepository.listMembers();
		return new ArrayList<>(member.stream().map(m -> {
			JdbcTeamMember teamMember = teamMembers.stream().filter(tm -> Objects.equals(tm.getDiscordId(), m.getUser().getId())).findAny().orElse(null);
			return new GncMember(m, teamMember);
		}).toList());
	}

	@Override
	protected void _saveData(GncMember serverMember) throws Exception {
		JdbcTeamMember teamMember = serverMember.getTeamMember();
		if (teamMember != null) {
			this.teamMemberRepository.saveMember(teamMember);
		} else {
			this.teamMemberRepository.deleteMember(serverMember.getMenoniMember().getUser().getId());
		}
	}

	public void deleteTeamMembership(String userId) {
		this.teamMemberRepository.deleteMember(userId);
	}

	public void saveTeamMembership(JdbcTeamMember teamMember) {
		this.teamMemberRepository.saveMember(teamMember);
	}

	public GncMember getTeamCaptain(String guildId, Long teamId) throws Exception {
		JdbcTeamMember captain = teamMemberRepository.getTeamCaptain(teamId);
		if (captain == null) {
			return null;
		}
		return this.get(guildId, captain.getDiscordId(), false);
	}
}
