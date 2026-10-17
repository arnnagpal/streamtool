package streamtool;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

import common.Api;
import common.ImageGen;

public class CommandManager {

    Data data;
    RuntimeData run;

    public CommandManager(Data data, RuntimeData run) {
        this.data = data;
        this.run = run;
    }

    public void executeDelayed(JSONObject commandObject, Long delay) {
        executeDelayed(commandObject, delay, false);
    }

    public void executeDelayed(JSONObject commandObject, Long delay, boolean resetInput) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {}
                execute(commandObject);
                if (resetInput) {
                    System.out.println();
                    System.out.print("> ");
                }
            }
        }).start();
    }

    public int execute(JSONObject commandObject) {
        int invalid = commandObject.getInt("invalid");

        if (invalid != 0) return 0;

        String command = commandObject.getString("command");
        switch (command) {
            case "quit": return quitCommand();
            case "test": return testCommand();
            case "restore": return restoreCommand();
            case "init": return initCommand();
            case "start": return startCommand();
            case "setinfo": return setinfoCommand(commandObject);
            case "reload": return reloadCommand();
            case "override": return overrideCommand(commandObject);
            case "mute": return muteCommand(commandObject);
            case "showtop": return showtopCommand(commandObject);
            case "import": return importCommand(commandObject);
            case "order": return orderCommand(commandObject);
            case "scene": return sceneCommand(commandObject);
            case "next": return nextCommand();
            case "setseed": return setseedCommand(commandObject);
            case "interview": return interviewCommand(commandObject);
            case "show": return showCommand(commandObject);
            case "match": return matchCommand(commandObject);
            case "leaderboard": return leaderboardCommand(commandObject);
            case "killfeeds": return killfeedsCommand();
            case "completions": return completionsCommand(commandObject);
            case "splits": return splitsCommand();
            case "update": return updateCommand(commandObject);
            case "timer": return timerCommand(commandObject);
            case "setlive": return setliveCommand(commandObject);
            case "addplayer": return addplayerCommand(commandObject);
            case "twitch": return twitchCommand(commandObject);
            case "list": return listCommand();
            case "live": return liveCommand();
            case "hide": return hideCommand(commandObject);
            case "unhide": return unhideCommand();
            case "snapshot": return snapshotCommand(commandObject);
            case "refresh": return refreshCommand(commandObject);
            case "mainspec": return mainspecCommand(commandObject);
        }


        return 0;
    }

    int quitCommand() {
        run.stop = true;
        return 1;
    }

    int testCommand() {

        MostPercentLeaderboard mp = new MostPercentLeaderboard(data, 6);

        mp.addMatch(13981770);
        ImageGen.completions(mp.getResult(1));
        ImageGen.leaderboard(mp.getLeaderboard(), 1, 6);

        run.scanner.next();

        mp.addMatch(13981891);
        ImageGen.completions(mp.getResult(2));
        ImageGen.leaderboard(mp.getLeaderboard(), 2, 6);

        run.scanner.next();

        mp.addMatch(13981992);
        ImageGen.completions(mp.getResult(3));
        ImageGen.leaderboard(mp.getLeaderboard(), 3, 6);

        run.scanner.next();

        mp.addMatch(13982099);
        ImageGen.completions(mp.getResult(4));
        ImageGen.leaderboard(mp.getLeaderboard(), 4, 6);

        run.scanner.next();

        mp.addMatch(13982185);
        ImageGen.completions(mp.getResult(5));
        ImageGen.leaderboard(mp.getLeaderboard(), 5, 6);

        run.scanner.next();

        mp.addMatch(13982272);
        ImageGen.completions(mp.getResult(6));
        ImageGen.leaderboard(mp.getLeaderboard(), 6, 6);

        return 1;
    }

    int initCommand() {
        int result = 0;

        // basically just a script to run these 4 commands

        result += run.commandManager.execute(run.commandParser.getCommand("import true"));
        result += run.commandManager.execute(run.commandParser.getCommand("order api"));
        result += run.commandManager.execute(run.commandParser.getCommand("match clear"));
        result += run.commandManager.execute(run.commandParser.getCommand("scene im"));

        return result;
    }

    int startCommand() {
        int result = 0;

        Main.seedDone = false;

        // a script to start timer and enable auto-update

        result += run.commandManager.execute(run.commandParser.getCommand("timer game"));
        result += run.commandManager.execute(run.commandParser.getCommand("update auto"));

        return result;
    }

    int setinfoCommand(JSONObject commandObject) {
        data.leagueNumber = commandObject.getInt("lnum");
        data.weekNumber = commandObject.getInt("wnum");

        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"League\", \"overlay\": true, \"inputSettings\": {\"text\":\"League " + data.leagueNumber + "\"}}}}");
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"Week\", \"overlay\": true, \"inputSettings\": {\"text\":\"Week " + data.weekNumber + "\"}}}}");

        int playerCount = data.players.length;
        int timeLimit = Main.getTimeLimit(data.leagueNumber);

        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"numPlayers\", \"overlay\": true, \"inputSettings\": {\"text\":\"Players: " + playerCount + "\"}}}}");
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"time text\", \"overlay\": true, \"inputSettings\": {\"text\":\"Time limit: " + timeLimit + " minutes\"}}}}");

        Main.setBackground(data, run);

        System.out.println("Set to league " + data.leagueNumber + " week " + data.weekNumber);

        return 1;
    }

    int restoreCommand() {
        System.out.println("Loading previous values");

        JSONObject object = Main.getData();
        run.seedList = object.getJSONObject("seedList");
        data.weekNumber = object.getInt("week");
        data.leagueNumber = object.getInt("league");
        run.hostName = object.getString("host");
        data.currentSeed = object.getInt("currentSeed");

        JSONArray idList = object.getJSONArray("matchIds");
        int[] matchIds = new int[idList.length()];
        for (int i = 0; i < matchIds.length; i++) matchIds[i] = idList.getInt(i);
        run.matchIds = matchIds;

        data.clearPlayers();
        JSONArray array = object.getJSONArray("players");

        int headsize = 100;
        if (run.mostPercent) headsize = 130;

        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            String name = o.getString("name");
            String twitch = o.getString("twitch");
            String displayName = o.getString("displayName");
            String pronouns = o.getString("pronouns");
            int id = o.getInt("id");
            JSONArray history = o.optJSONArray("history", new JSONArray());
            System.out.println(id + " " + name);
            Player player = new Player(name, twitch, id, history, headsize, displayName, pronouns);
            player.live = o.getBoolean("live");
            data.addPlayer(player);
        }

        if (run.mostPercent) {
            JSONArray a = object.getJSONArray("mpIds");
            for (int i = 0; i < a.length(); i++) {
                run.mp.addMatch(a.getInt(i));
            }
        }

        System.out.println("Import complete");
        return 1;
    }

    int reloadCommand() {
        System.out.println("Reloading obs layout");

        run.obsLayout = Api.readJSON(Paths.get("obs_layout.json").toFile());

        run.scenes = (JSONObject) run.obsLayout.get("scenes");
        run.audio = (JSONObject) run.obsLayout.get("audio");
        run.paths = (JSONObject) run.obsLayout.get("paths");

        run.imagePath = (String) run.paths.get("heads");
        run.seediconPath = (String) run.paths.get("seed_icons");
        run.seedimagePath = (String) run.paths.get("seed_images");

        Main.abDelay = run.obsLayout.optLong("switchDelay", 0L);
    
        return 1;
    }

    // "promotions", "demotions", "lb1", "lb2", "points", "reset"
    int overrideCommand(JSONObject commandObject) {

        String var = commandObject.getString("var");

        if (var.equals("reset")) {
            System.out.println("Reset overrides");
            for (int i = 0; i < run.overrides.length; i++) {
                run.overrides[i] = -1;
            }
            return 1;
        }

        int value = commandObject.getInt("value");

        switch (var) {
            case "promotions":
                System.out.println("Set override: " + var + " = " + value);
                run.overrides[0] = value;
                return 1;
            case "demotions":
                System.out.println("Set override: " + var + " = " + value);
                run.overrides[1] = value;
                return 1;
            case "lb1":
                System.out.println("Set override: " + var + " = " + value);
                run.overrides[2] = value;
                return 1;
            case "lb2":
                System.out.println("Set override: " + var + " = " + value);
                run.overrides[3] = value;
                return 1;
            case "points":
                System.out.println("Set override: " + var + " = " + value);
                run.overrides[4] = value;
                return 1;
            default:
                return 0;
        }
    }

    int muteCommand(JSONObject commandObject) {
        int value = commandObject.getInt("value");

        if (value == 0) {
            if (run.muted) run.muted = false; else run.muted = true;
        }
        
        if (value == 1) run.muted = true;
        if (value == 2) run.muted = false;

        Main.setMute(run.client, run.audio, run.muted);
        return 1;
    }

    int mainspecCommand(JSONObject commandObject) {
        int value = commandObject.getInt("value");

        if (value == 0) {
            if (run.mainspec) run.mainspec = false; else run.mainspec = true;
        }
        
        if (value == 1) run.mainspec = true;
        if (value == 2) run.mainspec = false;

        return 1;
    }

    int showtopCommand(JSONObject commandObject) {
        int value = commandObject.getInt("value");

        if (value == 0) {
            if (Main.top) Main.top = false; else Main.top = true;
        }
        
        if (value == 1) Main.top = true;
        if (value == 2) Main.top = false;

        System.out.println("Show top: " + Main.top);
        return 1;
    }

    int importCommand(JSONObject commandObject) {
        run.needSave = true;
        System.out.println("Select file");
        File file = run.fs.select();

        if (file == null) return 0;

        int headsize = 100;
        if (run.mostPercent) headsize = 130;

        if (file.exists()) {
            data.clearPlayers();
            JSONArray array = Api.readJSONArray(file);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                String name = o.getString("ign");
                String twitch = Main.fixLink(o.getString("twitch_username"));
                String displayName = o.getString("display_name");
                String pronouns = o.optString("pronouns", "");

                JSONArray history = o.optJSONArray("history", new JSONArray());
                int id = data.players.length;
                System.out.println(id + " " + name);
                Player player = new Player(name, twitch, id, history, headsize, displayName, pronouns);
                data.addPlayer(player);
            }
            System.out.println("Import complete");
            
        } else {
            System.out.println("Invalid file");
            return 0;
        }

        if (commandObject.getBoolean("value")) {
            String fileName = file.getName();
            String lnum = fileName.substring(5, 6);
            String wnum = fileName.substring(7, 9);

            int leagueNumber;
            int weekNumber;

            try {
                leagueNumber = Integer.parseInt(lnum);
            } catch (NumberFormatException e) {
                System.out.println("NumberFormatException with leagueNumber");
                leagueNumber = 0;
            }
            try {
                weekNumber = Integer.parseInt(wnum);
            } catch (NumberFormatException e) {
                System.out.println("NumberFormatException with weekNumber");
                weekNumber = 0;
            }

            JSONObject co = run.commandParser.getCommand("setinfo " + leagueNumber + " " + weekNumber);
            if (co.getInt("invalid") == 0) {
                run.commandManager.execute(co);
            } else {
                System.out.println("Unable to parse league / week information, update this manually with setinfo");
            }
        } else {
            run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"numPlayers\", \"overlay\": true, \"inputSettings\": {\"text\":\"Players: " + data.players.length + "\"}}}}");
        }

        return 1;
    }

    int orderCommand(JSONObject commandObject) {

        int value = commandObject.getInt("value");
        run.needSave = true;

        // print seed order
        if (value == 0 || value == 4) {
            if (run.seedList.isEmpty()) {
                System.out.println("No seed order has been imported");
                return 0;
            }

            JSONArray array = run.seedList.getJSONArray("data");

            System.out.println("Seed order:");

            // print seed order
            for (int i = 0; i < array.length(); i++) {
                int seed_number = i + 1;
                JSONObject this_seed = (JSONObject) array.get(i);
                String seed_type = (String) this_seed.get("type");

                System.out.println("Seed " + seed_number + " = " + seed_type);
            }


            if (value == 0) return 1;
        }

        // switch to scene
        if (value == 4) {
            orderGenImage();
            run.commandManager.execute(run.commandParser.getCommand("scene order"));
            return 1;
        }

        // skip argument
        if (value == 2) {
            if (run.seedList.isEmpty()) {
                System.out.println("No seed order has been imported");
                return 0;
            }

            int var = commandObject.getInt("var");

            JSONArray array = run.seedList.getJSONArray("data");

            if (var > array.length()) {
                System.out.println("Seed index is out of range");
                return 0;
            }

            // remove the seed and put it at the end
            JSONObject seed = array.getJSONObject(var - 1);
            array.remove(var - 1);
            array.put(seed);
            System.out.println("Skipping seed " + var + " to be played at the end");
            System.out.println("New seed order:");

            // print the seed order
            for (int i = 0; i < array.length(); i++) {
                JSONObject this_seed = (JSONObject) array.get(i);
                String seed_type = (String) this_seed.get("type");

                System.out.println("Seed " + (i + 1) + " = " + seed_type);
            }

            // generate seed order image
            orderGenImage();

            run.seedList.put("data", array);
            return 1;
        }

        File file = null;

        if (value == 1) {
            System.out.println("Select file");
            file = run.fs.select();
        } else {
            try {
                Main.downloadOrder(data.leagueNumber, data.weekNumber);
                file = Paths.get("seed_order.json").toFile();
            } catch (IOException | URISyntaxException e) {
                file = null;
            }
        }
        
        if (file == null) {
            System.out.println("Unable to read file");
            return 0;
        }

        run.seedList = Api.readJSON(file);
        JSONArray seed_data = (JSONArray) run.seedList.get("data");

        System.out.println("Seed order:");

        // print seed order
        for (int i = 0; i < seed_data.length(); i++) {
            JSONObject this_seed = (JSONObject) seed_data.get(i);
            String seed_type = (String) this_seed.get("type");

            System.out.println("Seed " + (i + 1) + " = " + seed_type);
        }

        // generate seed order image
        orderGenImage();

        return 1;
    }

    private void orderGenImage() {
        JSONArray seed_data = run.seedList.getJSONArray("data");
        ArrayList<String> gen = new ArrayList<>();
        for (int i = 0; i < seed_data.length(); i++) {
            JSONObject this_seed = (JSONObject) seed_data.get(i);
            String seed_type = (String) this_seed.get("type");
            gen.add(seed_type);
        }
        ImageGen.seedOrder(gen, data.currentSeed, Main.seedDone);

        // TODO copy this line to other parts
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seedorder\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + run.imageGenPath + "seedorder.png" + "\"}}}}");
    }

    int sceneCommand(JSONObject commandObject) {
        String scene = commandObject.getString("value");
        run.spectating = false;
        switch(scene) {
            case "intermission":
                System.out.println("Intermission");
                run.commandManager.execute(run.commandParser.getCommand("mute true"));
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("intermissionScene") + "\"}}}");
                return 1;

            case "nextseed":
                System.out.println("Next seed");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));

                // load seed information
                int seed_number = data.currentSeed;

                if (seed_number == 0) seed_number = 1;

                JSONArray seed_data = (JSONArray) run.seedList.get("data");
                JSONObject this_seed = (JSONObject) seed_data.get(seed_number - 1);
                String seed_type = (String) this_seed.get("type");
                String seedTypePreFormat = seed_type.replace("_", " ");
                if (seedTypePreFormat.length() < 2) seedTypePreFormat = "error";
                String seedTypeText = seedTypePreFormat.substring(0, 1).toUpperCase() + seedTypePreFormat.substring(1);
                        
                // set seed information
                run.commandManager.execute(run.commandParser.getCommand("setseed " + data.currentSeed));

                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seedtype text\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + seedTypeText + "\"}}}}");
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seedtype icon\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + run.seediconPath + seed_type + ".png" + "\"}}}}");
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seed_background\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + run.seedimagePath + seed_type + ".png" + "\"}}}}");

                // switch to next seed scene
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("nextseedScene") + "\"}}}");
                return 1;
            
            case "order":
                System.out.println("Seed list");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                String seedlistScene = (String) run.scenes.get("seedlistScene");
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + seedlistScene + "\"}}}");
                return 1;

            case "spectator":
                System.out.println("Spectator");
                run.spectating = true;
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("spectatorScene") + "\"}}}");
                return 1;
            case "game":
                System.out.println("Players");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                int pov_count = commandObject.getInt("var");
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + ((JSONObject) run.scenes.get("gameplayScenes")).get("gameplay" + pov_count) + "\"}}}");
                return 1;
            case "gs":
                System.out.println("Players + spec");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                int pv = commandObject.getInt("var");
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + ((JSONObject) run.scenes.get("gsScenes")).get("gs" + pv) + "\"}}}");
                return 1;
            case "update":
                int pov_count2 = commandObject.getInt("var");
                if (run.mainspec) {
                    run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + ((JSONObject) run.scenes.get("gsScenes")).get("gs" + pov_count2) + "\"}}}");
                } else {
                    run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + ((JSONObject) run.scenes.get("gameplayScenes")).get("gameplay" + pov_count2) + "\"}}}");
                }
                return 1;
            case "leaderboard":
                System.out.println("Leaderboard");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("leaderboardScene") + "\"}}}");
                return 1;
            case "multiweek":
                System.out.println("Multi-week");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("multiweekScene") + "\"}}}");
                return 1;
            case "commentators": 
                System.out.println("Commentators");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("commentatorScene") + "\"}}}");
                return 1;

            case "completions":
                System.out.println("Completions");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("completionScene") + "\"}}}");
                return 1;

            case "interview":
                System.out.println("Interview");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("interviewScene") + "\"}}}");
                return 1;

            default:
                System.out.println("Unknown scene");
                return 0;
        }
    }

    int nextCommand() {
        run.needSave = true;

        Main.seedDone = false;

        Main.unHide(data);

        if (data.currentSeed < Main.getSeedcount(data.leagueNumber)) data.currentSeed++; else {
            System.out.println("All seeds have been played");
            return 0;
        }

        // switch to seed order scene
        System.out.println("Highlighting next seed on the list");
        run.commandManager.execute(run.commandParser.getCommand("order show"));

        System.out.println("Switching to next seed scene in 10 seconds");
        run.commandManager.executeDelayed(run.commandParser.getCommand("scene ns"), 10000L, true);

        return 1;

    }

    int setseedCommand(JSONObject commandObject) {
        run.needSave = true;
        data.currentSeed = commandObject.getInt("value");
        System.out.println("Set seed to " + data.currentSeed);
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"Seed\", \"overlay\": true, \"inputSettings\": {\"text\":\"Seed " + data.currentSeed + "/" + Main.getSeedcount(data.leagueNumber) +"\"}}}}");
        return 1;
    }

    int interviewCommand(JSONObject commandObject) {
        int playerId = commandObject.getInt("id");
        Player player = data.getPlayerById(playerId);

        if (player == null) {
            System.out.println("Unable to find player");
            return 0;
        }

        System.out.println("Interview: " + player.name);

        // update interview skin
        Api.getInterviewSkin(player.name);
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"interview skin\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + run.imageGenPath + "interview.png" + "\"}}}}");

        // update player name
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"interview player\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + player.name + "\"}}}}");

        run.commandManager.execute(run.commandParser.getCommand("scene interview"));

        return 1;
    }

    int showCommand(JSONObject commandObject) {

        int id = commandObject.getInt("id");
        int pos = commandObject.getInt("pos");

        Player player = data.getPlayerById(id);

        if (player == null) {
            System.out.println("Unable to find player");
            return 0;
        }
        
        if (player.live) {
            run.client.showPlayer(player, pos);
            return 1;
        } else {
            System.out.println("Player is not live");
            return 0;
        }        
    }

    int refreshCommand(JSONObject commandObject) {
        int value = commandObject.getInt("value");

        if (value == 0) {
            int r = 0;
            for (int i = 1; i <= 4; i++) {
                r += run.client.refresh(i);
            }
            return r;
        } else {
            return run.client.refresh(value);
        }
    }

    int matchCommand(JSONObject commandObject) {
        String value = commandObject.getString("value");
        run.needSave = true;

        switch (value) {
            case "clear":
                System.out.println("Clearing match data");
                run.clearMatchIds(Main.getSeedcount(data.leagueNumber));
                if (run.mostPercent) run.mp.clearAllData();
                return 1;
            case "host":
                run.hostName = commandObject.getString("name");
                System.out.println("Set host to " + run.hostName);
                return 1;
            case "autodl":
                if (run.hostName.equals("")) {
                    System.out.println("Host not set");
                    return 0;
                }

                int seedNumber = data.currentSeed;
                int[] ids;
                try {
                    ids = Leaderboard.getMatchIds(run.hostName, 1);
                } catch (IOException | URISyntaxException e) {
                    System.out.println("Unable to get match id");
                    return 0;
                }
                int matchId = ids[0];
                System.out.println("Downloading seed " + seedNumber + " from id " + matchId);

                if (run.mostPercent) {
                    mostPercentStuff(matchId);
                    return 1;
                }

                boolean result = Leaderboard.downloadSeed(seedNumber, matchId, run);
                if (result) return 1; else return 0;
            case "dl":
                int seedNumber2 = commandObject.getInt("seed");
                int matchId2 = commandObject.getInt("match");
                System.out.println("Downloading seed " + seedNumber2 + " from id " + matchId2);

                if (run.mostPercent) {
                    mostPercentStuff(matchId2);
                    return 1;
                }

                boolean result2 = Leaderboard.downloadSeed(seedNumber2, matchId2, run);
                if (result2) return 1; else return 0;
            case "id":
                if (run.hostName.equals("")) {
                    System.out.println("Host not set");
                    return 0;
                }

                int count = commandObject.getInt("count");

                int[] ids2;
                try {
                    ids2 = Leaderboard.getMatchIds(run.hostName, count);
                } catch (IOException | URISyntaxException e) {
                    if (count == 1) System.out.println("Unable to get match id"); else System.out.println("Unable to get match ids");
                    return 0;
                }
                if (count == 1) System.out.println("Match id:"); else System.out.println("Match ids:");
                for (int i = ids2.length - 1; i >= 0; i--) System.out.println(ids2[i]);
        }

        return 0;
    }

    void mostPercentStuff(int matchId) {
        System.out.println("Generating completion images");
        run.mp.addMatch(matchId);
        ImageGen.completions(run.mp.getResult(run.mp.ids.size()));
        ImageGen.leaderboard(run.mp.getLeaderboard(), run.mp.ids.size(), run.mp.seedcount);

        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"completions\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + run.imageGenPath + "completions.png" + "\"}}}}");
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"leaderboard\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + run.imageGenPath + "leaderboard.png" + "\"}}}}");
    }

    int leaderboardCommand(JSONObject commandObject) {

        int seedcount = commandObject.getInt("value");

        if (seedcount == 0) {
            System.out.println("Initial multi-week scores");

            JSONObject lb = new JSONObject();
            JSONArray pl = new JSONArray();

            for (int i = 0; i < data.players.length; i++) {
                JSONObject o = new JSONObject();
                o.put("name", data.players[i].name);
                o.put("perfFraction", "0/1");
                pl.put(o);
            }

            lb.put("players", pl);
            lb.put("promotions", 0);
            lb.put("demotions", 0);

            ImageGen.averages(Leaderboard.genAvgLeaderboard(lb, data, run, true));

            // refresh the image
            run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"multiweek\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + run.imageGenPath + "multiweek.png" + "\"}}}}");

            // switch to lb scene
            run.commandManager.execute(run.commandParser.getCommand("scene mw"));

            return 1;
        }

        // check match ids

        boolean test = true;
        for (int i = 1; i <= seedcount; i++) {
            if (run.getMatchId(i) == -1) test = false;
        }

        if (!test) {
            // check again if only last id is missing
            boolean test2 = true;
            for (int i = 1; i < seedcount; i++) if (run.getMatchId(i) == -1) test2 = false;

            // run autodl if only missing last id and if seedcount is set to current seed
            if (test2 && seedcount == data.currentSeed) {
                int dl = run.commandManager.execute(run.commandParser.getCommand("match autodl"));
                if (dl != 1) {
                    System.out.println("Failed to download missing seed");
                    return 0;
                }
            } else {
                System.out.println("Missing match ids");
                return 0;
            }
        }

        boolean b = Leaderboard.genLeaderboard(seedcount, data, run);
        if (!b) {
            System.out.println("Failed to generate leaderboard");
            return 0;
        }

        // generate a new lb image
        JSONObject lb = Api.readJSON(Paths.get("lb_data", "leaderboard.json").toFile());
        ImageGen.leaderboard(lb, data.currentSeed, Main.getSeedcount(data.leagueNumber));

        // generate multi-week average board
        JSONObject avg = Leaderboard.genAvgLeaderboard(lb, data, run, false);
        ImageGen.averages(avg);

        // refresh the images
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"leaderboard\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + run.imageGenPath + "leaderboard.png" + "\"}}}}");
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"multiweek\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + run.imageGenPath + "multiweek.png" + "\"}}}}");

        // switch to lb scene
        run.commandManager.execute(run.commandParser.getCommand("scene leaderboard"));

        return 1;
    }

    int killfeedsCommand() {
        System.out.println("Killing all feeds");
        Streamlink.killAll();
        return 1;
    }

    int completionsCommand(JSONObject commandObject) {
        run.timer.stop();
        run.autoUpdate.stop();

        Main.seedDone = true;

        int value = commandObject.getInt("value");

        JSONObject comp = Comp.getCompletions(data.players, value, run, data);

        if (comp == null) {
            System.out.println("Failed to update completions");
            return 0;
        }

        ImageGen.completions(comp);

        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"completions\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + run.imageGenPath + "completions.png" + "\"}}}}");

        // switch to comp scene
        run.commandManager.execute(run.commandParser.getCommand("scene completions"));

        return 1;
    }

    int splitsCommand() {
        System.out.println("Splits");

        if (run.mostPercent) {
            try {
                data.updateAdv();
            } catch (Exception e) {
                System.out.println("Exception: Failed to update splits");
                return 0;
            }

            data.printAdv();
            return 1;
        }


        try {
            data.updateSplits();
        } catch (Exception e) {
            System.out.println("Exception: Failed to update splits");
            return 0;
        }
        data.printSplits();
        return 1;
    }

    int updateCommand(JSONObject commandObject) {

        int value = commandObject.getInt("value");
        String[] povs = new String[0];

        boolean msg = true;
        if (value == 4) {
            msg = false;
            value = 0;
        }

        switch (value) {
            case 0:
                if (msg) System.out.println("Updating by splits");

                if (data.useLiveApi) {
                    Api.getLiveData(data.apiUser, data.apiKey);
                }

                if (run.mostPercent) {
                    if (Main.top) {
                        povs = data.topAdv();
                    } else {
                        povs = data.bottomAdv();
                    }
                } else {
                    povs = data.topSplits();
                }

                if (povs.length > 0) {
                    int safety = povs.length;
                    if (safety > 4) safety = 4;

                    if (run.mainspec && safety == 4) safety = 3;

                    for (int i = 0; i < safety; i++) {
                        run.client.showPlayer(data.getPlayer(povs[i]), i + 1, false);
                    }

                    run.commandManager.executeDelayed(run.commandParser.getCommand("scene update " + safety), Main.abDelay);
                }

                return 1;
            case 1:
                System.out.println("Activating auto-update");
                run.autoUpdate.start();
                return 1;
            case 2:
                System.out.println("Stopping auto-update");
                run.autoUpdate.stop();
                return 1;
            case 3:
                System.out.println("Showing random povs");
                povs = data.randomPovs();

                if (povs.length > 0) {
                    int safety = povs.length;
                    if (safety > 4) safety = 4;

                    for (int i = 0; i < safety; i++) {
                        run.client.showPlayer(data.getPlayer(povs[i]), i + 1, false);
                    }

                    run.commandManager.execute(run.commandParser.getCommand("scene game " + safety));
                }

                return 1;
        }

        return 0;
    }

    int timerCommand(JSONObject commandObject) {
        String value = commandObject.getString("value");

        switch (value) {
            case "up":
                int limit = Main.getTimeLimit(data.leagueNumber) * 60;
                run.timer.start(limit, false, "Timer2", "none");
                return 1;
            case "down":
                int arg2 = commandObject.getInt("var");
                run.timer.start(arg2, true, "Timer", "none");
                return 1;
            case "advance":
                int arg3 = commandObject.getInt("var");
                run.timer.add(arg3);
                return 1;
            case "stop":
                run.timer.stop();
                return 1;
            default:
                return 0;
            }
        }

    int setliveCommand(JSONObject commandObject) {
        run.needSave = true;
        int id = commandObject.getInt("id");
        Player player = data.getPlayerById(id);

        if (player == null) {
            System.out.println("Player not found");
            return 0;
        }


        int value = commandObject.getInt("value");
        if (value == 1) player.live = true;
        if (value == 2) player.live = false;
        if (value == 0) {
            if (player.live) player.live = false; else player.live = true;
        }

        return 1;
    }

    int addplayerCommand(JSONObject commandObject) {
        run.needSave = true;
        String name = commandObject.getString("value");

        String apiTwitch = Main.getTwitch(name);

        if (apiTwitch != null) {
            System.out.println("Got twitch from api");
            data.addPlayer(name, apiTwitch);
            return 1;
        }

        data.addPlayer(name, "none");
        return 1;
    }

    int twitchCommand(JSONObject commandObject) {
        run.needSave = true;
        int playerId = commandObject.getInt("id");
        Player player = data.getPlayerById(playerId);

        if (player == null) {
            System.out.println("Player not found");
            return 0;
        }

        String twitch;
        if (commandObject.has("value")) twitch = commandObject.getString("value"); else twitch = Main.getTwitch(player.name);

        if (twitch == null) {
            System.out.println("Unable to get player's twitch username");
            return 0;
        }

        System.out.println("Set twitch for " + player.name + " to " + twitch);
        player.twitch = twitch;
        player.live = true;

        return 1;
    }

    int listCommand() {
        System.out.println("Player list:");
        for (int i = 0; i < data.players.length; i++) System.out.println(data.players[i].id + " " + data.players[i].name + " " + data.players[i].twitch + " " + data.players[i].live);
        System.out.println();

        return 1;
    }

    int liveCommand() {
        System.out.println("Live player list:");
        for (int i = 0; i < data.players.length; i++) if (data.players[i].live) System.out.println(i + " " + data.players[i].name + " " + data.players[i].twitch + " " + data.players[i].live);
        System.out.println();

        return 1;
    }

    int hideCommand(JSONObject commandObject) {
        int playerId = commandObject.getInt("id");

        try {
            data.updateSplits();
        } catch (Exception e) {
            System.out.println("Exception: Failed to update splits");
            return 0;
        }

        for (int i = 0; i < data.players.length; i++) if (data.players[i].id == playerId) {
            Player player = data.players[i];
            JSONObject split = data.currentSplit(player.name);
            int splitNumber = split.getInt("split");

            if (splitNumber >= 0) {
                System.out.println("Hiding player " + player.name + " for split " + split.getInt("split"));
                data.players[i].hideSplit = splitNumber;
            } else {
                System.out.println("Player " + player.name + " is not playing");
            }                    
        }

        return 1;
    }

    int unhideCommand() {
        System.out.println("Unhiding all hidden players");
        Main.unHide(data);

        return 1;
    }

    int snapshotCommand(JSONObject commandObject) {
        int value = commandObject.getInt("value");

        switch (value) {
            case 1:
                run.snapshot.start();
                return 1;
            case 2:
                run.snapshot.stop();
                return 1;
            case 3:
                run.snapshot.load(commandObject.getInt("start"));
                return 1;
            case 0:
                run.snapshot.status();
                return 1;
            default:
                return 0;
        }
    }

    

}