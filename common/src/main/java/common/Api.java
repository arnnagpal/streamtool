package common;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Api {

    // returns false if the skin has been downloaded within 6 hours
    public static boolean updateSkin(String username) {
        File pr = Paths.get("skins", username + ".properties").toFile();
        if (!pr.exists()) return true;

        Long dlTime = Api.readJSON(pr).optLong("dlTime", 0);
        if (dlTime + 21600000 > System.currentTimeMillis()) return false;

        return true;
    }

    public static void dlSkin(String username) throws MalformedURLException, IOException, URISyntaxException {
        File dlFile = Paths.get("download.json").toFile();

        BufferedInputStream in = new BufferedInputStream(new URI("https://api.mojang.com/users/profiles/minecraft/" + username).toURL().openStream());
        FileOutputStream out = new FileOutputStream(dlFile);
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }
        out.close();

        JSONObject user = Api.readJSON(dlFile);

        String uuid = user.getString("id");

        in = new BufferedInputStream(new URI("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid).toURL().openStream());
        out = new FileOutputStream(dlFile);
        dataBuffer = new byte[1024];
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }
        out.close();

        JSONObject profile = Api.readJSON(dlFile);


        JSONArray properties = profile.getJSONArray("properties");
        String value = null;

        for (int i = 0; i < properties.length(); i++) {
            JSONObject o = properties.getJSONObject(i);
            String name = o.optString("name", null);
            if (name != null && name.equals("textures")) {
                value = o.getString("value");
                break;
            }
        }

        if (value == null) return;

        JSONObject thing = new JSONObject(new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8));

        File skinsFolder = Paths.get("skins").toFile();
        if (!skinsFolder.exists()) skinsFolder.mkdirs();

        File skinPng = Paths.get("skins", username + ".png").toFile();
        File skinProperties = Paths.get("skins", username + ".properties").toFile();

        JSONObject textures = thing.getJSONObject("textures");
        JSONObject skin = textures.getJSONObject("SKIN");
        JSONObject metadata = skin.optJSONObject("metadata", null);

        boolean slim = false;

        if (metadata != null) {
            String model = metadata.optString("model", "nah");
            if (model.equals("slim")) slim = true;
        }

        JSONObject skPr = new JSONObject();
        skPr.put("slim", slim);
        skPr.put("dlTime", System.currentTimeMillis());
        
        String url = skin.getString("url");

        in = new BufferedInputStream(new URI(url).toURL().openStream());
        out = new FileOutputStream(skinPng);
        dataBuffer = new byte[1024];
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }
        out.close();

        BufferedWriter w = new BufferedWriter(new FileWriter(skinProperties));
        w.write(skPr.toString());
        w.close();
    }

    public static void downloadSeedUnsafe(int matchId) throws MalformedURLException, IOException, URISyntaxException {
        File matchFile = Paths.get("lb_data", "matches", matchId+".json").toFile();

        BufferedInputStream in = new BufferedInputStream(new URI("https://api.mcsrranked.com/matches/" + matchId).toURL().openStream());
        FileOutputStream out = new FileOutputStream(matchFile);
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }
        out.close();

        JSONObject matchData = Api.readJSON(matchFile);
        JSONObject data = matchData.getJSONObject("data");
        JSONArray completions = data.getJSONArray("completions");
        JSONArray players = data.getJSONArray("players");

        System.out.println("Completions:");

        for (int i = 0; i < completions.length(); i++) {
            JSONObject completion = completions.getJSONObject(i);
            String uuid = completion.getString("uuid");
            int time = completion.getInt("time");
            String name = "";

            for (int b = 0; b < players.length(); b++) {
                JSONObject player = players.getJSONObject(b);
                String playerUuid = player.getString("uuid");
                if (uuid.equals(playerUuid)) {
                    name = player.getString("nickname");
                    break;
                }
            }

            System.out.println(name + " " + time);
        }
    }

    public static void testHex(int r) {
        Hexagon hex = new Hexagon(r);
        hex.print();
    }

    public static BufferedImage getInterviewSkin(String username) {

        File skinPng = Paths.get("skins", username + ".png").toFile();
        File skinProperties = Paths.get("skins", username + ".properties").toFile();
        if (!skinPng.exists() || !skinProperties.exists()) {
            try {
                System.out.println("Skin file not found, attempting download");
                dlSkin(username);
            } catch (MalformedURLException e) {
                System.out.println("MalformedURLException");
                return null;
            } catch (IOException e) {
                System.out.println("IOException");
                return null;
            } catch (URISyntaxException e) {
                System.out.println("URISyntaxException");
                return null;
            }
        }

        BufferedImage skinBg = new BufferedImage(64, 64, 1);
        BufferedImage skinTransparent;

        try {
            skinTransparent = ImageIO.read(skinPng);
        } catch (IOException e) {
            System.out.println("IOException");
            return null;
        }

        Graphics2D sG = skinBg.createGraphics();

        sG.drawImage(skinTransparent, 0, 0, null);

        sG.dispose();

        JSONObject prop = Api.readJSON(skinProperties);
        boolean slim = prop.optBoolean("slim", false);

        int armWidth = 4;
        if (slim) armWidth = 3;

        Cuboid head = new Cuboid(15, 8, 8, 8);
        Cuboid body = new Cuboid(15, 8, 12, 4);
        Cuboid leftArm = new Cuboid(15, armWidth, 12, 4);
        Cuboid rightArm = new Cuboid(15, armWidth, 12, 4);
        Cuboid leftLeg = new Cuboid(15, 4, 12, 4);
        Cuboid rightLeg = new Cuboid(15, 4, 12, 4);

        Cuboid headL = new Cuboid(16, 8, 8, 8);
        Cuboid bodyL = new Cuboid(16, 8, 12, 4);
        Cuboid leftArmL = new Cuboid(16, armWidth, 12, 4);
        Cuboid rightArmL = new Cuboid(16, armWidth, 12, 4);
        Cuboid leftLegL = new Cuboid(16, 4, 12, 4);
        Cuboid rightLegL = new Cuboid(16, 4, 12, 4);

        body.setA(new Point(200, 200));
        head.setDEcenter(body.start, body.f);
        leftArm.setStart(body.b);
        rightArm.setB(body.start);
        rightLeg.setStart(body.d);
        leftLeg.setB(body.c);

        headL.setADcenter(head.a, head.d);
        bodyL.setADcenter(body.a, body.d);
        leftArmL.setADcenter(leftArm.a, leftArm.d);
        rightArmL.setADcenter(rightArm.a, rightArm.d);
        leftLegL.setADcenter(leftLeg.a, leftLeg.d);
        rightLegL.setADcenter(rightLeg.a, rightLeg.d);

        BufferedImage test = new BufferedImage(400, 800, 2);

        Graphics2D g = test.createGraphics();

        BufferedImage[] l1 = leftLegL.drawTexture(skinTransparent.getSubimage(0, 48, 16, 16));
        BufferedImage[] l2 = rightLegL.drawTexture(skinTransparent.getSubimage(0, 32, 16, 16));
        BufferedImage[] l3 = leftArmL.drawTexture(skinTransparent.getSubimage(48, 48, 16, 16));
        BufferedImage[] l4 = bodyL.drawTexture(skinTransparent.getSubimage(16, 32, 24, 16));
        BufferedImage[] l5 = rightArmL.drawTexture(skinTransparent.getSubimage(40, 32, 16, 16));
        BufferedImage[] l6 = headL.drawTexture(skinTransparent.getSubimage(32, 0, 32, 16));

        BufferedImage[] m1 = leftLeg.drawTexture(skinBg.getSubimage(16, 48, 16, 16));
        BufferedImage[] m2 = rightLeg.drawTexture(skinBg.getSubimage(0, 16, 16, 16));
        BufferedImage[] m3 = leftArm.drawTexture(skinBg.getSubimage(32, 48, 16, 16));
        BufferedImage[] m4 = body.drawTexture(skinBg.getSubimage(16, 16, 24, 16));
        BufferedImage[] m5 = rightArm.drawTexture(skinBg.getSubimage(40, 16, 16, 16));
        BufferedImage[] m6 = head.drawTexture(skinBg.getSubimage(0, 0, 32, 16));

        g.drawImage(l1[0], 0, 0, null);
        g.drawImage(m1[0], 0, 0, null);
        g.drawImage(m1[1], 0, 0, null);
        g.drawImage(l1[1], 0, 0, null);

        g.drawImage(l2[0], 0, 0, null);
        g.drawImage(m2[0], 0, 0, null);
        g.drawImage(m2[1], 0, 0, null);
        g.drawImage(l2[1], 0, 0, null);

        g.drawImage(l3[0], 0, 0, null);
        g.drawImage(m3[0], 0, 0, null);
        g.drawImage(m3[1], 0, 0, null);
        g.drawImage(l3[1], 0, 0, null);

        g.drawImage(l4[0], 0, 0, null);
        g.drawImage(m4[0], 0, 0, null);
        g.drawImage(m4[1], 0, 0, null);
        g.drawImage(l4[1], 0, 0, null);

        g.drawImage(l5[0], 0, 0, null);
        g.drawImage(m5[0], 0, 0, null);
        g.drawImage(m5[1], 0, 0, null);
        g.drawImage(l5[1], 0, 0, null);

        g.drawImage(l6[0], 0, 0, null);
        g.drawImage(m6[0], 0, 0, null);
        g.drawImage(m6[1], 0, 0, null);
        g.drawImage(l6[1], 0, 0, null);

        g.dispose();

        File interview = Paths.get("gen_images", "interview.png").toFile();

        try {
            ImageIO.write(test, "png", interview);
        } catch (IOException e) {}

        return test;
    }

    public static BufferedImage getHead(String username) {
        return getHead(username, 256);
    }

    public static BufferedImage getHead(String username, int size) {
        try {
            return getHeadEx(username, size, false);
        } catch (IOException e) {
            System.out.println("IOException");
            return null;
        }
    }

    private static BufferedImage getHeadEx(String username, int size, boolean threedim) throws IOException {

        if (size > 8092) {
            System.out.println("Max size: 8092");
            return null;
        }


        int layerSize = size / 2;
        int mainSize = (int) Math.round(layerSize / 16.0 * 15.0);



        File skinPng = Paths.get("skins", username + ".png").toFile();
        if (!skinPng.exists()) {
            try {
                System.out.println("Skin file not found, attempting download");
                dlSkin(username);
            } catch (MalformedURLException e) {
                System.out.println("MalformedURLException");
                return null;
            } catch (IOException e) {
                System.out.println("IOException");
                return null;
            } catch (URISyntaxException e) {
                System.out.println("URISyntaxException");
                return null;
            }
        }

        BufferedImage skin = ImageIO.read(skinPng);

        File headsFolder = Paths.get("heads").toFile();
        if (!headsFolder.exists()) headsFolder.mkdirs();

        int r1 = mainSize;
        int r2 = layerSize;

        int diff = r2 - r1;

        // 2D IMAGE

        if (!threedim) {
            Image headMainScaled = skin.getSubimage(8,8,8,8).getScaledInstance(mainSize*2, mainSize*2, Image.SCALE_FAST);
            Image headLayerScaled = skin.getSubimage(40,8,8,8).getScaledInstance(layerSize*2, layerSize*2, Image.SCALE_FAST);

            BufferedImage headScaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

            Graphics2D gr = headScaled.createGraphics();

            gr.setColor(new Color(0, 0, 0, 255));
            gr.fillRect(diff, diff, mainSize * 2, mainSize * 2);

            gr.drawImage(headMainScaled, diff, diff, null);
            gr.drawImage(headLayerScaled, 0, 0, null);

            gr.dispose();

            File headScaledPng = Paths.get("heads", username + "-2d.png").toFile();
            ImageIO.write(headScaled, "png", headScaledPng);

            return headScaled;
        }

        // 3D IMAGE

        Image face = skin.getSubimage(8,8,8,8);
        Image side = skin.getSubimage(0,8,8,8);
        Image top = skin.getSubimage(8,0,8,8);

        Image sideBack = skin.getSubimage(16,8,8,8);
        Image back = skin.getSubimage(24,8,8,8);
        Image bottom = skin.getSubimage(16,0,8,8);

        Image faceLayer = skin.getSubimage(40,8,8,8);
        Image sideLayer = skin.getSubimage(32,8,8,8);
        Image topLayer = skin.getSubimage(40,0,8,8);

        Image sideBackLayer = skin.getSubimage(48,8,8,8);
        Image backLayer = skin.getSubimage(56,8,8,8);
        Image bottomLayer = skin.getSubimage(48,0,8,8);

        Hexagon hex1 = new Hexagon(r1);
        Hexagon hex2 = new Hexagon(r2);

        // WARP MAIN
        BufferedImage warpTop = getWarped(top, hex1.f, hex1.a, hex1.center, hex1.b);
        BufferedImage warpSide = getWarped(side, hex1.f, hex1.center, hex1.e, hex1.d);
        BufferedImage warpFace = getWarped(face, hex1.center, hex1.b, hex1.d, hex1.c);

        BufferedImage warpBottom = getWarped(bottom, hex1.e, hex1.center, hex1.d, hex1.c);
        BufferedImage warpSideBack = getWarped(sideBack, hex1.b, hex1.a, hex1.c, hex1.center);
        BufferedImage warpBack = getWarped(back, hex1.a, hex1.f, hex1.center, hex1.e);

        // WARP LAYER
        BufferedImage warpTopLayer = getWarped(topLayer, hex2.f, hex2.a, hex2.center, hex2.b);
        BufferedImage warpSideLayer = getWarped(sideLayer, hex2.f, hex2.center, hex2.e, hex2.d);
        BufferedImage warpFaceLayer = getWarped(faceLayer, hex2.center, hex2.b, hex2.d, hex2.c);

        BufferedImage warpBottomLayer = getWarped(bottomLayer, hex2.e, hex2.center, hex2.d, hex2.c);
        BufferedImage warpSideBackLayer = getWarped(sideBackLayer, hex2.b, hex2.a, hex2.c, hex2.center);
        BufferedImage warpBackLayer = getWarped(backLayer, hex2.a, hex2.f, hex2.center, hex2.e);

        // COMBINE

        BufferedImage combinedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics g = combinedImage.getGraphics();

        g.drawImage(warpBackLayer, 0, 0, null);
        g.drawImage(warpSideBackLayer, 0, 0, null);
        g.drawImage(warpBottomLayer, 0, 0, null);

        g.drawImage(warpBack, 0+diff, 0+diff, null);
        g.drawImage(warpSideBack, 0+diff, 0+diff, null);
        g.drawImage(warpBottom, 0+diff, 0+diff, null);

        g.drawImage(warpTop, 0+diff, 0+diff, null);
        g.drawImage(warpSide, 0+diff, 0+diff, null);
        g.drawImage(warpFace, 0+diff, 0+diff, null);

        g.drawImage(warpTopLayer, 0, 0, null);
        g.drawImage(warpSideLayer, 0, 0, null);
        g.drawImage(warpFaceLayer, 0, 0, null);

        g.dispose();

        // WRITE TO FILE

        File warpComb = Paths.get("heads", username + "-3d.png").toFile();
        ImageIO.write(combinedImage, "png", warpComb);

        return combinedImage;
    }

    

    static BufferedImage getWarped(Image image, Point a, Point b, Point c, Point d) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);

        Point oA = new Point(0, 0);
        Point oB = new Point(width, 0);
        Point oC = new Point(0, height);
        Point oD = new Point(width, height);

        int widthNew = 0;
        if (a.x > widthNew) widthNew = a.x;
        if (b.x > widthNew) widthNew = b.x;
        if (c.x > widthNew) widthNew = c.x;
        if (d.x > widthNew) widthNew = d.x;

        int heightNew = 0;
        if (a.y > heightNew) heightNew = a.y;
        if (b.y > heightNew) heightNew = b.y;
        if (c.y > heightNew) heightNew = c.y;
        if (d.y > heightNew) heightNew = d.y;

        BufferedImage abcI = new BufferedImage(widthNew, heightNew, BufferedImage.TYPE_INT_ARGB);
        BufferedImage bcdI = new BufferedImage(widthNew, heightNew, BufferedImage.TYPE_INT_ARGB);

        Graphics2D abcG = abcI.createGraphics();
        Graphics2D bcdG = bcdI.createGraphics();

        Polygon abc = new Polygon();
        Polygon bcd = new Polygon();

        abc.addPoint(a.x, a.y);
        abc.addPoint(b.x, b.y);
        abc.addPoint(c.x, c.y);

        bcd.addPoint(b.x, b.y);
        bcd.addPoint(c.x, c.y);
        bcd.addPoint(d.x, d.y);

        abcG.setClip(abc);
        bcdG.setClip(bcd);

        AffineTransform abcTx = getTriangleTransform(oA, oB, oC, a, b, c);
        AffineTransform bcdTx = getTriangleTransform(oB, oC, oD, b, c, d);

        abcG.transform(abcTx);
        bcdG.transform(bcdTx);

        abcG.drawImage(image, 0, 0, null);
        bcdG.drawImage(image, 0, 0, null);

        abcG.dispose();
        bcdG.dispose();

        BufferedImage combine = new BufferedImage(widthNew, heightNew, BufferedImage.TYPE_INT_ARGB);

        Graphics2D com = combine.createGraphics();

        com.drawImage(abcI, 0, 0, null);
        com.drawImage(bcdI, 0, 0, null);

        com.dispose();

        return combine;
    }

    static AffineTransform getTriangleTransform(Point s0, Point s1, Point s2, Point d0, Point d1, Point d2) {
        // Determinant of the source point matrix
        double det = s0.x * (s1.y - s2.y) - s0.y * (s1.x - s2.x) + (s1.x * s2.y - s2.x * s1.y);
        
        if (Math.abs(det) < 1e-5) {
            throw new IllegalArgumentException("Source points are collinear; cannot compute transform.");
        }
        
        // Calculate matrix elements
        double m00 = ((s1.y - s2.y) * d0.x + (s2.y - s0.y) * d1.x + (s0.y - s1.y) * d2.x) / det;
        double m01 = ((s2.x - s1.x) * d0.x + (s0.x - s2.x) * d1.x + (s1.x - s0.x) * d2.x) / det;
        double m02 = ((s1.x * s2.y - s2.x * s1.y) * d0.x + (s2.x * s0.y - s0.x * s2.y) * d1.x + (s0.x * s1.y - s1.x * s0.y) * d2.x) / det;
        
        double m10 = ((s1.y - s2.y) * d0.y + (s2.y - s0.y) * d1.y + (s0.y - s1.y) * d2.y) / det;
        double m11 = ((s2.x - s1.x) * d0.y + (s0.x - s2.x) * d1.y + (s1.x - s0.x) * d2.y) / det;
        double m12 = ((s1.x * s2.y - s2.x * s1.y) * d0.y + (s2.x * s0.y - s0.x * s2.y) * d1.y + (s0.x * s1.y - s1.x * s0.y) * d2.y) / det;

        // Return matrix parameters mapped to standard Java AffineTransform arguments
        return new AffineTransform(m00, m10, m01, m11, m02, m12);
    }

    public static JSONObject readJSON(File file) {
        JSONObject result = null;

        for (int i = 0; i < 10; i++) {
            try {
                result = readJSONunsafe(file);
            } catch (JSONException e) {
                result = null;
            } catch (IOException e) {
                result = null;
            }

            if (result != null) return result;
        }

        return null;
    }

    public static JSONArray readJSONArray(File file) {
        JSONArray result = null;

        for (int i = 0; i < 10; i++) {
            try {
                result = readJSONArrayunsafe(file);
            } catch (JSONException e) {
                result = null;
            } catch (IOException e) {
                result = null;
            }

            if (result != null) return result;
        }

        return null;
    }

    static JSONObject readJSONunsafe(File file) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        String ls = System.getProperty("line.separator");
        while ((line = reader.readLine()) != null) {
	        stringBuilder.append(line);
	        stringBuilder.append(ls);
        }

        if (stringBuilder.length() == 0) {
            reader.close();
            return null;
        }

        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        reader.close();

        String content = stringBuilder.toString();

        JSONObject o = new JSONObject(content);

        return o;
    }

    static JSONArray readJSONArrayunsafe(File file) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        String ls = System.getProperty("line.separator");
        while ((line = reader.readLine()) != null) {
	        stringBuilder.append(line);
	        stringBuilder.append(ls);
        }

        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        reader.close();

        String content = stringBuilder.toString();

        JSONArray o = new JSONArray(content);

        return o;
    }
}

class Hexagon {
    Point center;
    Point a;
    Point b;
    Point c;
    Point d;
    Point e;
    Point f;

    void print() {
        System.out.println("HEXAGON");
        System.out.println();
        System.out.println("CENTER ("+center.x+", "+center.y+")");
        System.out.println("A ("+a.x+", "+a.y+")");
        System.out.println("B ("+b.x+", "+b.y+")");
        System.out.println("C ("+c.x+", "+c.y+")");
        System.out.println("D ("+d.x+", "+d.y+")");
        System.out.println("E ("+e.x+", "+e.y+")");
        System.out.println("F ("+f.x+", "+f.y+")");
        System.out.println();
    }

    Hexagon(int r) {
        center = new Point(r, r);
        a = new Point(r, 0);
        d = new Point(r, r * 2);

        double angle = Math.toRadians(60.0);
        double sin = r - r * Math.sin(angle);
        double cos = r - r * Math.cos(angle);

        int xB = (int) Math.round(sin);
        int yB = (int) Math.round(cos);

        f = new Point(xB, yB);
        e = new Point(xB, r * 2 - (r - yB));
        b = new Point(r * 2 - xB, yB);
        c = new Point (b.x, e.y);
    }
}
