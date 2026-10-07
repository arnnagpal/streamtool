package streamtool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

import common.Api;

public class Comp {
    public static JSONObject getCompletions(Player[] regPlayers, int seedNumber, RuntimeData run, Data data) {
        int completionPoints = regPlayers.length / 2;

        if (run.overrides[4] != -1) completionPoints = run.overrides[4] - 5;

        File file;
        String compKey = "completes";
        String uuidKey = "player";
        boolean needData = false;

        int matchId;

        if (seedNumber > 0) {
            matchId = run.getMatchId(seedNumber);

            if (matchId == -1) {
                System.out.println("matchId not found");
                return null;
            }

            file = Paths.get("lb_data", "matches", matchId+".json").toFile();

            compKey = "completions";
            uuidKey = "uuid";
            needData = true;
        } else file = Paths.get("spectate_match.json").toFile();

        if (!file.exists()) return null;

        JSONObject o = Api.readJSON(file);
        if (needData) o = (JSONObject) o.get("data");
        JSONArray comp = (JSONArray) o.get(compKey);
        JSONArray players = (JSONArray) o.get("players");

        for (int i = 0; i < comp.length(); i++) {
            JSONObject pl3 = (JSONObject) comp.get(i);
            if (!isRegistered(getName((String) pl3.get(uuidKey), players), regPlayers)) {
                System.out.println("Removed " + getName((String) pl3.get(uuidKey), players));
                comp.remove(i);
                i--;
            }
        }

        JSONArray completionsJson = new JSONArray();

        for (int i = 0; i < comp.length(); i++) {
            JSONObject o2 = (JSONObject) comp.get(i);
            String uuid = (String) o2.get(uuidKey);
            int time = (int) o2.get("time");
            int seconds = time / 1000;
            int minutes = seconds / 60;
            seconds = seconds - (minutes * 60);
            int ms = time - (minutes * 60000) - (seconds * 1000);
            String minuteString;
            String secondString;
            String msString;
            if (minutes < 10) minuteString = "0" + minutes; else minuteString = "" + minutes;
            if (seconds < 10) secondString = "0" + seconds; else secondString = "" + seconds;
            if (ms < 10) msString = "00" + ms; else if (ms < 100) msString = "0" + ms; else msString = "" + ms;
            String timeString = minuteString + ":" + secondString + "." + msString;
            int points = 0;
            if (i < completionPoints) points += completionPoints - i;
            if (i == 0) points += 5;
            if (i == 1) points += 3;
            if (i == 2) points += 1;
            if (points <= 0) points = 1;
            String pointString = "" + points;
            String name = getName(uuid, players);

            JSONObject co = new JSONObject();

            System.out.println(name + " " + timeString + " " + pointString);

            co.put("name", name);
            co.put("time", timeString);
            co.put("points", pointString);

            completionsJson.put(co);
        }

        JSONObject returnData = new JSONObject();
        returnData.put("completions", completionsJson);
        returnData.put("seedNum", seedNumber);
        if (seedNumber == 0) returnData.put("seedNum", data.currentSeed);
        returnData.put("seedCount", Main.getSeedcount(data.leagueNumber));

        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(Paths.get("lb_data", "completions.json").toFile()));
            w.write(returnData.toString());
            w.close();
        } catch (IOException e) {}

        return returnData;
    }

    public static String getName(String uuid, JSONArray players) {
        String output = "";

        for (int i = 0; i < players.length(); i++) {
            JSONObject pl = (JSONObject) players.get(i);
            String match = (String) pl.get("uuid");
            if (uuid.equals(match)) output = (String) pl.get("nickname");
        }

        return output;

    }

    public static boolean isRegistered(String name, Player[] regPlayers) {
        for (int i = 0; i < regPlayers.length; i++) if (name.equals(regPlayers[i].name)) return true;
        return false;
    }
}
