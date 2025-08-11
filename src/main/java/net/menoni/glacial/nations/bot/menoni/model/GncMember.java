package net.menoni.glacial.nations.bot.menoni.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcTeamMember;
import net.menoni.ws.common.model.discord.WsDiscordMember;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GncMember {

	private WsDiscordMember member;
	private JdbcTeamMember teamMember;

}
