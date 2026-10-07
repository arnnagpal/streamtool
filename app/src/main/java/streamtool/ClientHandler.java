package streamtool;

import java.net.URI;
import java.util.ArrayList;

import common.Api;
import common.Client;

public class ClientHandler {

    private Client client;

    RuntimeData run;

    Player[] povs = new Player[4];
    boolean useStreamlink;
    DoubleFeed[] feeds = new DoubleFeed[4];

    String imagePath;
    int portRange;

    // port range is 100 ports, starting from the selected value, so from 7000 to 7099 for example.
    int currentPort = 0;

    void updatePort() {
        currentPort++;
        if (currentPort > 100) currentPort = 0;
    }

    public ClientHandler(URI uri, RuntimeData run) {
        client = new Client(uri);
        for (int i = 0; i < povs.length; i++) povs[i] = null;
        this.useStreamlink = run.useStreamlink;
        this.imagePath = run.imagePath;
        this.portRange = run.portRange;
        this.run = run;
    }

    int showPlayer(Player player, int place) {
        return showPlayer(player, place, true);
    }

    int showPlayer(Player player, int place, boolean allowReplace) {
        if (place < 1 | place > 4) return 2;
        if (!player.live) return 3;
        if (povs[place - 1] != null) {
            if (povs[place - 1].name.equals(player.name)) return 4;
        }

        updatePort();
        int feedPort = portRange + currentPort;
        String feedLink = "http://localhost:" + feedPort;
        String name = player.displayName;
        String pronouns = player.pronouns;
        String image = imagePath + player.name + "-2d.png";
        String twitch = player.twitch;

        int headSize = 100;
        if (run.mostPercent) headSize = 130;

        try {
            Api.getHead(player.name, headSize);
        } catch (Exception e) {}
        
        if (allowReplace) {
            int check = 0;
            for (int i = 0; i < 4; i++) if (povs[i] != null) {
                if (player.name.equals(povs[i].name)) {
                    check = i + 1;
                }
            }

            if (check > 0) {
                Player swapPlayer = povs[place - 1];
                if (swapPlayer != null) showPlayer(swapPlayer, check, false);
            }
        }

        povs[place - 1] = player;

        if (feeds[place - 1] == null) {
            feeds[place - 1] = new DoubleFeed();
        }

        feeds[place - 1].newFeed(new Streamlink(twitch, feedPort));

        new ABSwitch(place, Main.abDelay, run, feeds[place - 1].feed, name, image, feedLink, pronouns, player.name);

        return 1;
    }

    int refresh(int place) {
        if (!useStreamlink) return 0;

        if (place > 4 || place < 1) return 0;

        Player player = povs[place - 1];

        if (player == null) return 0;

        updatePort();
        int feedPort = portRange + currentPort;
        String feedLink = "http://localhost:" + feedPort;
        String name = player.displayName;
        String image = imagePath + player.name + "-2d.png";
        String pronouns = player.pronouns;

        int headSize = 100;
        if (run.mostPercent) headSize = 130;

        try {
            Api.getHead(player.name, headSize);
        } catch (Exception e) {}

        String twitch = player.twitch;

        if (feeds[place - 1] == null) {
            feeds[place - 1] = new DoubleFeed();
        }

        feeds[place - 1].newFeed(new Streamlink(twitch, feedPort));

        new ABSwitch(place, 0L, run, feeds[place - 1].feed, name, image, feedLink, pronouns, player.name);

        return 1;
    }

    void send(String message) {
        client.send(message);
    }

    void connect() {
        try {
            client.connectBlocking();
        } catch (InterruptedException e) {}
    }

    void close() {
        try {
            client.closeBlocking();
        } catch (InterruptedException e) {}
    }
}

class ABSwitch implements Runnable {
    int pov;
    ArrayList<String> requests;
    Long wait;
    RuntimeData run;

    String name;
    String image;
    String pronouns;
    String ign;

    String feedName;
    String nameSlot;
    String pronounSlot;
    String headSlot;

    String advSlot;

    boolean feedA;
    boolean feedB;

    public ABSwitch(int pov, Long wait, RuntimeData run, boolean feed, String name, String image, String feedLink, String pronouns, String ign) {
        this.pov = pov;
        this.name = name;
        this.image = image;
        this.pronouns = pronouns;
        this.ign = ign;

        requests = new ArrayList<String>();

        feedName = "feed" + pov;
        if (!feed) feedName = feedName + "b";

        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \""+ feedName +"\", \"overlay\": true, \"inputSettings\": {\"input\":\"" + feedLink + "\"}}}}");

        nameSlot = "pov" + pov + "name";
        headSlot = "head" + pov;
        pronounSlot = "pronouns" + pov;
        advSlot = "adv" + pov;

        feedA = feed;
        if (feedA) feedB = false; else feedB = true;

        this.wait = wait;
        this.run = run;
        new Thread(this).start();
    }

    public void run() {
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {}

        // change head, name, pronouns
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + headSlot + "\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + image + "\"}}}}");
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + nameSlot + "\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + name + "\"}}}}");
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + pronounSlot + "\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + pronouns + "\"}}}}");

        // set feed visibility
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetSceneItemEnabled\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + "p" + pov + "\", \"sceneItemId\": " + (run.scenes.getJSONObject("abIds")).getJSONObject("p" + pov).getInt("a") + ", \"sceneItemEnabled\": " + feedA + "}}}");
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetSceneItemEnabled\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + "p" + pov + "\", \"sceneItemId\": " + (run.scenes.getJSONObject("abIds")).getJSONObject("p" + pov).getInt("b") + ", \"sceneItemEnabled\": " + feedB + "}}}");

        if (run.mostPercent) {
            run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + advSlot + "\", \"overlay\": true, \"inputSettings\": {\"text_file\":\"/home/me/MCRL Streaming/streamtool/adv_count/" + ign + ".txt\"}}}}");
        }
    }
}