package net.menoni.glacial.nations.bot.menoni.model;

import lombok.Getter;
import lombok.Setter;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcTeamMember;
import net.menoni.ws.common.model.discord.WsDiscordMember;
import net.menoni.ws.discord.model.ServerMember;

@Getter
@Setter
public class GncMember extends ServerMember {

	private JdbcTeamMember teamMember;

	public GncMember(WsDiscordMember menoniMember) {
		super(menoniMember);
	}

	public GncMember(WsDiscordMember member, JdbcTeamMember teamMember) {
		super(member);
		this.teamMember = teamMember;
	}
}
