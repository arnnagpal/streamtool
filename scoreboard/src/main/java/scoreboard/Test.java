package scoreboard;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.json.JSONObject;

import common.Api;
import common.Client;

public class Test {
    public static void main(String[] args) throws URISyntaxException, InterruptedException, MalformedURLException, IOException {
        System.out.println("TEST");

        //ClientHandler h = new ClientHandler(new Client(new URI("ws://127.0.0.1:4455")));

        // client.send("{\"op\":6, \"d\":{\"requestType\": \"GetSceneItemList\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"Board\"}}}");

        // IDS lb1 = 8, lb2 = 9, lb3 = 10
        // HEIGHTS 1st = 10, 2nd = 220, 3rd = 430

        //h.setScale(8, 1.0);

        int size = 256;

        if (args.length > 1) {
            size = Integer.parseInt(args[1]);
        }

        Api.getHead(args[0], size, true);

        // Thread.sleep(1000);
        //h.close();
    }
}

class ClientHandler {
    Client client;

    public ClientHandler(Client client) throws InterruptedException {
        this.client = client;
        this.client.connectBlocking();
        this.client.send("{\"op\": 1, \"d\": {\"rpcVersion\": 1, \"eventSubscriptions\": 0}}");
    }

    void setHeight(int id, int height) {
        JSONObject request = new JSONObject();
        request.put("op", 6);
        JSONObject d = new JSONObject();
        d.put("requestId", 0);
        d.put("requestType", "SetSceneItemTransform");
        JSONObject requestData = new JSONObject();
        requestData.put("sceneName", "Board");
        requestData.put("sceneItemId", id);
        JSONObject sceneItemTransform = new JSONObject();
        sceneItemTransform.put("positionY", height);
        requestData.put("sceneItemTransform", sceneItemTransform);
        d.put("requestData", requestData);
        request.put("d", d);
        client.send(request.toString());
    }

    void setScale(int id, double scale) {
        JSONObject request = new JSONObject();
        request.put("op", 6);
        JSONObject d = new JSONObject();
        d.put("requestId", 0);
        d.put("requestType", "SetSceneItemTransform");
        JSONObject requestData = new JSONObject();
        requestData.put("sceneName", "Board");
        requestData.put("sceneItemId", id);
        JSONObject sceneItemTransform = new JSONObject();
        sceneItemTransform.put("scaleX", scale);
        sceneItemTransform.put("scaleY", scale);
        requestData.put("sceneItemTransform", sceneItemTransform);
        d.put("requestData", requestData);
        request.put("d", d);
        client.send(request.toString());
    }

    void close() {
        client.close();
    }
}
