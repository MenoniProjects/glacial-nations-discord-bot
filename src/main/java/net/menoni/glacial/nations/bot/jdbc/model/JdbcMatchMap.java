package net.menoni.glacial.nations.bot.jdbc.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.menoni.ws.discord.service.support.MatchMapType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JdbcMatchMap {

	private Long matchId;
	private Integer mapIndex;
	private String mapUid;
	private String mapName;
	private MatchMapType type;
	private String typeData;

}
