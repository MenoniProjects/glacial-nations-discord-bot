package net.menoni.glacial.bot.jdbc.model;

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
	private String matchChannelId;
	private Long winTeamId; // nullable

}
