package scoreboard;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import common.Api;

class Block {
    BlockElements elements;

    BufferedImage combinedImg;

    int avgFade = 30;
    String newAverage;

    int scale;

    int u1, u5, u6, u25;
    int m1, um;

    boolean aReady = false;
    BufferedImage aImg;
    int gainPoints;
    int newPoints;
    int delay;

    boolean appeared = false;
    int appearN = 0;
    int appearS = 30;

    boolean move = false;
    boolean colorChange = false;

    Color color;

    public BufferedImage getFrame() {
        if (elements.checkUpdates()) combine();
        return combinedImg;
    }

    public void appearAnimation() {
        if (appearN == appearS) appeared = true;
        float a = (appearN * 1.0f / appearS);
        elements.setAlpha(a);
        appearN++;
    }

    public void prepAnimation(int gainPoints, int newPoints, Color color, int delay) {
        this.delay = delay;
        this.gainPoints = gainPoints;
        this.newPoints = newPoints;

        aImg = new BufferedImage(u1, u1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = aImg.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, u1, u1);

        g.setColor(Color.WHITE);
        String pts = gainPoints + "";

        Font pointFont = new Font("Mojang", 1, 160 / scale);
        g.setFont(pointFont);
        if (gainPoints < 10) {
            g.drawString(pts, 98 / scale, 220 / scale);
        } else {
            g.drawString(pts, 33 / scale, 220 / scale);
        }

        g.dispose();

        aReady = true;
    }

    public void avgFade(int frame) {
        int fade;

        if (frame <= 30) fade = 30 - frame; else fade = frame - 30;
        if (frame == 30) setAverage(newAverage);
        
        Float a = (fade * 1.0f) / 30;
        if (a > 1.0f) a = 1.0f;
        elements.average.setAlpha(a);
    }

    public void animate(int frame) {

        int localFrame = frame - delay;

        if (!aReady) return;
        if (localFrame > 11 && localFrame < 80) return;
        if (localFrame < 1) return;
        if (localFrame > 90) {
            aReady = false;
            return;
        }

        elements.newPoints.newImg();
        Graphics2D g = elements.newPoints.img.createGraphics();

        int rY = 0, rH = u1;
        if (localFrame > 80) {
            rY = u1 - (int) Math.round(u1 / 10d * (10 - (localFrame - 80)));
            rH = u1 - rY;
        }

        if (localFrame < 11) {
            rH = u1 - (int) Math.round(u1 / 10d * (10 - localFrame));
        }

        if (localFrame == 11) {
            setPoints(newPoints);
        }

        g.setClip(0, rY, u1, rH);
        g.drawImage(aImg, 0, 0, null);

        g.dispose();

        return;
    }

    Block(Color color, int scale) {
        init(color, scale);
    }

    private void init(Color color, int scale) {
        this.color = color;
        rescale(scale);
    }

    private void scale() {
        u1 = 300 / scale;
        u6 = u1 * 6;
        u5 = u1 * 5;
        u25 = u5 / 2;
        m1 = 10 / scale;
        um = u1 / 2;
        if (m1 < 2) m1 = 2;
    }

    void rescale(int scale) {
        this.scale = scale;
        scale();

        elements = new BlockElements();

        elements.background = new RenderElement(new BufferedImage(u6, u1, BufferedImage.TYPE_INT_ARGB), 0, 0, 1.0f);
        elements.player = new RenderElement(new BufferedImage(u5, u1, BufferedImage.TYPE_INT_ARGB), 0, 0, 1.0f);
        elements.points = new RenderElement(new BufferedImage(u1, u1, BufferedImage.TYPE_INT_ARGB), u5, 0, 1.0f);
        elements.newPoints = new RenderElement(new BufferedImage(u1, u1, BufferedImage.TYPE_INT_ARGB), u5, 0, 1.0f);
        elements.average = new RenderElement(new BufferedImage(u25, um, BufferedImage.TYPE_INT_ARGB), u1, u1 - um, 1.0f);

        setColor();
    }

    void newColor(Color color) {
        this.color = color;

        setColor();
    }

    void setColor() {
        elements.background.newImg();

        Graphics2D g = elements.background.img.createGraphics();

        g.setColor(color);
        g.fillRect(0, 0, u6, u1);
        g.dispose();

        elements.update();
    }

    private void combine() {
        combinedImg = new BufferedImage(u6, u1, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = combinedImg.createGraphics();

        g.drawImage(elements.background.get(), 0, 0, null);
        g.drawImage(elements.player.get(), 0, 0, null);
        g.drawImage(elements.average.get(), elements.average.x, elements.average.y, null);
        g.drawImage(elements.points.get(), elements.points.x, elements.points.y, null);
        if (aReady) g.drawImage(elements.newPoints.get(), elements.newPoints.x, elements.newPoints.y, null);

        g.dispose();

        elements.loadAll();
    }

    void setPlayer(String username) {
        elements.player.newImg();

        Graphics2D g = elements.player.img.createGraphics();

        int headSize = u1 - (m1 * 2);

        BufferedImage head = Api.getHead(username, headSize, true);

        g.drawImage(head, m1, m1, null);

        g.setColor(Color.WHITE);
        Font fontName = new Font("Mojang", 1, fontSize(username.length()));
        g.setFont(fontName);
        g.drawString(username, u1 + m1, um);

        g.dispose();
    }

    void setNewAverage(String average) {
        newAverage = average;
    }

    void setAverage(String input) {
        elements.average.newImg();

        Graphics2D g = elements.average.img.createGraphics();

        g.setColor(Main.avgColor);

        Font fontName = new Font("Mojang", 1, fontSize(12));
        g.setFont(fontName);
        g.drawString(input, m1, um - (m1 * 2));

        g.dispose();
    }

    void setPoints(int input) {
        elements.points.newImg();

        Graphics2D g = elements.points.img.createGraphics();

        String pointString = input + "";

        g.setColor(Color.WHITE);

        Font pointFont = new Font("Mojang", 1, 160 / scale);
        g.setFont(pointFont);
        if (input < 10) {
            g.drawString(pointString, 98 / scale, 220 / scale);
        } else {
            g.drawString(pointString, 33 / scale, 220 / scale);
        }
        g.dispose();
    }

    private int fontSize(int length) {
        if (length == 16) return 90 / scale;
        if (length == 15) return 95 / scale;
        if (length == 14) return 100 / scale;
        if (length == 13) return 105 / scale;
        if (length == 12) return 110 / scale;
        if (length == 11) return 115 / scale;

        return 120 / scale;

    }
}

class BlockElements {
    RenderElement background;
    RenderElement player;
    RenderElement points;
    RenderElement newPoints;
    RenderElement average;

    boolean requestUpdate = true;

    void update() {
        requestUpdate = true;
    }
    
    void setAlpha(float a) {
        background.setAlpha(a);
        player.setAlpha(a);
        points.setAlpha(a);
        average.setAlpha(a);
    }

    void loadAll() {
        background.load(); player.load(); points.load(); newPoints.load(); average.load();
    }

    boolean checkUpdates() {
        if (requestUpdate) {
            requestUpdate = false;
            return true;
        }
        if (background.load && player.load && points.load && newPoints.load && average.load) return false;
        return true;
    }
}