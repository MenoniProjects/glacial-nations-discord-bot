package net.menoni.glacial.bot.util;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class DiscordRoleUtil {

	public enum RoleUpdateResult {
		NO_CHANGE,
		ROLE_ADDED,
		ROLE_REMOVED
	}

	public static RoleUpdateResult updateRole(Member guildMember, Role guildRole, boolean shouldHaveRole) {
		boolean hasRole = false;
		for (Role role : guildMember.getRoles()) {
			if (role.getId().equals(guildRole.getId())) {
				hasRole = true;
				break;
			}
		}

		if (shouldHaveRole && !hasRole) {
			guildMember.getGuild().addRoleToMember(guildMember, guildRole).queue();
			return RoleUpdateResult.ROLE_ADDED;
		} else if (!shouldHaveRole && hasRole) {
			guildMember.getGuild().removeRoleFromMember(guildMember, guildRole).queue();
			return RoleUpdateResult.ROLE_REMOVED;
		} else {
			return RoleUpdateResult.NO_CHANGE;
		}
	}

	public static boolean hasRole(Member guildMember, Role guildRole) {
		return hasRole(guildMember, guildRole.getId());
	}

	public static boolean hasRole(Member guildMember, String guildRoleId) {
		for (Role role : guildMember.getRoles()) {
			if (role.getId().equals(guildRoleId)) {
				return true;
			}
		}
		return false;
	}

}
