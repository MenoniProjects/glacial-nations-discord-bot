package net.menoni.glacial.nations.bot.service;

import net.menoni.glacial.nations.bot.jdbc.model.JdbcMatchMap;
import net.menoni.glacial.nations.bot.jdbc.repository.MatchMapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchMapService {

	@Autowired
	private MatchMapRepository repository;

	public void setMatchMaps(Long matchId, List<JdbcMatchMap> maps) {
		repository.deleteForMatch(matchId);
		repository.add(maps);
	}

	public List<JdbcMatchMap> getMatchMaps(Long matchId) {
		return repository.getForMatch(matchId);
	}

}
