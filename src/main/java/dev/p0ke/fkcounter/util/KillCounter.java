package dev.p0ke.fkcounter.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import dev.jeinton.mwutils.MwScoreboardParser;
import dev.p0ke.fkcounter.FKCounterMod;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class KillCounter {

	private static final String PREP_PHASE = "Prepare your defenses!";

	private static final String[] KILL_MESSAGES = {
			"(\\w+) was shot and killed by (\\w+).*",
			"(\\w+) was snowballed to death by (\\w+).*",
			"(\\w+) was killed by (\\w+).*",
			"(\\w+) was killed with a potion by (\\w+).*",
			"(\\w+) was killed with an explosion by (\\w+).*",
			"(\\w+) was killed with magic by (\\w+).*",
			"(\\w+) was filled full of lead by (\\w+).*",
			"(\\w+) was iced by (\\w+).*",
			"(\\w+) met their end by (\\w+).*",
			"(\\w+) lost a drinking contest with (\\w+).*",
			"(\\w+) was killed with dynamite by (\\w+).*",
			"(\\w+) lost the draw to (\\w+).*",
			"(\\w+) was struck down by (\\w+).*",
			"(\\w+) was turned to dust by (\\w+).*",
			"(\\w+) was turned to ash by (\\w+).*",
			"(\\w+) was melted by (\\w+).*",
			"(\\w+) was incinerated by (\\w+).*",
			"(\\w+) was vaporized by (\\w+).*",
			"(\\w+) was struck with Cupid's arrow by (\\w+).*",
			"(\\w+) was given the cold shoulder by (\\w+).*",
			"(\\w+) was hugged too hard by (\\w+).*",
			"(\\w+) drank a love potion from (\\w+).*",
			"(\\w+) was hit by a love bomb from (\\w+).*",
			"(\\w+) was no match for (\\w+).*",
			"(\\w+) was smote from afar by (\\w+).*",
			"(\\w+) was justly ended by (\\w+).*",
			"(\\w+) was purified by (\\w+).*",
			"(\\w+) was killed with holy water by (\\w+).*",
			"(\\w+) was dealt vengeful justice by (\\w+).*",
			"(\\w+) was returned to dust by (\\w+).*",
			"(\\w+) be shot and killed by (\\w+).*",
			"(\\w+) be snowballed to death by (\\w+).*",
			"(\\w+) be sent to Davy Jones' locker by (\\w+).*",
			"(\\w+) be killed with rum by (\\w+).*",
			"(\\w+) be shot with cannon by (\\w+).*",
			"(\\w+) be killed with magic by (\\w+).*",
			"(\\w+) was glazed in BBQ sauce by (\\w+).*",
			"(\\w+) was sprinked in chilli poweder by (\\w+).*",
			"(\\w+) was sliced up by (\\w+).*",
			"(\\w+) was overcooked by (\\w+).*",
			"(\\w+) was deep fried by (\\w+).*",
			"(\\w+) was boiled by (\\w+).*",
			"(\\w+) was injected with malware by (\\w+).*",
			"(\\w+) was DDoS'd by (\\w+).*",
			"(\\w+) was deleted by (\\w+).*",
			"(\\w+) was purged by an antivirus owned by (\\w+).*",
			"(\\w+) was fragmented by (\\w+).*",
			"(\\w+) was squeaked from a distance by (\\w+).*",
			"(\\w+) was hit by frozen cheese from (\\w+).*",
			"(\\w+) was chewed up by (\\w+).*",
			"(\\w+) was chemically cheesed by (\\w+).*",
			"(\\w+) was turned into cheese wiz by (\\w+).*",
			"(\\w+) was magically squeaked by (\\w+).*",
			"(\\w+) was corrupted by (\\w+).*"
	};
	
	private static final int TEAMS = 4;
	public static final int RED_TEAM = 0;
	public static final int GREEN_TEAM = 1;
	public static final int YELLOW_TEAM = 2;
	public static final int BLUE_TEAM = 3;
	
	private static final String[] SCOREBOARD_PREFIXES = {
			"[R]", "[G]", "[Y]", "[B]"	
		};
	private static final String[] DEFAULT_PREFIXES = {
			"c", "a", "e", "9"	
		};

	private String[] prefixes;
	private HashMap<String, Integer>[] teamKills;
	private ArrayList<String> deadPlayers;

	private String gameId;

	@SuppressWarnings("unchecked")
	public KillCounter(String gameId) {
		this.gameId = gameId;

		MinecraftForge.EVENT_BUS.register(this);

		prefixes = new String[TEAMS];
		teamKills = new HashMap[TEAMS];
		deadPlayers = new ArrayList<String>();
		for(int team = 0; team < TEAMS; team++) {
			prefixes[team] = DEFAULT_PREFIXES[team];
			teamKills[team] = new HashMap<String, Integer>();
		}
	}
	
	@SubscribeEvent
	public void onChatMessage(ClientChatReceivedEvent event) {
		if (!FKCounterMod.isInMwGame()) {
			return;
		}

		String rawMessage = event.message.getUnformattedText();
		String colorMessage = event.message.getFormattedText();
		
		//System.out.println("Raw: " + rawMessage + "\nColor: " + colorMessage);
				
		//Team color detection
		if(rawMessage.equals(PREP_PHASE)) {
			setTeamPrefixes();
		}
		
		//Kill message detection
		for(String p : KILL_MESSAGES) {
			Matcher killMessageMatcher = Pattern.compile(p).matcher(rawMessage);
			if(killMessageMatcher.matches()) {
				String killed = killMessageMatcher.group(1);
				String killer = killMessageMatcher.group(2);
				
				String killedTeam = colorMessage.split("\u00a7")[2].substring(0, 1);
				String killerTeam = colorMessage.split("\u00a7")[8].substring(0, 1);
				
				removeKilledPlayer(killed, killedTeam);
				
				if(isWitherDead(killedTeam))
					addKill(killer, killerTeam);
				
				break;
				
			}
		}
	}

	public String getGameId() {
		return gameId;
	}

	public int getKills(int team) {
		if(!isValidTeam(team)) { return 0; }
		
		int kills = 0;
		for(int k : teamKills[team].values()) {
			kills += k;
		}
		return kills;
	}
	
	public HashMap<String, Integer> getPlayers(int team){
		if(!isValidTeam(team)) { return new HashMap<String, Integer>(); }
		return teamKills[team];
	}

	private boolean isWitherDead(String color) {
		return !MwScoreboardParser.instance().getMwScoreboardData().isWitherAlive(color);
	}
	
	private void setTeamPrefixes() {
		for(String line : getScoreboardNames()) {
			for(int team = 0; team < TEAMS; team++) {
				if(line.contains(SCOREBOARD_PREFIXES[team])) {
					prefixes[team] = line.split("\u00a7")[1].substring(0, 1);
				}
			}
		}
	}
	
	private void removeKilledPlayer(String player, String color) {
		int team = getTeamFromColor(color);
		if(!isValidTeam(team)) { return; }

		if(isWitherDead(color)) {
			teamKills[team].remove(player);
			deadPlayers.add(player);
		}
		
	}
	
	private void addKill(String player, String color) {
		int team = getTeamFromColor(color);
		if(!isValidTeam(team)) { return; }
		if(deadPlayers.contains(player)) { return; }
		
		if(teamKills[team].containsKey(player)) {
			teamKills[team].put(player, teamKills[team].get(player) + 1);
		} else {
			teamKills[team].put(player, 1);
		}
		
		sortTeamKills(team);
		
	}
	
	private void sortTeamKills(int team) {
		if(!isValidTeam(team)) { return; }
		
		teamKills[team] = teamKills[team].entrySet().stream().sorted(Entry.<String, Integer>comparingByValue().reversed())
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}
	
	private int getTeamFromColor(String color) {
		for(int team = 0; team < TEAMS; team++) {
			if(prefixes[team].equalsIgnoreCase(color))
				return team;
		}
		
		return -1;
	}
	
	private boolean isValidTeam(int team) {
		return (team >= 0 && team < TEAMS);
	}
	
	private static ArrayList<String> getScoreboardNames() {
        
		ArrayList<String> scoreboardNames = new ArrayList<String>();
		
        try {
	        Scoreboard scoreboard = FMLClientHandler.instance().getClient().theWorld.getScoreboard();
	        ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
	        Collection<Score> scores = scoreboard.getSortedScores(sidebarObjective);
	        for(Score score : scores) {
	            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
	            scoreboardNames.add(ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()));
	        }
        } catch (Exception e) { }
        
        return scoreboardNames;
    }

}
