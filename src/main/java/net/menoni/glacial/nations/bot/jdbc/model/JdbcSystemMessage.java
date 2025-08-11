package net.menoni.glacial.nations.bot.jdbc.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JdbcSystemMessage {

	private Long id;
	private String key;
	private String channelId;
	private String messageId;

}
