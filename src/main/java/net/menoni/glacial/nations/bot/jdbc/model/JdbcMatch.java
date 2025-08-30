package net.menoni.glacial.nations.bot.jdbc.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JdbcMatch {

	private Long id;
	private Boolean primaryBracket;
	private Integer roundNumber;
	private Long firstTeamId;
	private Long secondTeamId;
	private String firstCaptainId;
	private String secondCaptainId;
	private String matchChannelId;
	private String pinnedMessageId;
	private Integer winIndex; // nullable

}
