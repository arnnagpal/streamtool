package streamtool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import common.Api;

public class Data {

    Player[] players;

    int leagueNumber = 0;
    int weekNumber = 0;

    int currentSeed = 0;
    int headsize = 100;

    boolean useLiveApi = false;
    String apiKey = "";
    String apiUser = "";

    enum SplitType {
        nether,
        bastion,
        fortress,
        firstrod,
        blind,
        stronghold,
        end,
        finish,
        forfeit,
        other,
        reset,
        none
    }

    String[] listed = new String[0];

    void list(String name) {
        if (name == null) return;

        String[] newList = new String[listed.length + 1];
        for (int i = 0; i < listed.length; i++) newList[i] = listed[i];
        newList[listed.length] = name;
        listed = newList;
    }
    
    void clearList() {
        listed = new String[0];
        list("$RESET");
    }

    boolean alreadyListed(String name) {

        if (name == null) return true;

        for (int i = 0; i < listed.length; i++) {
            if (name.equals(listed[i])) return true;
        }
        
        return false;
    }

    void updatePlaying(JSONObject spectateMatch) {
        JSONArray playerList = (JSONArray) spectateMatch.get("players");

        // set playing to false for the entire playerlist
        for (Player player : players) player.playing = false;

        // set playing to true for everyone in the match
        for (int i = 0; i < playerList.length(); i++) {
            JSONObject playerData = (JSONObject) playerList.get(i);
            String playerName = (String) playerData.get("nickname");
            Player player = getPlayer(playerName);
            if (player != null) player.playing = true;
        }

        JSONArray completions = (JSONArray) spectateMatch.get("completes");

        // set playing to false for everyone who finished
        for (int i = 0; i < completions.length(); i++) {
            JSONObject playerData = (JSONObject) completions.get(i);
            String uuid = (String) playerData.get("player");
            String playerName = getName(uuid);
            if (playerName != null) {
                Player player = getPlayer(playerName);
                if (player != null) player.playing = false;
            }
        }
    }

    String[] randomPovs() { // returns 4 randomly selected people who are live

        ArrayList<String> playerList = new ArrayList<>();

        for (int i = 0; i < players.length; i++) if (players[i].live) {
            playerList.add(players[i].name);
        }

        int count = playerList.size();
        if (count > 4) count = 4;

        int found = 0;

        String[] result = new String[count];

        Random random = new Random();

        while (found < count) {
            int r = random.nextInt(0, playerList.size());
            result[found] = playerList.get(r);
            playerList.remove(r);
            found++;
        }

        return result;

    }

    String[] topSplits() { // returns top players (up to 4) by splits
        try {
            updateSplits();
        } catch (Exception e) {
            return new String[0];
        }

        List<JSONObject> playerList = new ArrayList<>();

        for (int i = 0; i < players.length; i++) if (players[i].playing & players[i].live & players[i].hideSplit < currentSplit(players[i].name).getInt("split")) {
            JSONObject player = currentSplit(players[i].name);
            playerList.add(player);
        }

        Collections.sort(playerList, new Comparator<JSONObject>() {
            public int compare(JSONObject a, JSONObject b) {
                int split = Integer.compare(b.getInt("split"), a.getInt("split"));
                if (split != 0) return split; else return Integer.compare(a.getInt("time"), b.getInt("time"));
            }
        });

        int count = playerList.size();
        if (count > 4) count = 4;

        String[] result = new String[count];

        for (int i = 0; i < count; i++) {
            result[i] = playerList.get(i).getString("name");
        }

        return result;
    }

    String[] topPoints() { // returns top players (up to 4) by points
        try {
            updateSplits();
        } catch (Exception e) {
            return new String[0];
        }

        JSONObject leaderboard = Api.readJSON(Paths.get("lb_data", "leaderboard.json").toFile());

        JSONArray players = (JSONArray) leaderboard.get("players");

        List<JSONObject> playerList = new ArrayList<>();

        for (int i = 0; i < players.length(); i++) {
            JSONObject pl = players.getJSONObject(i);
            String name = pl.getString("name");
            Player player = getPlayer(name);

            if (player.playing & player.live & player.hideSplit < currentSplit(player.name).getInt("split")) playerList.add(pl);
        }

        int count = playerList.size();
        if (count > 4) count = 4;

        String[] result = new String[count];

        for (int i = 0; i < count; i++) {
            result[i] = playerList.get(i).getString("name");
        }

        return result;
    }

    String[] topHybrid() { // returns top player by splits and up to 3 more by points

        String[] topSplits = topSplits();
        String[] topPoints = topPoints();

        int count = topSplits.length;

        String[] result = new String[count];

        if (count > 0) {
            result[0] = topSplits[0];
        }

        int b = 0;

        for (int i = 0; i < count - 1; i++) {
            if (topPoints[b].equals(result[0])) {
                i--;
            } else {
                result[i + 1] = topPoints[b];
            }
            b++;
        }

        return result;
    }

    String[] topAdv() { // returns top players (up to 4) by advancement count
        try {
            updateAdv();
        } catch (Exception e) {
            return new String[0];
        }

        List<Player> playerList = new ArrayList<>();

        for (int i = 0; i < players.length; i++) if (players[i].playing && players[i].live) {
            Player player = players[i];
            if (player != null) playerList.add(player);
        }

        Collections.sort(playerList, new Comparator<Player>() {
            public int compare(Player a, Player b) {
                return b.advCount - a.advCount;
            }
        });

        int count = playerList.size();
        if (count > 4) count = 4;

        String[] result = new String[count];

        for (int i = 0; i < count; i++) {
            result[i] = playerList.get(i).name;
        }

        return result;
    }

    String[] bottomAdv() { // returns top players (up to 4) by advancement count
        try {
            updateAdv();
        } catch (Exception e) {
            return new String[0];
        }

        List<Player> playerList = new ArrayList<>();

        for (int i = 0; i < players.length; i++) if (players[i].playing & players[i].live) {
            Player player = players[i];
            if (player != null) playerList.add(player);
        }

        Collections.sort(playerList, new Comparator<Player>() {
            public int compare(Player a, Player b) {
                return a.advCount - b.advCount;
            }
        });

        int count = playerList.size();
        if (count > 4) count = 4;

        String[] result = new String[count];

        for (int i = 0; i < count; i++) {
            result[i] = playerList.get(i).name;
        }

        return result;
    }

    JSONObject currentSplit(String playerName) {

        JSONObject result = new JSONObject();
        result.put("name", playerName);

        for (int i = 0; i < forfeit.length; i++) if (forfeit[i].player != null & forfeit[i].player.equals(playerName)) {
            result.put("split", -1);
            result.put("time", -1);
            return result;
        }

        for (int i = 0; i < finish.length; i++) if (finish[i].player != null & finish[i].player.equals(playerName)) {
            result.put("split", -1);
            result.put("time", -1);
            return result;
        }

        for (int i = 0; i < end.length; i++) if (end[i].player != null & end[i].player.equals(playerName)) {
            result.put("split", 7);
            result.put("time", end[i].time);
            return result;
        }

        for (int i = 0; i < stronghold.length; i++) if (stronghold[i].player != null & stronghold[i].player.equals(playerName)) {
            result.put("split", 6);
            result.put("time", stronghold[i].time);
            return result;
        }

        for (int i = 0; i < blind.length; i++) if (blind[i].player != null & blind[i].player.equals(playerName)) {
            result.put("split", 5);
            result.put("time", blind[i].time);
            return result;
        }

        for (int i = 0; i < firstrod.length; i++) if (firstrod[i].player != null & firstrod[i].player.equals(playerName)) {
            result.put("split", 4);
            result.put("time", firstrod[i].time);
            return result;
        }

        for (int i = 0; i < fortress.length; i++) if (fortress[i].player != null & fortress[i].player.equals(playerName)) {
            result.put("split", 3);
            result.put("time", fortress[i].time);
            return result;
        }

        for (int i = 0; i < bastion.length; i++) if (bastion[i].player != null & bastion[i].player.equals(playerName)) {
            result.put("split", 2);
            result.put("time", bastion[i].time);
            return result;
        }

        for (int i = 0; i < nether.length; i++) if (nether[i].player != null & nether[i].player.equals(playerName)) {
            result.put("split", 1);
            result.put("time", nether[i].time);
            return result;
        }

        for (int i = 0; i < players.length; i++) if (players[i] != null & players[i].name.equals(playerName)) {
            result.put("split", 0);
            result.put("time", -1 * players[i].lb_points);
            return result;
        }

        result.put("split", -1);
        result.put("time", -1);
        return result;
    }

    void printSplits() {

        clearList();

        for (int i = 0; i < forfeit.length; i++) if (!alreadyListed(forfeit[i].player)) {
            list(forfeit[i].player);

            System.out.println("     " + forfeit[i].player + " FORFEIT " + Main.getTimeString(forfeit[i].time, true));
        }

        System.out.println();

        for (int i = 0; i < finish.length; i++) if (!alreadyListed(finish[i].player)) {
            list(finish[i].player);

            System.out.println("     " + finish[i].player + " FINISH " + Main.getTimeString(finish[i].time, true));
        }

        for (int i = 0; i < end.length; i++) if (!alreadyListed(end[i].player)) {
            list(end[i].player);
            Player p = this.getPlayer(end[i].player);
            boolean live = false;
            if (p != null) live = p.live;

            if (live) System.out.println("LIVE " + p.id + " " + end[i].player + " END " + Main.getTimeString(end[i].time, true)); else System.out.println("     " + end[i].player + " END " + Main.getTimeString(end[i].time, true));
        }

        for (int i = 0; i < stronghold.length; i++) if (!alreadyListed(stronghold[i].player)) {
            list(stronghold[i].player);
            Player p = this.getPlayer(stronghold[i].player);
            boolean live = false;
            if (p != null) live = p.live;
            if (live) System.out.println("LIVE " + p.id + " " + stronghold[i].player + " STRONGHOLD " + Main.getTimeString(stronghold[i].time, true)); else System.out.println("     " + stronghold[i].player + " STRONGHOLD " + Main.getTimeString(stronghold[i].time, true));
        }

        for (int i = 0; i < blind.length; i++) if (!alreadyListed(blind[i].player)) {
            list(blind[i].player);
            Player p = this.getPlayer(blind[i].player);
            boolean live = false;
            if (p != null) live = p.live;
            if (live) System.out.println("LIVE " + p.id + " " + blind[i].player + " BLIND " + Main.getTimeString(blind[i].time, true)); else System.out.println("     " + blind[i].player + " BLIND " + Main.getTimeString(blind[i].time, true));
        }

        for (int i = 0; i < firstrod.length; i++) if (!alreadyListed(firstrod[i].player)) {
            list(firstrod[i].player);
            Player p = this.getPlayer(firstrod[i].player);
            boolean live = false;
            if (p != null) live = p.live;
            if (live) System.out.println("LIVE " + p.id + " " + firstrod[i].player + " FIRST ROD " + Main.getTimeString(firstrod[i].time, true)); else System.out.println("     " + firstrod[i].player + " FIRST ROD " + Main.getTimeString(firstrod[i].time, true));
        }

        for (int i = 0; i < fortress.length; i++) if (!alreadyListed(fortress[i].player)) {
            list(fortress[i].player);
            Player p = this.getPlayer(fortress[i].player);
            boolean live = false;
            if (p != null) live = p.live;
            if (live) System.out.println("LIVE " + p.id + " " + fortress[i].player + " FORTRESS " + Main.getTimeString(fortress[i].time, true)); else System.out.println("     " + fortress[i].player + " FORTRESS " + Main.getTimeString(fortress[i].time, true));
        }

        for (int i = 0; i < bastion.length; i++) if (!alreadyListed(bastion[i].player)) {
            list(bastion[i].player);
            Player p = this.getPlayer(bastion[i].player);
            boolean live = false;
            if (p != null) live = p.live;
            if (live) System.out.println("LIVE " + p.id + " " + bastion[i].player + " BASTION " + Main.getTimeString(bastion[i].time, true)); else System.out.println("     " + bastion[i].player + " BASTION " + Main.getTimeString(bastion[i].time, true));
        }

        for (int i = 0; i < nether.length; i++) if (!alreadyListed(nether[i].player)) {
            list(nether[i].player);
            Player p = this.getPlayer(nether[i].player);
            boolean live = false;
            if (p != null) live = p.live;
            if (live) System.out.println("LIVE " + p.id + " " + nether[i].player + " NETHER " + Main.getTimeString(nether[i].time, true)); else System.out.println("     " + nether[i].player + " NETHER " + Main.getTimeString(nether[i].time, true));
        }

        for (int i = 0; i < players.length; i++) if (players[i].playing && !alreadyListed(players[i].name)) {
            list(players[i].name);
            if (players[i].live) System.out.println("LIVE " + players[i].id + " " + players[i].name + " OVERWORLD"); else System.out.println("     " + players[i].name + " OVERWORLD");
        }
    }

    void printAdv() {
        ArrayList<Player> pl = new ArrayList<>();
        for (int i = 0; i < players.length; i++) {
            Player p = players[i];
            if (p != null && p.playing) {
                pl.add(p);
            }
        }

        Collections.sort(pl, new Comparator<Player>() {
            public int compare(Player a, Player b) {
                return b.advCount - a.advCount;
            }
        });

        for (int i = 0; i < pl.size(); i++) {
            String s = "";
            if (pl.get(i).live) s = s + "LIVE ";
            s = s + pl.get(i).name + " " + pl.get(i).advCount;
            System.out.println(s);
        }
    }

    SplitType ident(JSONObject o) {
        String type = o.getString("type");

        if (type.equals("nether.root")) return SplitType.nether;
        if (type.equals("nether.find_bastion")) return SplitType.bastion;
        if (type.equals("nether.find_fortress")) return SplitType.fortress;
        if (type.equals("nether.obtain_blaze_rod")) return SplitType.firstrod;
        if (type.equals("projectelo.timeline.blind_travel")) return SplitType.blind;
        if (type.equals("story.follow_ender_eye")) return SplitType.stronghold;
        if (type.equals("story.enter_the_end")) return SplitType.end;
        if (type.equals("projectelo.timeline.complete")) return SplitType.finish;
        if (type.equals("projectelo.timeline.forfeit")) return SplitType.forfeit;
        if (type.equals("projectelo.timeline.eliminate")) return SplitType.forfeit;
        if (type.equals("projectelo.timelime.reset")) return SplitType.reset;

        return SplitType.other;
    }

    boolean isAdv(JSONObject o) {
        String type = o.getString("type");

        if (type.contains("projectelo")) return false;

        if (type.contains("story.")) return true;
        if (type.contains("nether.")) return true;
        if (type.contains("end.")) return true;
        if (type.contains("adventure.")) return true;
        if (type.contains("husbandry.")) return true;

        return false;
    }

    Split[] nether;
    Split[] bastion;
    Split[] fortress;
    Split[] firstrod;
    Split[] blind;
    Split[] stronghold;
    Split[] end;
    Split[] finish;
    Split[] forfeit;

    public Data() {
        clearPlayers();
        clearSplits();
    }

    public Data(boolean mostPercent) {
        this();
        if (mostPercent) headsize = 130;
    }

    public Data(boolean mostPercent, String apiKey, String apiUser) {
        this(mostPercent);
        this.useLiveApi = true;
        this.apiKey = apiKey;
        this.apiUser = apiUser;
    }

    void clearPlayers() {
        players = new Player[0];
    }

    void clearSplits() {
        nether = new Split[0];
        bastion = new Split[0];
        fortress = new Split[0];
        firstrod = new Split[0];
        blind = new Split[0];
        stronghold = new Split[0];
        end = new Split[0];
        finish = new Split[0];
        forfeit = new Split[0];
    }

    void clearSplitsForPlayer(String name) {
        clearHelper(name, nether);
        clearHelper(name, bastion);
        clearHelper(name, fortress);
        clearHelper(name, firstrod);
        clearHelper(name, blind);
        clearHelper(name, stronghold);
        clearHelper(name, end);
    }

    private void clearHelper(String name, Split[] s) {
        for (int i = 0; i < s.length; i++) if (s[i].player.equals(name)) {
            s[i].player = "$RESET";
        }
    }

    void clearAdv() {
        for (int i = 0; i < players.length; i++) {
            if (players[i] != null) players[i].advCount = 0;
        }
    }

    void addSplit(SplitType type, Split split) {

        if (split.player == null) return;

        Player player = getPlayer(split.player);

        if (player == null) return;

        switch (type) {
            case reset:
                clearSplitsForPlayer(split.player);
                break;
            case bastion:
                addSplit(bastion, split, type);
                break;
            case blind:
                addSplit(blind, split, type);
                break;
            case end:
                addSplit(end, split, type);
                break;
            case fortress:
                addSplit(fortress, split, type);
                break;
            case firstrod:
                addSplit(firstrod, split, type);
                break;
            case nether:
                addSplit(nether, split, type);
                break;
            case stronghold:
                addSplit(stronghold, split, type);
                break;
            case finish:
                addSplit(finish, split, type);
                break;
            case forfeit:
                addSplit(forfeit, split, type);
                player.playing = false;
                break;
            default:
                break;

        }
    }

    void updateSplits() throws Exception {
        String path;
        if (useLiveApi) path = "live_data.json"; else path = "spectate_match.json";

        JSONObject spectate = Api.readJSON(Paths.get(path).toFile());

        if (spectate == null) throw new Exception();

        JSONArray players = (JSONArray) spectate.opt("players");
        JSONArray timelines = (JSONArray) spectate.opt("timelines");

        if (players == null || timelines == null) throw new Exception();

        updatePlaying(spectate);
        loadUuid(players);
        clearSplits();
        loadSplits(timelines);
    }

    void updateAdv() throws Exception {
        String path;
        if (useLiveApi) path = "live_data.json"; else path = "spectate_match.json";

        JSONObject spectate = Api.readJSON(Paths.get(path).toFile());

        if (spectate == null) throw new Exception();

        JSONArray players = (JSONArray) spectate.opt("players");
        JSONArray timelines = (JSONArray) spectate.opt("timelines");

        if (players == null || timelines == null) throw new Exception();

        updatePlaying(spectate);
        loadUuid(players);
        clearAdv();
        loadAdv(timelines);
        writeAdvToFiles(players);
    }

    void writeAdvToFiles(JSONArray players) {
        for (int i = 0; i < players.length(); i++) {
            Player p = getPlayer(players.getJSONObject(i).getString("nickname"));
            File folder = Paths.get("adv_count").toFile();
            if (!folder.exists()) folder.mkdirs();

            if (p != null) {
                File playerFile = Paths.get("adv_count", p.name + ".txt").toFile();
                try {
                    BufferedWriter w = new BufferedWriter(new FileWriter(playerFile));
                    w.write(p.advCount + " adv");
                    w.close();
                } catch (IOException e) {}
            }
        }
    }

    void loadUuid(JSONArray pls) {
        boolean assignedIds = true;

        for (int i = 0; i < players.length; i++) if (players[i].uuid == null) {
            assignedIds = false;
            break;
        }

        if (!assignedIds) for (int i = 0; i < pls.length(); i++) {
            JSONObject player = (JSONObject) pls.get(i);
            String name = player.getString("nickname");
            String uuid = player.getString("uuid");
            for (int b = 0; b < players.length; b++) {
                if (players[b].name.equals(name)) players[b].uuid = uuid;
            }
        }
    }

    void loadSplits(JSONArray timelines) {
        if (Main.top) {
            for (int i = 0; i < timelines.length(); i++) {
                JSONObject ss = (JSONObject) timelines.get(i);
                Data.SplitType type = ident(ss);
                String uuid = ss.getString("uuid");
                int time = ss.getInt("time");

                String name = getName(uuid);

                Split split = new Split(name, time);

                if (type != Data.SplitType.other) {
                    addSplit(type, split);
                }
            }
        } else {
            // READ TIMELINE IN REVERSE IF THAT'S SELECTED
            for (int i = timelines.length() - 1; i >= 0; i--) {
                JSONObject ss = (JSONObject) timelines.get(i);
                Data.SplitType type = ident(ss);
                String uuid = ss.getString("uuid");
                int time = ss.getInt("time");

                String name = getName(uuid);

                Split split = new Split(name, time);

                if (type != Data.SplitType.other) {
                    addSplit(type, split);
                }
            }
        }
    }

    void loadAdv(JSONArray timelines) {
        for (int i = 0; i < timelines.length(); i++) {
            JSONObject o = (JSONObject) timelines.get(i);
            Data.SplitType type = ident(o);
            String uuid = o.getString("uuid");
            boolean adv = isAdv(o);
            String name = getName(uuid);

            if (type == Data.SplitType.reset) {
                if (getPlayer(name) != null) getPlayer(name).advCount = 0;
            }

            if (adv && getPlayer(name) != null) getPlayer(name).advCount++;
        }
    }

    void addSplit(Split[] array, Split split, SplitType type) {
        Split[] newArray = new Split[array.length + 1];
        for (int i = 0; i < array.length; i++) newArray[i] = array[i];
        newArray[array.length] = split;
        switch (type) {
            case bastion:
                bastion = newArray;
                break;
            case blind:
                blind = newArray;
                break;
            case end:
                end = newArray;
                break;
            case fortress:
                fortress = newArray;
                break;
            case firstrod:
                firstrod = newArray;
                break;
            case nether:
                nether = newArray;
                break;
            case stronghold:
                stronghold = newArray;
                break;
            case finish:
                finish = newArray;
                break;
            case forfeit:
                forfeit = newArray;
                break;
            default:
                break;

        }
    }

    String getName(String uuid) {
        for (int i = 0; i < players.length; i++) {
            if (players[i].uuid != null && players[i].uuid.equals(uuid)) return players[i].name;
        }

        return null;
    }

    Player getPlayer(String name) {
        if (name == null) return null;
        for (int i = 0; i < players.length; i++) if (name.equals(players[i].name)) return players[i];
        return null;
    }

    Player getPlayerById(int id) {
        if (id < 0) return null;
        for (int i = 0; i < players.length; i++) if (id == players[i].id) return players[i];
        return null;
    }

    void addPlayer(String name, String twitch) {
        Player player = new Player(name, twitch, players.length, new JSONArray(), headsize, name, "");
        addPlayer(player);
    }

    void addPlayer(Player player) {
        Player[] newPlayers = new Player[players.length + 1];
        for (int i = 0; i < players.length; i++) newPlayers[i] = players[i];
        newPlayers[players.length] = player;
        players = newPlayers;
    }

    void toggleLive(int index) {
        if (index < 0) return;
        if (index < players.length) if (players[index].live) players[index].live = false; else players[index].live = true;
        System.out.println("Live = " + players[index].live);
    }

    void editTwitch(int index, String twitch) {
        if (index < players.length) players[index].twitch = twitch;
        if (!twitch.equals("none")) players[index].live = true; else players[index].twitch = "";
    }
}