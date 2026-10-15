package common;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;

public class ImageGen {
    static Color bg = new Color(0, 0, 0, 0);
    static Color main = new Color(255, 255, 255, 63);

    static Color white = new Color(255, 255, 255, 255);
    static Color gray = new Color(193, 193, 193, 255);

    static Color promote = new Color(127, 255, 127, 63);
    static Color demote = new Color(255, 127, 127, 63);

    static Font font80 = new Font("Mojang", 1, 80);
    static Font font64 = new Font("Mojang", 1, 64);

    public static void seedOrder(ArrayList<String> seeds, int current, boolean seedDone) {

        System.out.println("Current: " + current);
        System.out.println("Seeds: " + seeds.size());

        int c = current;

        if (c < 1) c = 1;
        if (c > seeds.size()) c = seeds.size() + 1; 

        c--;

        Color next = new Color(127, 193, 127, 127);
        Color used = new Color(111, 95, 95, 95);

        ArrayList<BufferedImage> blocks = new ArrayList<>();

        for (int i = 0; i < seeds.size(); i++) {
            String typeId = seeds.get(i);

            if (typeId.length() < 3) {
                System.out.println("Invalid seed type");
                return;
            }

            File iconFile = Paths.get("assets", typeId + ".png").toFile();
            if (!iconFile.exists()) {
                System.out.println("Seed icon not found, abort");
                return;
            }

            String s = typeId.replace("_", " ");
            String typeTxt = s.substring(0, 1).toUpperCase() + s.substring(1);

            BufferedImage icon;
            try {
                icon = ImageIO.read(iconFile);
            } catch (IOException e) {
                System.out.println("IOException: failed to read image");
                return;
            }

            BufferedImage block = new BufferedImage(900, 150, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = block.createGraphics();

            if (i < current - 1 || (seedDone && i <= current - 1)) {
                g.setBackground(bg);
            } else if (i == current - 1) {
                g.setBackground(next);
            } else g.setBackground(main);

            g.clearRect(0, 0, 900, 150);

            g.drawImage(icon, 11, 11, null);
            g.setFont(font64);

            g.drawString(typeTxt, 160, 131);

            if (i < current - 1 || (seedDone && i <= current - 1)) {
                g.setColor(used);
                g.fillRect(0, 0, 900, 150);
            }

            g.dispose();

            blocks.add(block);
        }

        BufferedImage render = new BufferedImage(1810, 630, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = render.createGraphics();

        g.setBackground(bg);
        g.clearRect(0, 0, 1810, 630);

        int h = 3;
        if (blocks.size() < 7) h = 2;

        for (int i = 0; i < blocks.size(); i++) {
            int x, y;
            if (i > h) {
                x = 910;
                y = 160 * (i - h - 1);
            } else {
                x = 0;
                y = 160 * i;
            }
            
            g.drawImage(blocks.get(i), x, y, null);
        }

        g.dispose();

        File outputFolder = Paths.get("gen_images").toFile();
        if (!outputFolder.exists()) outputFolder.mkdirs();

        File output = Paths.get("gen_images", "seedorder.png").toFile();

        try {
            ImageIO.write(render, "png", output);
        } catch (IOException e) {
            System.out.println("IOException: failed to write image file");
            return;
        }

        System.out.println("Image generated successfully");
    }

    public static void completions(JSONObject comp) {

        JSONArray completions = comp.getJSONArray("completions");

        BufferedImage title = new BufferedImage(1000, 100, 2);
        String titleTxt = "Seed results";
        String seedNum = comp.getInt("seedNum") + "/" + comp.getInt("seedCount");

        if (true) {
            Graphics2D g = title.createGraphics();
            g.setFont(font80);
            g.setBackground(main);
            g.clearRect(0, 0, 695, 100);
            g.drawString(titleTxt, 10, 82);
            g.clearRect(705, 0, 203, 100);
            g.drawString(seedNum, 715, 82);
            g.dispose();
        }

        ArrayList<BufferedImage> blocks = new ArrayList<>();
        int[] a = getSize(completions.length());
        int col = a[0];
        int width = a[1];
        int ro = completions.length() / col;
        if (col * ro < completions.length()) ro++;

        for (int i = 0; i < completions.length(); i++) {
            JSONObject o = completions.getJSONObject(i);
            String name = o.getString("name");
            String time = o.getString("time");
            String points = o.optString("points", "");

            int height = width/5;
            int margin = width/60;

            BufferedImage head = Api.getHead(name, height - (margin * 2));

            int fontsize = (height - (margin * 3)) / 2;
            int namesize = fontsize;

            int ln = name.length() * Math.round(fontsize * 0.8f) + margin;
            if (ln > width - height - (margin * 2)) {
                Double f = width - height - (margin * 2.0);
                f /= ln;
                namesize = (int) Math.round(Math.floor(fontsize * f));
            }

            BufferedImage block = new BufferedImage(width, height, 2);
            Graphics2D g = block.createGraphics();
            g.setBackground(main);
            g.clearRect(0, 0, width, height);
            g.setColor(white);
            g.setFont(new Font("Mojang", 1, namesize));
            g.drawImage(head, margin, margin, null);
            g.drawString(name, height + margin, (height + margin) / 2 - margin);
            g.setColor(gray);
            g.setFont(new Font("Mojang", 1, fontsize));
            g.drawString(time, height + margin, height - margin);
            g.drawString(points, width - (Math.round(fontsize * 0.75f) * points.length()) - margin, height - margin);

            g.dispose();

            blocks.add(block);
        }

        BufferedImage render = new BufferedImage(1920, 846, 2);

        Graphics2D g = render.createGraphics();

        g.setBackground(bg);
        g.clearRect(0, 0, 1920, 846);

        g.drawImage(title, 10, 10 ,null);

        int curCol = 0;

        for (int i = 0; i < blocks.size(); i++) {
            int i2 = i;
            i2 -= ro * curCol;
            if (i2 == ro) curCol++;

            int y = i;
            y -= ro * curCol;

            g.drawImage(blocks.get(i), ((width + 10) * curCol) + 10, 120 + y * (width / 5 + 10), null);
        }

        g.dispose();

        File outputFolder = Paths.get("gen_images").toFile();
        if (!outputFolder.exists()) outputFolder.mkdirs();

        File output = Paths.get("gen_images", "completions.png").toFile();

        try {
            ImageIO.write(render, "png", output);
        } catch (IOException e) {
            System.out.println("IOException: failed to write image file");
            return;
        }

        System.out.println("Image generated successfully");
    }

    public static void leaderboard(JSONObject lb, int currentSeed, int seedCount) {

        JSONArray players = lb.getJSONArray("players");

        BufferedImage title = new BufferedImage(941, 100, 2);
        String titleTxt = "Leaderboard";
        String seedNum = currentSeed + "/" + seedCount;

        if (true) {
            Graphics2D g = title.createGraphics();
            g.setFont(font80);
            g.setBackground(main);
            g.clearRect(0, 0, 728, 100);
            g.drawString(titleTxt, 10, 82);
            g.clearRect(738, 0, 203, 100);
            g.drawString(seedNum, 748, 82);
            g.dispose();
        }

        ArrayList<BufferedImage> blocks = new ArrayList<>();
        int[] a = getSize(players.length());
        int col = a[0];
        int width = a[1];
        int ro = players.length() / col;
        if (col * ro < players.length()) ro++;

        for (int i = 0; i < players.length(); i++) {
            JSONObject o = players.getJSONObject(i);
            String name = o.getString("name");
            String average = o.getString("average");
            String points = o.getInt("points") + "";

            int height = width/5;
            int margin = width/60;

            BufferedImage head = Api.getHead(name, height - (margin * 2));

            int fontsize = (height - (margin * 3)) / 2;
            int namesize = fontsize;

            int ln = name.length() * Math.round(fontsize * 0.8f) + margin;
            if (ln > width - height - (margin * 2)) {
                Double f = width - height - (margin * 2.0);
                f /= ln;
                namesize = (int) Math.round(Math.floor(fontsize * f));
            }

            BufferedImage block = new BufferedImage(width, height, 2);
            Graphics2D g = block.createGraphics();
            g.setBackground(main);

            if (i == 0) g.setBackground(promote);

            g.clearRect(0, 0, width, height);
            g.setColor(white);
            g.setFont(new Font("Mojang", 1, namesize));
            g.drawImage(head, margin, margin, null);
            g.drawString(name, height + margin, (height + margin) / 2 - margin);
            g.setColor(gray);
            g.setFont(new Font("Mojang", 1, fontsize));
            g.drawString(average, height + margin, height - margin);
            g.drawString(points, width - (Math.round(fontsize * 0.75f) * points.length()) - margin, height - margin);

            g.dispose();

            blocks.add(block);
        }

        BufferedImage render = new BufferedImage(1920, 846, 2);

        Graphics2D g = render.createGraphics();

        g.setBackground(bg);
        g.clearRect(0, 0, 1920, 846);

        g.drawImage(title, 10, 10 ,null);

        int curCol = 0;

        for (int i = 0; i < blocks.size(); i++) {
            int i2 = i;
            i2 -= ro * curCol;
            if (i2 == ro) curCol++;

            int y = i;
            y -= ro * curCol;

            g.drawImage(blocks.get(i), ((width + 10) * curCol) + 10, 120 + y * (width / 5 + 10), null);
        }

        g.dispose();

        File outputFolder = Paths.get("gen_images").toFile();
        if (!outputFolder.exists()) outputFolder.mkdirs();

        File output = Paths.get("gen_images", "leaderboard.png").toFile();

        try {
            ImageIO.write(render, "png", output);
        } catch (IOException e) {
            System.out.println("IOException: failed to write image file");
            return;
        }

        System.out.println("Image generated successfully");
    }

    public static void averages(JSONObject lb) {

        JSONArray players = lb.getJSONArray("players");

        int promotions = lb.optInt("promotions", 0);
        int demotions = lb.optInt("demotions", 0);

        BufferedImage title = new BufferedImage(970, 100, 2);
        String titleTxt = "Multi-week scores";

        if (true) {
            Graphics2D g = title.createGraphics();
            g.setFont(font80);
            g.setBackground(main);
            g.clearRect(0, 0, 970, 100);
            g.drawString(titleTxt, 10, 82);
            g.dispose();
        }

        ArrayList<BufferedImage> blocks = new ArrayList<>();
        int[] a = getSize(players.length());
        int col = a[0];
        int width = a[1];
        int ro = players.length() / col;
        if (col * ro < players.length()) ro++;

        for (int i = 0; i < players.length(); i++) {
            JSONObject o = players.getJSONObject(i);
            String name = o.getString("name");
            String perf = o.getString("perfMulti");

            if (perf.equals("100.0%")) perf = "Winner";

            int height = width/5;
            int margin = width/60;

            BufferedImage head = Api.getHead(name, height - (margin * 2));

            int fontsize = (height - (margin * 3)) / 2;
            int namesize = fontsize;

            int ln = name.length() * Math.round(fontsize * 0.8f) + margin;
            if (ln > width - height - (margin * 2)) {
                Double f = width - height - (margin * 2.0);
                f /= ln;
                namesize = (int) Math.round(Math.floor(fontsize * f));
            }

            BufferedImage block = new BufferedImage(width, height, 2);
            Graphics2D g = block.createGraphics();
            g.setBackground(main);

            if (i < promotions) g.setBackground(promote);
            if (i >= players.length() - demotions) g.setBackground(demote);
            if (perf.equals("0.0%")) {
                if (lb.optBoolean("initial", false)) {
                    perf = "None";
                } else if (demotions > 0) {
                    g.setBackground(demote);
                }
            }

            g.clearRect(0, 0, width, height);
            g.setColor(white);
            g.setFont(new Font("Mojang", 1, namesize));
            g.drawImage(head, margin, margin, null);
            g.drawString(name, height + margin, (height + margin) / 2 - margin);
            g.setColor(gray);
            g.setFont(new Font("Mojang", 1, fontsize));
            g.drawString(perf, height + margin, height - margin);

            g.dispose();

            blocks.add(block);
        }

        BufferedImage render = new BufferedImage(1920, 846, 2);

        Graphics2D g = render.createGraphics();

        g.setBackground(bg);
        g.clearRect(0, 0, 1920, 846);

        g.drawImage(title, 10, 10 ,null);

        int curCol = 0;

        for (int i = 0; i < blocks.size(); i++) {
            int i2 = i;
            i2 -= ro * curCol;
            if (i2 == ro) curCol++;

            int y = i;
            y -= ro * curCol;

            g.drawImage(blocks.get(i), ((width + 10) * curCol) + 10, 120 + y * (width / 5 + 10), null);
        }

        g.dispose();

        File outputFolder = Paths.get("gen_images").toFile();
        if (!outputFolder.exists()) outputFolder.mkdirs();

        File output = Paths.get("gen_images", "multiweek.png").toFile();

        try {
            ImageIO.write(render, "png", output);
        } catch (IOException e) {
            System.out.println("IOException: failed to write image file");
            return;
        }

        System.out.println("Image generated successfully");
    }

    private static int[] getSize(int size) {
        if (size <= 2 * 3) return new int[] {2, 900};
        if (size <= 3 * 5) return new int[] {3, 600};
        if (size <= 4 * 7) return new int[] {4, 450};
        if (size <= 5 * 9) return new int[] {5, 355};
        if (size <= 6 * 10) return new int[] {6, 300};
        if (size <= 7 * 11) return new int[] {7, 265};
        if (size <= 8 * 13) return new int[] {8, 225};
        if (size <= 9 * 14) return new int[] {9, 200};
        return new int[] {10, 180};
    }

    private static int getCol(int width) {
        int max = 726;

        int height = width / 5;
        int total = height;
        int i = 0;

        while (total <= max) {
            total += 10;
            total += height;
            i++;
        }

        return i;
    }
}
