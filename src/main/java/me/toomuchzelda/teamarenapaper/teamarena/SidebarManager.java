package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.Iterator;

//could be made into an instantiable object class
public class SidebarManager {

    public static final Scoreboard SCOREBOARD = Bukkit.getScoreboardManager().getNewScoreboard();

    //The text on top of the sidebar
    public static final Objective OBJECTIVE;
    public static ArrayList<Team> lineTeams = new ArrayList<>();

    public static long teamNames = 0;
    public static final String TEAMS_IDENTIFIER = "sb";
    //some colour code, just for invisible entry name
    public static final String ENTRY_IDENTIFIER = "§b";

    static {
        OBJECTIVE = SCOREBOARD.registerNewObjective("testObjective", "dummy",
                Component.text("Component name").color(TextColor.color(0, 255, 255)), RenderType.INTEGER);

        OBJECTIVE.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    /**
     * Set the "title" (Objective name) of the sidebar (Objective shown on the sidebar DisplaySlot)
     */
    public static void setTitle(Component displayName) {
        OBJECTIVE.displayName(displayName);
    }

    public static void setLines(Component... lines) {
        int max = Math.max(lines.length, lineTeams.size());
        //set scores to keep order of entries correct in sidebar
        int lineNum = 0;
        
        /*for(int i = 0; i <= lineTeams.size(); i++) {
            OBJECTIVE.getScore(getUniqueEntryName(i)).resetScore();
        }*/

        for(int i = 0; i < max; i++) {
            //replace the team prefix for that line
            if(i < lines.length && i < lineTeams.size()) {
                Team team = lineTeams.get(i);
                if(!team.suffix().contains(lines[i]) && !lines[i].contains(team.suffix()))
                    team.suffix(lines[i]);
                String entryName = getUniqueEntryName(i);
                //will auto remove from other team for me
                team.addEntry(entryName);
                OBJECTIVE.getScore(entryName).setScore(lines.length - i);
            }
            //more existing lines than we now want, so remove existing line
            else if(i >= lines.length && i < lineTeams.size()) {
                OBJECTIVE.getScore(getUniqueEntryName(i)).resetScore();
                lineTeams.get(i).unregister();
                //don't remove just yet to not interrupt the for loop?
                lineTeams.set(i, null);
            }
            //we need to add more lines (teams with prefix)
            else if(i >= lineTeams.size() && i < lines.length){
                Team newLine = SCOREBOARD.registerNewTeam(TEAMS_IDENTIFIER + teamNames++);
                newLine.suffix(lines[i]);
                String entry = getUniqueEntryName(i);
                newLine.addEntry(entry);
                OBJECTIVE.getScore(entry).setScore(lines.length - i);
                lineTeams.add(newLine);
            }
            
            //lineNum--;
        }

        //clean up
        Iterator<Team> iter = lineTeams.iterator();
        while(iter.hasNext()) {
            if(iter.next() == null)
                iter.remove();
        }
    }

    public static void updatePreGameScoreboard(TeamArena game) {
        SidebarManager.setTitle(Component.text("Teams").color(NamedTextColor.GOLD));
        TeamArenaTeam[] teams = game.getTeams();
        Component[] teamsList = new Component[teams.length];
        for(int i = 0; i < teams.length; i++) {
            teamsList[i] = teams[i].getComponentName();
        }
        setLines(teamsList);
    }

    public static void updateTeamsDecidedScoreboard(TeamArena game) {
        // Team names and number of players e.g
        //--------------------
        // Blue Team
        // Players: 13
        // Red Team
        // Players: 12
        //--------------------

        TeamArenaTeam[] teams = game.getTeams();

        int size = game.getTeams().length * 2;
        //two more lines for spectators, if there are any
        if(game.getSpectators().size() > 0)
            size += 1;

        Component[] lines = new Component[size];
        int index = 0;
        for(TeamArenaTeam team : teams) {
            lines[index] = team.getComponentName();
            lines[index + 1] = Component.text("Players: " + team.getEntityMembers().size());

            index += 2;
        }

        if(game.getSpectators().size() > 0) {
            lines[index] = game.getSpectatorTeam().getComponentName();
        }
        setLines(lines);
    }

    public static String getUniqueEntryName(int num) {
        /*String s = ENTRY_IDENTIFIER;
        for(int i = 0; i < num; i++) {
            s += ENTRY_IDENTIFIER;
        }

        return s;*/
        return ENTRY_IDENTIFIER + ENTRY_IDENTIFIER.repeat(Math.max(0, num));
    }
}
