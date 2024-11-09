package net.menoni.glacial.bot.jdbc.repository;

import net.dv8tion.jda.api.entities.Role;
import net.menoni.glacial.bot.discord.DiscordBot;
import net.menoni.glacial.bot.jdbc.model.JdbcTeam;
import net.menoni.glacial.bot.util.CountryFlagUtil;
import net.menoni.spring.commons.jdbc.AbstractTypeRepository;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Repository
public class TeamRepository extends AbstractTypeRepository<JdbcTeam> {

	public JdbcTeam getById(Long id) {
		return this.queryOne("SELECT id, country, country_flag_emote, name, discord_role_id FROM team WHERE id = ?", id);
	}

	public JdbcTeam getByRoleId(String roleId) {
		return this.queryOne("SELECT id, country, country_flag_emote, name, discord_role_id FROM team WHERE discord_role_id = ?", roleId);
	}

	public List<JdbcTeam> getAll() {
		return this.queryMany("SELECT id, country, country_flag_emote, name, discord_role_id FROM team");
	}

	public CompletableFuture<JdbcTeam> ensureTeam(DiscordBot bot, String country, String name) {
		CompletableFuture<JdbcTeam> future = new CompletableFuture<>();
		JdbcTeam team = this.queryOne("SELECT id, country, country_flag_emote, name, discord_role_id FROM team WHERE name = ?", name);
		if (team == null) {
			String countryFlag = CountryFlagUtil.tryGetCountryFlag(country);
			bot.withGuild(g ->
					g.createRole()
							.setName(name)
							.queue(r -> {
								JdbcTeam newTeam = new JdbcTeam(null, country, countryFlag, name, r.getId());
								GeneratedKeyHolder key = this.insertOne(
										"INSERT INTO team " +
										"(country, country_flag_emote, name, discord_role_id) VALUE " +
										"(:country, :countryFlagEmote, :name, :discordRoleId)",
										newTeam
								);
								if (key != null) {
									newTeam.setId(key.getKey().longValue());
								}
								future.complete(newTeam);
							}, future::completeExceptionally)
			);
		} else {
			future.complete(team);
		}
		return future;
	}

	public void deleteTeam(DiscordBot bot, JdbcTeam e) {
		if (e == null) {
			return;
		}
		if (e.getId() == null) {
			return;
		}
		this.update("DELETE FROM team WHERE id = :id", Map.of("id", e.getId()));
		if (e.getDiscordRoleId() != null) {
			bot.withGuild(g -> {
				Role role = g.getRoleById(e.getDiscordRoleId());
				if (role != null) {
					role.delete().queue();
				}
			});
		}
	}
}
