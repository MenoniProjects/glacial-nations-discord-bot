package net.menoni.glacial.nations.bot.jdbc.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JdbcTeam {

	private Long id;
	private String country;
	private String countryFlagEmote;
	private String name;
	private String discordRoleId;

}
