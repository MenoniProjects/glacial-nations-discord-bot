package net.menoni.glacial.nations.bot.event;


import net.menoni.glacial.nations.bot.jdbc.model.JdbcMatch;

public record MatchCompletedEvent(
		JdbcMatch match
) { }
