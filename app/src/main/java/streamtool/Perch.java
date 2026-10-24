package streamtool;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import dev.arnagpal.perch.PerchTimer;
import dev.arnagpal.perch.Status;

public class Perch {

    static PerchTimer perch;

    static void start(RuntimeData run, Data data) {
        File presets = new File("presets.zip");

        if (!presets.exists()) {
            System.out.println("No presets.zip, perch timer is off");
            return;
        }

        perch = PerchTimer.start();

        try {
            System.out.println("Loaded " + perch.load(presets).size() + " perch presets");
        } catch (IOException e) {
            System.out.println("Couldn't read presets.zip, perch timer is off");
            perch.close();
            return;
        }

        place(run.obsLayout.optString("perchCorner", "bottom-right"));

        Thread thread = new Thread() {
            public void run() {
                int seed = -1;
                String[] shown = new String[4], shownSooner = new String[4], shownLater = new String[4];

                while (true) {
                    try {
                        if (data.currentSeed != seed) {
                            seed = data.currentSeed;
                            perch.run(seed);
                        }

                        for (int i = 0; i < 4; i++) {
                            Status status = status(run.client.povs[i]);
                            String text = text(status);
                            String sooner = text.isEmpty() ? "" : status.soonerText();
                            String later = text.isEmpty() ? "" : status.laterText();

                            if (!text.equals(shown[i])) {
                                send(run, "perch" + (i + 1), text);
                                shown[i] = text;
                            }

                            if (!sooner.equals(shownSooner[i])) {
                                send(run, "perchsooner" + (i + 1), sooner);
                                shownSooner[i] = sooner;
                            }

                            if (!later.equals(shownLater[i])) {
                                send(run, "perchlater" + (i + 1), later);
                                shownLater[i] = later;
                            }
                        }
                    } catch (Exception e) {}

                    try {
                        sleep(500);
                    } catch (InterruptedException e) {}
                }
            }
        };

        thread.setDaemon(true);
        thread.start();
    }

    static Status status(Player player) {
        if (player == null) return null;

        Status status = player.uuid == null ? null : perch.status(player.uuid);
        if (status == null) status = perch.status(player.name);
        if (status == null || !status.visible()) return null;

        return status;
    }

    static String text(Status status) {
        if (status == null || status.state() == Status.State.PERCHING) return "";

        return String.format("Perch  in  %.0fs", status.seconds());
    }

    static void send(RuntimeData run, String source, String text) {
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + source + "\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + text + "\"}}}}");
    }

    static void place(String corner) {
        double[][] layout;

        switch (corner) {
            case "top-left": layout = new double[][]{{120, 110, 5}, {120, 160, 5}, {200, 160, 5}}; break;
            case "top-right": layout = new double[][]{{1890, 30, 6}, {1810, 80, 6}, {1890, 80, 6}}; break;
            case "bottom-left": layout = new double[][]{{30, 1050, 9}, {30, 1004, 9}, {110, 1004, 9}}; break;

            default:
                corner = "bottom-right";
                layout = new double[][]{{1890, 1050, 10}, {1810, 1004, 10}, {1890, 1004, 10}};
        }

        try {
            Placer placer = new Placer(layout);

            if (!placer.connectBlocking(5, TimeUnit.SECONDS) || !placer.done.await(5, TimeUnit.SECONDS)) {
                System.out.println("Couldn't place the perch timer");
            } else if (placer.placed == 0) {
                System.out.println("No perch sources in obs, add them to the pov groups");
            } else {
                System.out.println("Placed " + placer.placed + " perch sources " + corner);
            }

            placer.close();
        } catch (Exception e) {
            System.out.println("Couldn't place the perch timer");
        }
    }

    static class Placer extends WebSocketClient {

        static final String[] SOURCES = {"perch", "perchsooner", "perchlater"};

        double[][] layout;
        CountDownLatch done = new CountDownLatch(4 * SOURCES.length);
        int placed;

        Placer(double[][] layout) throws Exception {
            super(new URI("ws://127.0.0.1:4455"));
            this.layout = layout;
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
        }

        @Override
        public void onMessage(String message) {
            JSONObject o = new JSONObject(message);
            JSONObject d = o.getJSONObject("d");

            if (o.getInt("op") == 0) {
                send("{\"op\": 1, \"d\": {\"rpcVersion\": 1, \"eventSubscriptions\": 0}}");
                return;
            }

            if (o.getInt("op") == 2) {
                for (int pov = 1; pov <= 4; pov++) {
                    for (int s = 0; s < SOURCES.length; s++) {
                        send("{\"op\": 6, \"d\": {\"requestType\": \"GetSceneItemId\", \"requestId\": \"" + pov + ":" + s + "\", \"requestData\": {\"sceneName\": \"p" + pov + "\", \"sourceName\": \"" + SOURCES[s] + pov + "\"}}}");
                    }
                }

                return;
            }

            if (o.getInt("op") != 7 || !d.getString("requestType").equals("GetSceneItemId")) return;

            if (d.getJSONObject("requestStatus").getBoolean("result")) {
                String[] id = d.getString("requestId").split(":");
                int item = d.getJSONObject("responseData").getInt("sceneItemId");
                double[] l = layout[Integer.parseInt(id[1])];
                send("{\"op\": 6, \"d\": {\"requestType\": \"SetSceneItemTransform\", \"requestId\": \"set\", \"requestData\": {\"sceneName\": \"p" + id[0] + "\", \"sceneItemId\": " + item + ", \"sceneItemTransform\": {\"positionX\": " + l[0] + ", \"positionY\": " + l[1] + ", \"alignment\": " + (int) l[2] + "}}}}");
                placed++;
            }

            done.countDown();
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
        }

        @Override
        public void onError(Exception e) {
        }

    }

}
