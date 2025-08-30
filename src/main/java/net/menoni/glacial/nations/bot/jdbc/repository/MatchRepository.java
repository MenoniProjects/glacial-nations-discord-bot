package net.menoni.glacial.nations.bot.jdbc.repository;

import net.menoni.glacial.nations.bot.jdbc.model.JdbcMatch;
import net.menoni.spring.commons.jdbc.AbstractTypeRepository;
import net.menoni.spring.commons.util.NullableMap;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class MatchRepository extends AbstractTypeRepository<JdbcMatch> {

	public JdbcMatch find(boolean primaryBracket, int roundNumber, String firstCaptainId, String secondCaptainId) {
		return this.queryOne(
				"SELECT id, primary_bracket, round_number, first_team_id, second_team_id, first_captain_id, second_captain_id, match_channel_id, pinned_message_id, win_index " +
						"FROM `match` WHERE primary_bracket = ? AND round_number = ? AND " +
				"(first_captain_id = ? AND second_captain_id = ?) OR (second_captain_id = ? AND first_captain_id = ?)",
				primaryBracket, roundNumber, firstCaptainId, secondCaptainId, firstCaptainId, secondCaptainId
		);
	}

	public JdbcMatch save(JdbcMatch match) {
		if (match.getId() == null) {
			GeneratedKeyHolder key = this.insertOne(
					"INSERT INTO `match` (primary_bracket, round_number, first_team_id, second_team_id, first_captain_id, second_captain_id, match_channel_id, pinned_message_id, win_index) VALUE " +
							"(:primaryBracket, :roundNumber, :firstTeamId, :secondTeamId, :firstCaptainId, :secondCaptainId, :matchChannelId, :pinnedMessageId, :winIndex)",
					match
			);
			if (key != null) {
				match.setId(key.getKey().longValue());
			}
		} else {
			this.update("UPDATE `match` SET first_team_id = :firstTeamId, second_team_id = :secondTeamId, " +
							"first_captain_id = :firstCaptainId, second_captain_id = :secondCaptainId, " +
							"match_channel_id = :matchChannelId, pinned_message_id = :pinnedMessageId, " +
							"win_index = :winIndex WHERE id = :id",
					NullableMap.create()
							.add("firstTeamId", match.getFirstTeamId())
							.add("secondTeamId", match.getSecondTeamId())
							.add("firstCaptainId", match.getFirstCaptainId())
							.add("secondCaptainId", match.getSecondCaptainId())
							.add("matchChannelId", match.getMatchChannelId())
							.add("pinnedMessageId", match.getPinnedMessageId())
							.add("winIndex", match.getWinIndex())
							.add("id", match.getId())
			);
		}
		return match;
	}

	public JdbcMatch findByChannel(String channelId) {
		return this.queryOne(
				"SELECT id, primary_bracket, round_number, first_team_id, second_team_id, first_captain_id, second_captain_id, match_channel_id, pinned_message_id, win_index FROM `match` WHERE match_channel_id = ?",
				channelId
		);
	}

	public JdbcMatch get(Long id) {
		return this.queryOne(
				"SELECT id, primary_bracket, round_number, first_team_id, second_team_id, first_captain_id, second_captain_id, match_channel_id, pinned_message_id, win_index FROM `match` WHERE id = ?",
				id
		);
	}
}
