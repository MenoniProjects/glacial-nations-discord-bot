package net.menoni.glacial.nations.bot.jdbc.repository;

import net.menoni.spring.commons.jdbc.AbstractTypeRepository;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcTeamMember;
import net.menoni.spring.commons.util.NullableMap;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Repository
public class TeamMemberRepository extends AbstractTypeRepository<JdbcTeamMember> {

    public List<JdbcTeamMember> getMembersByTeam(Long teamId) {
        return this.queryMany("SELECT discord_id, team_id FROM team_member WHERE team_id = ?", teamId);
    }

    public JdbcTeamMember getById(String discordId) {
        return this.queryOne(
                "SELECT discord_id, team_id FROM team_member WHERE discord_id = ?",
                discordId
        );
    }

    public boolean deleteMember(String discordId) {
        return this.update(
                "DELETE FROM team_member WHERE discord_id = :discordId",
                Map.of("discordId", discordId)
        ) > 0;
    }

    public JdbcTeamMember saveMember(JdbcTeamMember member) {
        this.update(
                "INSERT INTO team_member (discord_id, team_id) VALUES (:discordId, :teamId) ON DUPLICATE KEY UPDATE team_id = :teamId",
                Map.of(
                        "discordId", member.getDiscordId(),
                        "teamId", member.getTeamId()
                )
        );
        return member;
    }

    public List<JdbcTeamMember> listMembers() {
        return this.queryMany(
                "SELECT discord_id, team_id FROM team_member"
        );
    }
}
