package net.menoni.glacial.nations.bot.jdbc.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JdbcTeamMember {

    private String discordId;
    private Long teamId;
    private Boolean captain;

}
