package net.menoni.glacial.bot.discord.emote;

public enum StandardEmoji implements Emotable<StandardEmoji>{

    THUMBS_UP("\uD83D\uDC4D"),
    THUMBS_DOWN("\uD83D\uDC4E"),
    ROBOT("\uD83E\uDD16"),
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
