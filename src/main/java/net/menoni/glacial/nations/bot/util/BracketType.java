package net.menoni.glacial.nations.bot.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;

@RequiredArgsConstructor
@Getter
public enum BracketType {

	PRIMARY("a", new Color(85,191,185)),
	SECONDARY("b", new Color(194,102,57)),
	;
	private String displayName;
	private final String character;
	private final Color color;

	public static BracketType getBracket(boolean primary) {
		return primary ? PRIMARY : SECONDARY;
	}

	public static BracketType getBracket(String character) {
		for (BracketType value : BracketType.values()) {
			if (value.getCharacter().equalsIgnoreCase(character)) {
				return value;
			}
		}
		return null;
	}

	public String getDisplayName() {
		if (this.displayName == null) {
			this.displayName = StringUtils.capitalize(this.name().toLowerCase());
		}
		return this.displayName;
	}

	public boolean isPrimary() {
		return this == PRIMARY;
	}

	public boolean isSecondary() {
		return this == SECONDARY;
	}
}
