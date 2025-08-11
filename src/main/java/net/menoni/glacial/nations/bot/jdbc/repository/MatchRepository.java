package net.menoni.glacial.nations.bot.jdbc.repository;

import net.menoni.glacial.nations.bot.jdbc.model.JdbcMatch;
import net.menoni.spring.commons.jdbc.AbstractTypeRepository;
import net.menoni.spring.commons.util.NullableMap;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class MatchRepository extends AbstractTypeRepository<JdbcMatch> {

	public JdbcMatch find(boolean primaryBracket, int roundNumber, Long firstTeamId, Long secondTeamId) {
		return this.queryOne(
				"SELECT id, primary_bracket, round_number, first_team_id, second_team_id, match_channel_id, win_team_id FROM `match` WHERE primary_bracket = ? AND round_number = ? AND " +
				"(first_team_id = ? AND second_team_id = ?) OR (second_team_id = ? AND first_team_id = ?)",
				primaryBracket, roundNumber, firstTeamId, secondTeamId, firstTeamId, secondTeamId
		);
	}

	public JdbcMatch save(JdbcMatch match) {
		if (match.getId() == null) {
			GeneratedKeyHolder key = this.insertOne(
					"INSERT INTO `match` (primary_bracket, round_number, first_team_id, second_team_id, match_channel_id, win_team_id) VALUE " +
							"(:primaryBracket, :roundNumber, :firstTeamId, :secondTeamId, :matchChannelId, :winTeamId)",
					match
			);
			if (key != null) {
				match.setId(key.getKey().longValue());
			}
		} else {
			this.update("UPDATE `match` SET first_team_id = :firstTeamId, second_team_id = :secondTeamId, match_channel_id = :matchChannelId, win_team_id = :winTeamId WHERE id = :id", NullableMap.of(
					"firstTeamId", match.getFirstTeamId(),
					"secondTeamId", match.getSecondTeamId(),
					"matchChannelId", match.getMatchChannelId(),
					"winTeamId", match.getWinTeamId(),
					"id", match.getId()
			));
		}
		return match;
	}

	public JdbcMatch findByChannel(String channelId) {
		return this.queryOne(
				"SELECT id, primary_bracket, round_number, first_team_id, second_team_id, match_channel_id, win_team_id FROM `match` WHERE match_channel_id = ?",
				channelId
		);
	}
}
