package net.menoni.glacial.bot.discord.emote;

import lombok.Getter;

/**
 * To get an emote ID send:
 * \:emote:
 * in a channel (like a normal emote with a backslash before)
 * it will send something like:
 * <:ozzy:1139727471244226580>
 * instead
 */
@Getter
public enum CustomEmote implements Emotable<CustomEmote> {
    OZZY("1139727471244226580"),
    VIKING("1139701997877075988")
    ;

    private final String id;
    private final boolean animated;

    CustomEmote(String id, boolean animated) {
        this.id = id;
        this.animated = animated;
    }

    CustomEmote(String id) {
        this(id, false);
    }

    @Override
    public String print() {
        if (this.animated) {
            return String.format("<a:%s:%s>", this.name().toLowerCase(), this.id);
        }
        return String.format("<:%s:%s>", this.name().toLowerCase(), this.id);
    }

}
