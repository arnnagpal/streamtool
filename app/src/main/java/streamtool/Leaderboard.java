package streamtool;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import common.Api;
import common.Fraction;

public class Leaderboard {

    public static boolean downloadSeed(int seedNumber, int matchId, RuntimeData run) {
        try {
            Api.downloadSeedUnsafe(matchId);
            boolean setId = run.setMatchId(seedNumber, matchId);
            if (!setId) {
                System.out.println("Failed to save match id");
                return false;
            }
        } catch (Exception e) {
            System.out.println("Download error");
            return false;
        }

        return true;
    }

    public static int[] getMatchIds(String host, int count) throws MalformedURLException, IOException, URISyntaxException {
        int[] result = new int[count];

        BufferedInputStream in = new BufferedInputStream(new URI("https://api.mcsrranked.com/users/" + host + "/matches?type=3&count=" + count).toURL().openStream());
        File file = Paths.get("lb_data", "id_request.json").toFile();
        FileOutputStream out = new FileOutputStream(file);
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }
        out.close();

        JSONObject idRequest = Api.readJSON(file);

        String status = (String) idRequest.get("status");
        if (!status.equals("success")) return null;

        JSONArray data = (JSONArray) idRequest.get("data");

        for (int i = 0; i < result.length; i++) result[i] = data.getJSONObject(i).getInt("id");

        return result;
    }

    public static boolean genLeaderboard(int seedcount, Data data, RuntimeData run) {

        Player[] regList = data.players;

        int playersWithPoints = 0;

        int completionPoints = regList.length / 2;

        if (run.overrides[4] != -1) completionPoints = run.overrides[4] - 5;

        if (completionPoints < 0) completionPoints = 0;

        for (int i = 0; i < regList.length; i++) {
            regList[i].lb_points = 0;
            regList[i].lb_comps = 0;
            regList[i].lb_played = false;
            regList[i].lb_time = 0;
        }

        for (int i = 1; i <= seedcount; i++) {

            int matchId = run.getMatchId(i);

            if (matchId == -1) {
                return false;
            }

            File file = Paths.get("lb_data", "matches", matchId+".json").toFile();

            String compKey = "completions";
            String uuidKey = "uuid";

            JSONObject o = Api.readJSON(file);
            o = (JSONObject) o.get("data");
            JSONArray comp = (JSONArray) o.get(compKey);
            JSONArray players = (JSONArray) o.get("players");

            // remove players who are not registered
            for (int eee = 0; eee < players.length(); eee++) {
                JSONObject pl4 = (JSONObject) players.get(eee);
                if (!Comp.isRegistered((String) pl4.get("nickname"), regList)) {
                    players.remove(eee);
                    eee--;
                }
            }

            // mark players as having played
            for (int aw = 0; aw < players.length(); aw++) {
                JSONObject pl5 = (JSONObject) players.get(aw);
                Player player = data.getPlayer((String) pl5.get("nickname"));
                player.lb_played = true;
            }

            // remove completions from players who are not registered
            for (int i2 = 0; i2 < comp.length(); i2++) {
                JSONObject pl3 = (JSONObject) comp.get(i2);
                if (data.getName(pl3.getString(uuidKey)) == null) {
                    comp.remove(i2);
                    i2--;
                }
            }

            // add points to players
            for (int num = 0; num < comp.length(); num++) {
                JSONObject pl = (JSONObject) comp.get(num);
                Player player = data.getPlayer(data.getName(pl.getString(uuidKey)));
                int points = 0;
                if (num == 0) points += 5;
                if (num == 1) points += 3;
                if (num == 2) points += 1;
                if (completionPoints > num) points += completionPoints - num;
                if (points == 0) points = 1;
                player.lb_points += points;
                player.lb_comps++;
                player.lb_played = true;
                player.lb_time += (int) pl.get("time");
            }
        }

        JSONObject leaderboard = new JSONObject();

        leaderboard.put("league", data.leagueNumber);
        leaderboard.put("week", data.weekNumber);

        JSONArray lbPlayers = new JSONArray();

        for (int i = 0; i < regList.length; i++) if (regList[i].lb_played && regList[i].lb_points > 0) playersWithPoints++;

        for (int i = 0; i < regList.length; i++) {
            if (regList[i].lb_played) {

                int average = regList[i].lb_time;
                int comps = regList[i].lb_comps;
                int timeLimit = getTimeLimit(data.leagueNumber);
                if (comps < seedcount) average += timeLimit * (seedcount - comps);

                average *= 10;
                average /= seedcount;
                average += 5;
                average /= 10;

                int avgMin = average / 60000;
                int avgSec = (average / 1000) - (avgMin * 60);
                int avgMs = average - (avgMin * 60000) - (avgSec * 1000);

                String avgMinStr, avgSecStr, avgMsStr;
                if (avgMin < 10) avgMinStr = "0" + avgMin; else avgMinStr = "" + avgMin;
                if (avgSec < 10) avgSecStr = "0" + avgSec; else avgSecStr = "" + avgSec;
                if (avgMs < 10) avgMsStr = "00" + avgMs; else if (avgMs < 100) avgMsStr = "0" + avgMs; else avgMsStr = "" + avgMs;


                String avgStr = avgMinStr + ":" + avgSecStr + "." + avgMsStr;

                JSONObject player = new JSONObject();

                player.put("name", regList[i].name);
                player.put("points", regList[i].lb_points);
                player.put("avg_ms", average);
                player.put("average", avgStr);

                lbPlayers.put(player);
            }            
        }

        List<JSONObject> jsonList = new ArrayList<>();
        for (int a = 0; a < lbPlayers.length(); a++) jsonList.add((JSONObject) lbPlayers.get(a));

        Collections.sort(jsonList, new Comparator<JSONObject>() {
            public int compare(JSONObject a, JSONObject b) {
                int aaa = Integer.compare(b.getInt("points"), a.getInt("points"));
                if (aaa != 0) return aaa; else return Integer.compare(a.getInt("avg_ms"), b.getInt("avg_ms"));
            }
        });

        JSONArray sortedLeaderboard = new JSONArray();
        for (JSONObject obj : jsonList) {
            obj.remove("avg_ms");
            sortedLeaderboard.put(obj);
        }

        for (int i = 0; i < sortedLeaderboard.length(); i++) {
            int revRank = playersWithPoints - i;
            if (revRank < 0) revRank = 0;
            int perf = (int) Math.round((revRank * 1000.0) / playersWithPoints);
            String s = perf + "";
            int l = s.length();
            if (l == 1) {
                s = "00";
                l = 2;
            }

            String p = s.substring(0, l-1) + "." + s.substring(l-1) + "%";
            String f = revRank + "/" + playersWithPoints;

            sortedLeaderboard.getJSONObject(i).put("perfFraction", f);
            sortedLeaderboard.getJSONObject(i).put("perfRounded", p);
        }

        leaderboard.put("players", sortedLeaderboard);

        leaderboard.put("promotions", getPromotions(data.leagueNumber, sortedLeaderboard.length()));
        if (run.overrides[0] > 0) leaderboard.put("promotions", run.overrides[0]);

        leaderboard.put("demotions", getDemotions(data.leagueNumber, sortedLeaderboard.length()));
        if (run.overrides[1] > 0) leaderboard.put("demotions", run.overrides[1]);

        File file = Paths.get("lb_data", "leaderboard.json").toFile();

        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(file));
            w.write(leaderboard.toString());
            w.close();
        } catch (IOException e) {
            System.out.println("Failed to write leaderboard");
            return false;
        }

        return true;
    }

    static int getTimeLimit(int leagueNumber) {
        if (leagueNumber == 1) return 780000;
        if (leagueNumber == 2) return 900000;
        if (leagueNumber == 3) return 1020000;
        if (leagueNumber == 4) return 1200000;
        if (leagueNumber == 5) return 1500000;
        if (leagueNumber == 6) return 1800000;
        if (leagueNumber == 7) return 3600000;
        return 0;
    }

    static int getPromotions(int leagueNumber, int playerCount) {
        if (leagueNumber == 1) return 0;
        int result = playerCount * 15 + 50;
        return result / 100;
    }

    static int getDemotions(int leagueNumber, int playerCount) {
        if (leagueNumber == 6) return 0;
        int result = playerCount * 15 + 50;
        return result / 100;
    }

    public static JSONObject genAvgLeaderboard(JSONObject lb, Data data, RuntimeData run, boolean initial) {
        JSONArray board = lb.getJSONArray("players");
        ArrayList<JSONObject> avgBoard = new ArrayList<>();

        for (int i = 0; i < board.length(); i++) {
            JSONObject o = board.getJSONObject(i);

            String name = o.getString("name");
            Double current = new Fraction(o.getString("perfFraction")).getDouble();

            Player player = data.getPlayer(name);
            ArrayList<Double> history = new ArrayList<>();

            String weeks = "";

            if (player != null) {
                int j = 2;
                for (int i2 = player.history.length() - 1; i2 >= 0; i2--) {
                    history.add(player.history.optDouble(i2, -1.0));
                    j--;
                    if (j < 1) break;
                }
            }

            for (int i2 = 0; i2 < history.size(); i2++) {
                if (history.get(i2) < 0) {
                    history.remove(i2);
                    i2--;
                }
            }

            int div = history.size() + 1;

            Double sum = current;

            weeks = div + "w";

            if (initial) {
                sum = 0.0;
                div--;

                weeks = div + "w";

                if (div == 0) {
                    weeks = "";
                    div = 1;
                }
            }

            for (int i2 = 0; i2 < history.size(); i2++) {
                sum += history.get(i2);
            }

            sum /= div;

            if (!initial && i == 0) {
                sum = 100.0;
                weeks = "";
            }

            DecimalFormat formatter = new DecimalFormat("#0.0", DecimalFormatSymbols.getInstance( Locale.ENGLISH ));

            String perfMulti = formatter.format(sum) + "%";

            o.put("sum", sum);
            o.put("perfMulti", perfMulti);
            o.put("weeks", weeks);

            avgBoard.add(o);
        }

        avgBoard.sort(new Comparator<JSONObject>() {
            public int compare(JSONObject a, JSONObject b) {
                Double d1 = a.getDouble("sum");
                Double d2 = b.getDouble("sum");

                if (d1 > d2) return -1;
                if (d2 > d1) return 1;

                int w1 = 0; int w2 = 0;

                if (a.optString("weeks", "").equals("1w")) w1 = 1;
                if (a.optString("weeks", "").equals("2w")) w1 = 2;
                if (a.optString("weeks", "").equals("3w")) w1 = 3;
                if (b.optString("weeks", "").equals("1w")) w2 = 1;
                if (b.optString("weeks", "").equals("2w")) w2 = 2;
                if (b.optString("weeks", "").equals("3w")) w2 = 3;

                if (w1 > w2) return -1;
                if (w2 > w1) return 1;


                return 0;
            }
        });

        JSONArray avgLb = new JSONArray();
        for (int i = 0; i < avgBoard.size(); i++) {
            JSONObject o = avgBoard.get(i);
            o.remove("sum");
            avgLb.put(o);
        }

        JSONObject result = new JSONObject();
        result.put("players", avgLb);

        result.put("promotions", lb.getInt("promotions"));
        result.put("demotions", lb.getInt("demotions"));
        result.put("initial", initial);

        File file = Paths.get("lb_data", "multiweek.json").toFile();

        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(file));
            w.write(result.toString());
            w.close();
        } catch (IOException e) {
            System.out.println("Failed to write averages");
        }

        return result;
    }
}
