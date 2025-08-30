package net.menoni.glacial.nations.bot.jdbc.repository;

import net.menoni.glacial.nations.bot.jdbc.model.JdbcMatchMap;
import net.menoni.spring.commons.jdbc.AbstractTypeRepository;
import net.menoni.spring.commons.util.NullableMap;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class MatchMapRepository extends AbstractTypeRepository<JdbcMatchMap> {

	public void deleteForMatch(Long matchId) {
		this.update("DELETE FROM match_map WHERE match_id = :matchId", Map.of("matchId", matchId));
	}

	public void add(List<JdbcMatchMap> maps) {
		this.insertBatch(
				"INSERT INTO match_map (match_id, map_index, map_uid, map_name, type, type_data) VALUES (:matchId, :mapIndex, :mapUid, :mapName, :type, :typeData)",
				maps,
				m -> NullableMap.create()
						.add("matchId", m.getMatchId())
						.add("mapIndex", m.getMapIndex())
						.add("mapUid", m.getMapUid())
						.add("mapName", m.getMapName())
						.add("type", m.getType().name())
						.add("typeData", m.getTypeData())
		);
	}

	public List<JdbcMatchMap> getForMatch(Long matchId) {
		return this.queryMany(
				"SELECT match_id, map_index, map_uid, map_name, type, type_data FROM match_map WHERE match_id = ? ORDER BY map_index",
				matchId
		);
	}
}
