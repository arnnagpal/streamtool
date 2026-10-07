package streamtool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.json.JSONArray;
import org.json.JSONObject;

import common.Api;

public class MostPercentLeaderboard {
    ArrayList<Integer> ids = new ArrayList<>();
    ArrayList<Player> list = new ArrayList<>();
    Data data;
    int peakCount = 0;
    int seedcount = 0;

    public MostPercentLeaderboard(Data data, int seedcount) {
        this.data = data;
        this.seedcount = seedcount;
    }

    void addId(int id) {
        try {
            Api.downloadSeedUnsafe(id);
        } catch (IOException | URISyntaxException e) {
            System.out.println("Error downloading seed");
            return;
        }

        ids.add(id);
    }

    void printIds() {
        for (int i = 0; i < ids.size(); i++) {
            System.out.println("Match " + (i + 1) + ": " + ids.get(i));
        }
    }

    void clearScores() {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).lb_points = 0;
            list.get(i).lb_adv = 0;
        }
    }

    void clearAllData() {
        clearScores();
        list = new ArrayList<>();
        ids = new ArrayList<>();
        peakCount = 0;
    }

    void addMatch(int id) {

        addId(id);

        File matchFile = Paths.get("lb_data","matches",id + ".json").toFile();
        if (!matchFile.exists()) return;

        JSONObject match = Api.readJSON(matchFile);
        JSONObject d = match.getJSONObject("data");
        JSONArray pl = d.getJSONArray("players");
        int count = 0;

        for (int i = 0; i < pl.length(); i++) {
            Player p = data.getPlayer(pl.getJSONObject(i).getString("nickname"));
            if (p != null) {
                p.uuid = pl.getJSONObject(i).getString("uuid");
                if (!list.contains(p)) list.add(p);
                count++;
            }
        }

        if (count > peakCount) peakCount = count;
    }

    JSONObject getLeaderboard() {

        JSONObject result = new JSONObject();
        JSONArray players = new JSONArray();

        for (int i = 0; i < data.players.length; i++) {
            data.players[i].lb_points = 0;
            data.players[i].lb_adv = 0;
        }

        for (int i = 0; i < ids.size(); i++) {
            JSONArray a = getResult(ids.get(i), i + 1).getJSONArray("completions");

            for (int ai = 0; ai < a.length(); ai++) {
                Player p = data.getPlayer(a.getJSONObject(ai).getString("name"));

                if (p != null) {
                    p.lb_points += peakCount - ai - 1;
                    p.lb_adv += a.getJSONObject(ai).getInt("advancements");
                }
            }
        }

        Collections.sort(list, new Comparator<Player>() {
            public int compare(Player a, Player b) {
                if (a.lb_points > b.lb_points) return -1;
                if (b.lb_points > a.lb_points) return 1;
                if (a.lb_adv > b.lb_adv) return -1;
                if (b.lb_adv > a.lb_adv) return 1;
                return 0;
            }
        });

        for (int i = 0; i < list.size(); i++) {
            JSONObject o = new JSONObject();
            o.put("name", list.get(i).name);
            o.put("points", list.get(i).lb_points);
            o.put("average", list.get(i).lb_adv + " adv");

            players.put(o);
        }

        result.put("players", players);

        return result;
    }

    JSONObject getResult(int matchNumber) {
        if (matchNumber > ids.size()) return null;
        return getResult(ids.get(matchNumber - 1), matchNumber);
    }

    JSONObject getResult(int id, int matchNumber) {
        JSONObject result = new JSONObject();

        JSONArray players = new JSONArray();

        File matchFile = Paths.get("lb_data","matches",id + ".json").toFile();
        if (!matchFile.exists()) {
            System.out.println("Match " + id + " file not found");
            return null;
        }

        JSONObject match = Api.readJSON(matchFile);
        JSONObject d = match.getJSONObject("data");

        JSONObject winner = new JSONObject();
        String winnerName = data.getName(d.getJSONObject("result").getString("uuid"));
        winner.put("name", winnerName);
        players.put(winner);

        JSONArray timelines = d.getJSONArray("timelines");

        data.clearAdv();

        for (int i = 0; i < timelines.length(); i++) {
            JSONObject o = timelines.getJSONObject(i);
            Player p = data.getPlayer(data.getName(o.getString("uuid")));

            if (o.getString("type").equals("projectelo.timeline.eliminate") || data.ident(o) == Data.SplitType.forfeit) {
                if (p != null) {
                    JSONObject elim = new JSONObject();
                    String name = data.getName(o.getString("uuid"));
                    elim.put("name", name);
                    players.put(elim);
                } else {
                    System.out.println("Unknown player: " + o.getString("uuid"));
                }
            }
        }

        for (int i = timelines.length() - 1; i >= 0; i--) {
            JSONObject o = timelines.getJSONObject(i);
            Player p = data.getPlayer(data.getName(o.getString("uuid")));

            if (data.ident(o) == Data.SplitType.reset && p != null) {
                p.advCount = 0;
            }

            if (data.isAdv(o) && p != null) {
                p.advCount++;
            }
        }

        for (int i = 0; i < players.length(); i++) {
            JSONObject o = players.getJSONObject(i);

            o.put("advancements", data.getPlayer(o.getString("name")).advCount);
            o.put("time", o.getInt("advancements") + " adv");

            players.put(i, o);
        }

        result.put("completions", players);
        result.put("seedCount", seedcount);
        result.put("seedNum", matchNumber);


        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(Paths.get("lb_data","result" + matchNumber + ".json").toFile()));
            w.write(result.toString());
            w.close();
        } catch (IOException e) {}

        return result;
    }
}
