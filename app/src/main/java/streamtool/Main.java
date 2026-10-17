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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import common.Api;
import common.FileSelect;

public class Main {

    static Long abDelay = 0L;
    static boolean top = true;

    static boolean seedDone = false;

    public static void main(String[] args) throws URISyntaxException, InterruptedException, IOException {

        Files.createDirectories(Paths.get("lb_data", "matches"));

        Files.createDirectories(Paths.get("heads"));
        Files.createDirectories(Paths.get("output"));

        RuntimeData run = new RuntimeData();

        int[] overrides = {-1, -1, -1, -1, -1};
        run.overrides = overrides;

        run.obsLayout = Api.readJSON(Paths.get("obs_layout.json").toFile());

        JSONObject liveApiConfig = Api.readJSON(Paths.get("liveapi_config.json").toFile());

        run.updateInterval = run.obsLayout.optLong("updateInterval", 1000L);

        run.portRange = run.obsLayout.getInt("portRange"); // ports for streamlink feeds, they are this value + playerId
        run.scenes = (JSONObject) run.obsLayout.get("scenes");
        run.audio = (JSONObject) run.obsLayout.get("audio");
        run.paths = (JSONObject) run.obsLayout.get("paths");

        abDelay = run.obsLayout.optLong("switchDelay", 0L);
        run.mostPercent = run.obsLayout.optBoolean("mostPercent", false);

        run.imagePath = (String) run.paths.get("heads");
        run.seediconPath = (String) run.paths.get("seed_icons");
        run.seedimagePath = (String) run.paths.get("seed_images");
        run.imageGenPath = (String) run.paths.get("gen_images");

        run.seedList = new JSONObject();

        run.enableSocket = true;
        run.muted = true;

        if (args.length > 0) run.enableSocket = false;

        run.fs = new FileSelect();

        run.client = new ClientHandler(new URI("ws://127.0.0.1:4455"), run);
        run.client.connect();

        run.scanner = new Scanner(System.in);

        run.client.send("{\"op\": 1, \"d\": {\"rpcVersion\": 1, \"eventSubscriptions\": 0}}");

        Data data;

        if (liveApiConfig != null && liveApiConfig.getBoolean("enabled")) {
            data = new Data(run.mostPercent, liveApiConfig.getString("privateKey"), liveApiConfig.getString("username"));
            run.updateInterval = liveApiConfig.optLong("updateInterval", 1000L);
        } else {
            data = new Data(run.mostPercent);
        }

        if (run.mostPercent) run.mp = new MostPercentLeaderboard(data, 5);

        run.hostName = "";

        run.needSave = false;

        run.commandParser = new CommandParser(data);
        run.commandManager = new CommandManager(data, run);

        run.snapshot = new Snapshot();
        run.autoUpdate = new AutoUpdate(run);
        run.timer = new Timer(run);

        while(!run.stop) {

            if (run.needSave) {
                System.out.println("Saving data");
                saveData(data, run);
                run.needSave = false;
            }

            System.out.println();
            System.out.print("> ");
            String s = run.scanner.nextLine();

            JSONObject commandObject = run.commandParser.getCommand(s);

            System.out.println();

            int invalid = commandObject.getInt("invalid");
            System.out.println("invalid = " + invalid);

            if (invalid == 0) {
                // print identified command
                String command = commandObject.getString("command");
                System.out.println("command = " + command);

                // attempt to execute command and report
                int success = run.commandManager.execute(commandObject);
                System.out.println("success = " + success);
            }

            if (invalid == 1) System.out.println("Unknown command");
            if (invalid == 2) System.out.println("Syntax error");

        }

        run.scanner.close();
        run.snapshot.stop();
        run.timer.stop();
        Streamlink.killAll();

        run.client.close();
    }

    static void downloadOrder(int leagueNumber, int weekNumber) throws MalformedURLException, IOException, URISyntaxException {
        File file = Paths.get("seed_order.json").toFile();
        
        BufferedInputStream in = new BufferedInputStream(new URI("https://pastel-shrimp-251.convex.site/api/seeds/order?leagueNumber=" + leagueNumber).toURL().openStream());
        FileOutputStream out = new FileOutputStream(file);
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }
        out.close();

        JSONArray apiData = Api.readJSONArray(file);
        JSONArray data = new JSONArray();

        for (int i = 0; i < apiData.length(); i++) {
            JSONObject api = apiData.getJSONObject(i);
            JSONObject seed = new JSONObject();
            seed.put("number", api.getInt("order"));
            seed.put("type", api.getString("type").toLowerCase());

            data.put(seed);
        }

        JSONObject seedList = new JSONObject();

        int seedcount = getSeedcount(leagueNumber);
        int timelimit = getTimeLimit(leagueNumber);

        seedList.put("league", leagueNumber);
        seedList.put("week", weekNumber);
        seedList.put("seedcount", seedcount);
        seedList.put("timelimit", timelimit);
        seedList.put("data", data);

        BufferedWriter w = new BufferedWriter(new FileWriter(file));
        w.write(seedList.toString());
        w.close();
    }

    static void unHide(Data data) {
        for (int i = 0; i < data.players.length; i++) {
            data.players[i].hideSplit = -1;
        }
    }

    static void setMute(ClientHandler client, JSONObject audio, boolean muted) {
        String mic = audio.getString("mic");
        String disc = audio.getString("disc");

        boolean micEnabled = audio.getBoolean("micEnabled");
        boolean discEnabled = audio.getBoolean("discEnabled");

        if (micEnabled) client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputMute\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + mic + "\", \"inputMuted\": " + muted + "}}}");
        if (discEnabled) client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputMute\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + disc + "\", \"inputMuted\": " + muted + "}}}");

        System.out.println("Muted = " + muted);
    }

    static int getTimeLimit(int leagueNumber) {
        if (leagueNumber == 1) return 13;
        if (leagueNumber == 2) return 15;
        if (leagueNumber == 3) return 17;
        if (leagueNumber == 4) return 20;
        if (leagueNumber == 5) return 25;
        if (leagueNumber == 6) return 30;
        return 60;
    }

    static int getSeedcount(int leagueNumber) {
        if (leagueNumber == 1) return 8;
        if (leagueNumber == 2) return 8;
        if (leagueNumber == 3) return 8;
        if (leagueNumber == 4) return 6;
        if (leagueNumber == 5) return 5;
        if (leagueNumber == 6) return 5;
        return 2;
    }

    public static String fixLink(String twitch) {
        String result = twitch;
        while(result.contains("/")) {
            result = result.substring(1);
        }
        return result;
    }

    static void setBackground(Data data, RuntimeData run) {
        for (int i = 1; i <= 6; i++) {
            boolean visible = false;
            if (i == data.leagueNumber) visible = true;
            run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetSceneItemEnabled\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + "background\", \"sceneItemId\": " + (run.obsLayout.getJSONObject("bgIds")).optInt("l" + i, 0) + ", \"sceneItemEnabled\": " + visible + "}}}");
        }
    }

    static void saveData(Data data, RuntimeData run) throws IOException {

        Player[] players = data.players;
        int leagueNumber = data.leagueNumber;
        int weekNumber = data.weekNumber;

        int[] matchIds = run.matchIds;

        File file = Paths.get("runtime_data.json").toFile();
        JSONObject object = new JSONObject();
        JSONArray playerList = new JSONArray();

        for (int i = 0; i < players.length; i++) {
            JSONObject pl = new JSONObject();
            pl.put("id", players[i].id);
            pl.put("name", players[i].name);
            pl.put("twitch", players[i].twitch);
            pl.put("displayName", players[i].displayName);
            pl.put("pronouns", players[i].pronouns);
            pl.put("live", players[i].live);
            pl.put("history", players[i].history);
            playerList.put(pl);
        }

        JSONArray idList = new JSONArray();
        for (int i = 0; i < matchIds.length; i++) idList.put(matchIds[i]);

        object.put("league", leagueNumber);
        object.put("week", weekNumber);
        object.put("players", playerList);
        object.put("seedList", run.seedList);
        object.put("currentSeed", data.currentSeed);
        object.put("host", run.hostName);
        object.put("matchIds", idList);

        if (run.mostPercent) {
            JSONArray mpIds = new JSONArray();
            for (int i = 0; i < run.mp.ids.size(); i++) {
                mpIds.put(run.mp.ids.get(i));
            }
            object.put("mpIds", mpIds);
        }

        BufferedWriter w = new BufferedWriter(new FileWriter(file));
        w.write(object.toString());
        w.close();
    }

    static JSONObject getData() {
        File file = Paths.get("runtime_data.json").toFile();
        JSONObject object = Api.readJSON(file);
        return object;
    }

    static String getTwitch(String username) {
        String result;

        try {
            result = getTwitchFromRanked(username);
        } catch (MalformedURLException e){
            result = null;
        } catch (IOException e) {
            result = null;
        } catch (URISyntaxException e) {
            result = null;
        }

        return result;
    }

    static String getTwitchFromRanked(String username) throws MalformedURLException, IOException, URISyntaxException {
        File file = Paths.get("player_request.json").toFile();

        BufferedInputStream in = new BufferedInputStream(new URI("https://api.mcsrranked.com/users/" + username).toURL().openStream());
        FileOutputStream out = new FileOutputStream(file);
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }
        out.close();

        JSONObject user = Api.readJSON(file);
        if (user == null) return null;
        String status = user.getString("status");
        if (status != null && status.equals("success")) {
            JSONObject data = user.getJSONObject("data");
            JSONObject connections;
            try {
                connections = data.getJSONObject("connections");
            } catch (JSONException e) {
                connections = null;
            }
            if (connections != null) {
                JSONObject twitch;
                try {
                    twitch = connections.getJSONObject("twitch");
                } catch (JSONException e) {
                    twitch = null;
                }
                if (twitch != null) {
                    String id;
                    try {
                        id = twitch.getString("id");
                    } catch (JSONException e) {
                        id = null;
                    }
                    if (id != null) return id;
                }
            }
        }

        return null;
    }

    public static String getTimeString(int ms, boolean includeMs) {
        String result = "";

        if (ms > 0) {
            int minutes = ms / 60000;
            int seconds = (ms / 1000) - (minutes * 60);
            int milliseconds = ms - (minutes * 60000) - (seconds * 1000);

            if (minutes < 10) result = result + "0" + minutes; else result = result + minutes;
            if (seconds < 10) result = result + ":0" + seconds; else result = result + ":" + seconds;
            if (includeMs) {
                if (milliseconds < 10) result = result + ".00" + milliseconds; else if (milliseconds < 100) result = result + ".0" + milliseconds; else result = result + "." + milliseconds;
            }
        }

        return result;
    }

}

class Player {

    String name;
    String twitch;
    boolean live;
    String uuid;
    int id;

    String displayName;
    String pronouns;

    JSONArray history;

    int lb_points = 0;
    boolean lb_played = false;
    int lb_comps = 0;
    int lb_time = 0;
    int lb_adv = 0;

    int hideSplit = -1; // not hide = -1, hide = {0, 1, 2, 3, 4, 5, 6, 7} for the specific split, once they get the next one they're shown again
    
    boolean playing = false;
    int advCount = 0;

    public Player(String name, String twitch, int id, JSONArray history, int headsize, String displayName, String pronouns) {
        this.name = name;
        this.twitch = twitch;
        this.id = id;
        this.displayName = displayName;
        this.pronouns = pronouns;

        this.history = history;

        if (twitch.equals("none")) twitch = "";
        if (twitch.equals("")) live = false; else live = true;

        try {
            if (Api.updateSkin(name)) Api.dlSkin(name);
            Api.getHead(name, headsize);
        } catch (Exception e) {}
    }
}

class Split {
    String player;
    int time;

    public Split(String player, int time) {
        this.player = player;
        this.time = time;
    }
}

class RuntimeData {
    public Timer timer;
    public Snapshot snapshot;
    CommandManager commandManager;
    CommandParser commandParser;

    Scanner scanner;
    boolean needSave;
    String hostName;

    boolean stop = false;

    boolean mainspec = false;

    int[] overrides;

    JSONObject obsLayout;

    int portRange;
    boolean useStreamlink = true;

    JSONObject scenes;
    JSONObject audio;
    JSONObject paths;

    String imagePath;
    String seediconPath;
    String seedimagePath;
    String imageGenPath;

    JSONObject seedList;

    boolean enableSocket;
    boolean muted;

    boolean spectating = false;

    boolean mostPercent;
    MostPercentLeaderboard mp;

    ClientHandler client;
    FileSelect fs;

    Long updateInterval;

    int[] matchIds = new int[0];
    public AutoUpdate autoUpdate;

    void clearMatchIds(int count) {
        int[] ids = new int[count];
        for (int i = 0; i < ids.length; i++) ids[i] = -1;
        matchIds = ids;
    }

    int getMatchId(int seedNumber) {
        if (seedNumber < 1 || seedNumber > matchIds.length) return -1;
        return matchIds[seedNumber - 1];
    }

    boolean setMatchId(int seedNumber, int matchId) {
        if (seedNumber < 1 || seedNumber > matchIds.length) return false;
        matchIds[seedNumber - 1] = matchId;
        return true;
    }
}