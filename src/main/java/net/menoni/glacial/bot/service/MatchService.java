package net.menoni.glacial.bot.service;

import net.menoni.glacial.bot.jdbc.model.JdbcMatch;
import net.menoni.glacial.bot.jdbc.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MatchService {

	@Autowired
	private MatchRepository matchRepository;

	public void setMatchChannel(boolean primaryBracket, int roundNumber, Long firstTeamId, Long secondTeamId, String matchChannelId) {
		JdbcMatch match = matchRepository.find(primaryBracket, roundNumber, firstTeamId, secondTeamId);
		if (match != null) {
			match.setMatchChannelId(matchChannelId);
			match.setWinTeamId(null);
			matchRepository.save(match);
		} else {
			match = new JdbcMatch(null, primaryBracket, roundNumber, firstTeamId, secondTeamId, matchChannelId, null);
			matchRepository.save(match);
		}
	}

	public boolean isMatchChannel(String channelId) {
		return getMatchForChannel(channelId) != null;
	}

	public JdbcMatch getMatchForChannel(String channelId) {
		return matchRepository.findByChannel(channelId);
	}

	public JdbcMatch getMatchExact(boolean primaryBracket, int roundNumber, Long winTeamId, Long loseRoleId) {
		return matchRepository.find(primaryBracket, roundNumber, winTeamId, loseRoleId);
	}

	public JdbcMatch updateMatch(JdbcMatch match) {
		return matchRepository.save(match);
	}

}
