package net.menoni.glacial.nations.bot.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.menoni.glacial.nations.bot.discord.DiscordBot;
import net.menoni.glacial.nations.bot.jdbc.model.JdbcTeam;
import net.menoni.glacial.nations.bot.menoni.model.GncMember;
import net.menoni.glacial.nations.bot.service.model.GSheetFormat;
import net.menoni.glacial.nations.bot.service.model.GSheetTab;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GSheetService {

	private final RestTemplate template = new RestTemplateBuilder()
			.messageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8))
			.build();

	@Autowired
	private MemberService memberService;
	@Autowired
	private DiscordBot bot;
	@Autowired
	private TeamService teamService;
	@Autowired
	private ApplicationContext applicationContext;

	// key part of edit url
	@Value("${glacial.sheet.key:''}")
	private String sheetKey;

	// key part of published url
	@Value("${glacial.sheet.public-key:''}")
	private String sheetPublicKey;

	// sheet tab ids
	@Value("#{${glacial.sheet.tabs:{T(java.util.Collections).emptyMap()}}}")
	private Map<GSheetTab, String> sheetTabIds;

	@PostConstruct
	public void testSheetTabPresence() {
		if (this.sheetKey == null || this.sheetKey.isBlank()) {
			log.warn("Missing sheet key");
		}
		if (this.sheetPublicKey == null || this.sheetPublicKey.isBlank()) {
			log.warn("Missing sheet public key");
		}
		for (GSheetTab enumKey : GSheetTab.values()) {
			if (!sheetTabIds.containsKey(enumKey)) {
				log.warn("Missing sheet tab configuration for {}", enumKey);
			}
		}
	}

	public String getSheetTabId(GSheetTab tab) {
		return sheetTabIds.get(tab);
	}

	public String getSheetUrl(GSheetTab tab, GSheetFormat format) {
		String sheetId = getSheetTabId(tab);
		if (format == GSheetFormat.CSV) {
			if (sheetId == null) {
				return null;
			}
			return "https://docs.google.com/spreadsheets/d/%s/export?format=csv&gid=%s".formatted(this.sheetKey, sheetId);
		}
		if (format == GSheetFormat.PUBLIC) {
			if (this.sheetPublicKey != null && !this.sheetPublicKey.isBlank()) {
				return "https://docs.google.com/spreadsheets/d/e/%s/pubhtml#".formatted(this.sheetPublicKey);
			}
		}
		return "https://docs.google.com/spreadsheets/d/%s/edit?gid=%s#gid=%s".formatted(this.sheetKey, sheetId, sheetId);
	}

	public List<String[]> getGncSheet(GSheetTab tab) throws IOException, CsvException {
		if (this.sheetKey == null || this.sheetKey.isBlank()) {
			return null;
		}
		String url = getSheetUrl(tab, GSheetFormat.CSV);
		if (url == null) {
			return null;
		}

		ResponseEntity<String> csvEntity = template.getForEntity(url, String.class);
		if (csvEntity.getStatusCode().isError()) {
			log.error("sheet error: {}", csvEntity.getStatusCode().value());
			return null;
		}
		if (csvEntity.getBody() == null) {
			return new ArrayList<>();
		}

		String body = csvEntity.getBody();
		CSVReader reader = new CSVReader(new StringReader(body));
		List<String[]> result = reader.readAll();
		reader.close();
		return result;
	}

	public List<String> createMatchChannelsForRound(boolean primary, int round) throws Exception {
		List<String> resultText = new ArrayList<>();
		List<String[]> sheetContent = getGncSheet(primary ? GSheetTab.BRACKET_PRIMARY : GSheetTab.BRACKET_SECONDARY);
		if (sheetContent == null || sheetContent.size() < 5) {
			// not enough rows
			resultText.add("Sheet does not have enough rows");
			return resultText;
		}
		String[] roundHeaders = sheetContent.get(1);
		int roundColumnIndex = -1;
		for (int i = 0; i < roundHeaders.length; i++) {
			if (roundHeaders[i].equalsIgnoreCase("round %d".formatted(round))) {
				roundColumnIndex = i;
				break;
			}
		}
		if (roundColumnIndex == -1) {
			// round not found
			resultText.add("Could not find round in sheet");
			return resultText;
		}

		List<Role> roles = bot.applyGuild(Guild::getRoles, null);
		if (roles == null) {
			resultText.add("Could not load guild roles");
			return resultText;
		}
		Map<String, Role> nameMappedRoles = roles.stream().collect(Collectors.toMap(Role::getName, r -> r));

		List<GncMember> captains = memberService.getAll(this.bot.getGuildId());
		captains.removeIf(m -> {
			if (m.getTeamMember() == null) {
				return true;
			}
			return !m.getTeamMember().getCaptain();
		});

		List<JdbcTeam> teams = teamService.getAllTeams();

		MatchChannelService matchChannelService = applicationContext.getBean(MatchChannelService.class);

		// start at 4th row
		int matchNum = 1;
		for (int i = 3; i < sheetContent.size(); i+=2) {
			if (sheetContent.size() < i + 2) {
				break;
			}
			String firstTeamName = sheetContent.get(i)[roundColumnIndex];
			String secondTeamName = sheetContent.get(i+1)[roundColumnIndex];

			if (firstTeamName.isBlank() && secondTeamName.isBlank()) {
				resultText.add("Missing both team names in match #%d".formatted(matchNum));
			} else if (firstTeamName.isBlank() || secondTeamName.isBlank()) {
				resultText.add("Missing team in match #%d".formatted(matchNum));
			} else {
				Role firstTeamRole = nameMappedRoles.get(firstTeamName);
				Role secondTeamRole = nameMappedRoles.get(secondTeamName);
				if (firstTeamRole == null && secondTeamRole == null) {
					resultText.add("Could not find both team roles for team '%s' and team '%s' in match #%d".formatted(firstTeamName, secondTeamName, matchNum));
				} else if (firstTeamRole == null) {
					resultText.add("Could not find team role for team '%s' in match #%d".formatted(firstTeamName, matchNum));
				} else if (secondTeamRole == null) {
					resultText.add("Could not find team role for team '%s' in match #%d".formatted(secondTeamName, matchNum));
				} else {
					JdbcTeam team1 = teams.stream().filter(t -> Objects.equals(t.getDiscordRoleId(), firstTeamRole.getId())).findAny().orElse(null);
					JdbcTeam team2 = teams.stream().filter(t -> Objects.equals(t.getDiscordRoleId(), secondTeamRole.getId())).findAny().orElse(null);
					if (team1 == null || team2 == null) {
						resultText.add("Failed to find team for match #%d".formatted(matchNum));
						continue;
					}
					GncMember captain1 = captains.stream().filter(c -> Objects.equals(c.getTeamMember().getTeamId(), team1.getId())).findAny().orElse(null);
					GncMember captain2 = captains.stream().filter(c -> Objects.equals(c.getTeamMember().getTeamId(), team2.getId())).findAny().orElse(null);
					if (captain1 == null || captain2 == null) {
						resultText.add("Failed to find captain for team in match #%d".formatted(matchNum));
						continue;
					}

					CompletableFuture<TextChannel> channelFuture = matchChannelService.createMatchChannel(primary, round, firstTeamRole, secondTeamRole, captain1.getMenoniMember().getEffectiveName(), captain2.getMenoniMember().getEffectiveName());
					try {
						TextChannel textChannel = channelFuture.get(10L, TimeUnit.SECONDS);
						resultText.add("Match #%d: (%s vs %s) -> <#%s>".formatted(matchNum, firstTeamName, secondTeamName, textChannel.getId()));
					} catch (InterruptedException | ExecutionException | TimeoutException e) {
						log.error("Failed to create match channel #%d (%s vs %s) for bracket %s round %d".formatted(matchNum, firstTeamName, secondTeamName, primary ? "primary" : "secondary", round), e);
						resultText.add("Error creating channel for match #%d (%s vs %s)".formatted(matchNum, firstTeamName, secondTeamName));
					}
				}
			}
			matchNum++;
		}
		return resultText;
	}

	public List<String> verifySheet(boolean primary) throws IOException, CsvException {
		List<String> resultText = new ArrayList<>();
		List<String[]> sheetContent = getGncSheet(primary ? GSheetTab.BRACKET_PRIMARY : GSheetTab.BRACKET_SECONDARY);
		if (sheetContent == null || sheetContent.size() < 5) {
			// not enough rows
			resultText.add("Sheet does not have enough rows");
			return resultText;
		}
		String[] roundHeaders = sheetContent.get(1);
		int roundColumnIndex = -1;
		for (int i = 0; i < roundHeaders.length; i++) {
			if (roundHeaders[i].equalsIgnoreCase("round %d".formatted(1))) {
				roundColumnIndex = i;
				break;
			}
		}
		if (roundColumnIndex == -1) {
			// round not found
			resultText.add("Could not find round 1 in sheet");
			return resultText;
		}

		List<Role> roles = bot.applyGuild(Guild::getRoles, null);
		if (roles == null) {
			resultText.add("Could not load guild roles");
			return resultText;
		}
		Map<String, Role> nameMappedRoles = roles.stream().collect(Collectors.toMap(Role::getName, r -> r));

		// start at 4th row
		int matchNum = 1;
		int correct = 0;
		int invalid = 0;
		for (int i = 3; i < sheetContent.size(); i+=2) {
			if (sheetContent.size() < i + 2) {
				break;
			}
			String firstTeamName = sheetContent.get(i)[roundColumnIndex];
			String secondTeamName = sheetContent.get(i+1)[roundColumnIndex];

			if (firstTeamName.isBlank() || secondTeamName.isBlank()) {
				resultText.add("Missing team in match #%d".formatted(matchNum));
				invalid++;
			} else {
				Role firstTeamRole = nameMappedRoles.get(firstTeamName);
				Role secondTeamRole = nameMappedRoles.get(secondTeamName);
				if (firstTeamRole == null && secondTeamRole == null) {
					resultText.add("Could not find **both** teams for match #%d".formatted(matchNum));
					invalid++;
				} else if (firstTeamRole == null) {
					resultText.add("Could not find **first** team for match #%d".formatted(matchNum));
					invalid++;
				} else if (secondTeamRole == null) {
					resultText.add("Could not find **second** team for match #%d".formatted(matchNum));
					invalid++;
				} else {
					correct++;
				}
			}
			matchNum++;
		}
		resultText.add("Verification result: %d correct matches and %d invalid matches".formatted(correct, invalid));
		return resultText;
	}

}
