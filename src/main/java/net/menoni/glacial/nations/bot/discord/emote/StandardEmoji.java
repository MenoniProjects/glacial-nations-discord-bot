package net.menoni.glacial.nations.bot.discord.emote;

import net.menoni.jda.commons.discord.emote.Emotable;

public enum StandardEmoji implements Emotable<StandardEmoji> {

    WORLD_MAP("\uD83D\uDDFA\uFE0F"),
    MEMO("\uD83D\uDCDD"),
    TROPHY("\uD83C\uDFC6"),
    HEAVY_MINUS_SIGN("\u2796"),
    CROSS("\u274c"),
    WHITE_CHECK_MARK("\u2705"),
    WARNING("\u26a0\ufe0f️"),
    PLAY_BUTTON("\u25B6\uFE0F"),

    MEDAL_FIRST("\uD83E\uDD47"),
    MEDAL_SECOND("\uD83E\uDD48"),
    MEDAL_THIRD("\uD83E\uDD49"),
    MEDAL_GENERIC("\uD83C\uDFC5"),
    ;

    private final String value;

    StandardEmoji(String value) {
        this.value = value;
    }

    @Override
    public String print() {
        return this.value;
    }
}
