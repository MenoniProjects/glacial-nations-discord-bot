package net.menoni.glacial.nations.bot.discord.listener;

import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.menoni.glacial.nations.bot.discord.DiscordBot;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcTeamMember;
import net.menoni.glacial.nations.bot.menoni.model.GncMember;
import net.menoni.glacial.nations.bot.service.MemberService;
import net.menoni.glacial.nations.bot.service.TeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DiscordMemberListener implements EventListener {

    private static final Logger logger = LoggerFactory.getLogger(DiscordMemberListener.class);

    @Autowired
    private MemberService memberService;

    @Autowired
    private DiscordBot bot;

    @Autowired
    private TeamService teamService;

    @PostConstruct
    public void init() {
        this.bot.addEventListener(this);
    }

    @Override
    public void onEvent(GenericEvent event) {
        if (event instanceof GuildMemberJoinEvent memberJoinEvent) {
            this.memberJoinEvent(memberJoinEvent);
        }
    }

    private void memberJoinEvent(GuildMemberJoinEvent event) {
        if (event.getUser().isBot() || event.getUser().isSystem()) {
            return;
        }
        new Thread(() -> {
	        try { Thread.sleep(5000L); } catch (InterruptedException e) {  }
            logger.info("Ensuring new member roles (join): {}/{}", event.getUser().getId(), event.getUser().getName());
            GncMember botMember = memberService.get(event.getGuild().getId(), event.getMember().getId());
            Role memberRole = bot.getMemberRole();
            Role playerRole = bot.getPlayerRole();
            Role teamLeadRole = bot.getTeamLeadRole();
            teamService.ensurePlayerRoles(event.getMember(), botMember, memberRole, playerRole, teamLeadRole);
        }).start();
    }


}
