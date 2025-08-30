package net.menoni.glacial.nations.bot.service;

import net.menoni.glacial.nations.bot.event.MatchCompletedEvent;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcMatch;
import net.menoni.glacial.nations.bot.jdbc.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class MatchService {

	@Autowired
	private MatchRepository matchRepository;
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;

	public void setMatchChannel(
			boolean primaryBracket,
			int roundNumber,
			Long firstTeamId,
			Long secondTeamId,
			String firstCaptainId,
			String secondCaptainId,
			String matchChannelId,
			String pinnedMessageId
	) {
		JdbcMatch match = matchRepository.find(primaryBracket, roundNumber, firstCaptainId, secondCaptainId);
		if (match != null) {
			match.setMatchChannelId(matchChannelId);
			match.setWinIndex(null);
			match.setFirstCaptainId(firstCaptainId);
			match.setSecondCaptainId(secondCaptainId);
			match.setPinnedMessageId(pinnedMessageId);
			matchRepository.save(match);
		} else {
			match = new JdbcMatch(null, primaryBracket, roundNumber, firstTeamId, secondTeamId, firstCaptainId, secondCaptainId, matchChannelId, pinnedMessageId, null);
			matchRepository.save(match);
		}
	}

	public boolean isMatchChannel(String channelId) {
		return getMatchForChannel(channelId) != null;
	}

	public JdbcMatch getMatchForChannel(String channelId) {
		return matchRepository.findByChannel(channelId);
	}

	public JdbcMatch getMatchExact(boolean primaryBracket, int roundNumber, String winTeamCaptain, String loseTeamCaptain) {
		return matchRepository.find(primaryBracket, roundNumber, winTeamCaptain, loseTeamCaptain);
	}

	public JdbcMatch updateMatch(JdbcMatch match) {
		return matchRepository.save(match);
	}

	public JdbcMatch getMatchById(Long id) {
		return this.matchRepository.get(id);
	}

	public void setMatchWinner(JdbcMatch match, Integer winIndex) {
		match.setWinIndex(winIndex);
		this.updateMatch(match);
		this.applicationEventPublisher.publishEvent(new MatchCompletedEvent(match));
	}
}
