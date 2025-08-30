package net.menoni.glacial.nations.bot.event;

import net.menoni.ws.discord.service.support.PickBanSession;

public record PickBanCompletedEvent(
		PickBanSession session
) { }
