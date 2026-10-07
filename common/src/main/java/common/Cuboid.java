package common;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;

public class Cuboid {

    Point start, a, b, c, d, e, f, end;
    int x, y, z;

    public void print() {
        System.out.println("Start " + start.x + " " + start.y);
        System.out.println("A " + a.x + " " + a.y);
        System.out.println("B " + b.x + " " + b.y);
        System.out.println("C " + c.x + " " + c.y);
        System.out.println("D " + d.x + " " + d.y);
        System.out.println("E " + e.x + " " + e.y);
        System.out.println("F " + f.x + " " + f.y);
        System.out.println("End " + end.x + " " + end.y);
    }

    public void setCorner(Point newCorner) {
        int dX = newCorner.x - f.x;
        int dY = newCorner.y - a.y;

        move(dX, dY);
    }

    public void setStart(Point newStart) {
        int dX = newStart.x - start.x;
        int dY = newStart.y - start.y;

        move(dX, dY);
    }

    public void setA(Point newA) {
        int dX = newA.x - a.x;
        int dY = newA.y - a.y;

        move(dX, dY);
    }

    public void setB(Point newB) {
        int dX = newB.x - b.x;
        int dY = newB.y - b.y;

        move(dX, dY);
    }

    public void setDEcenter(Point newD, Point newE) {
        Double x1 = (newD.x + newE.x) * 0.5;
        Double x2 = (d.x + e.x) * 0.5;

        int dX = (int) Math.round(x1 - x2);

        Double y1 = (newD.y + newE.y) * 0.5;
        Double y2 = (d.y + e.y) * 0.5;

        int dY = (int) Math.round(y1 - y2);

        move(dX, dY);
    }

    public void setADcenter(Point newA, Point newD) {
        Double x1 = (newA.x + newD.x) * 0.5;
        Double x2 = (a.x + d.x) * 0.5;

        int dX = (int) Math.round(x1 - x2);

        Double y1 = (newA.y + newD.y) * 0.5;
        Double y2 = (a.y + d.y) * 0.5;

        int dY = (int) Math.round(y1 - y2);

        move(dX, dY);
    }

    void move (int dX, int dY) {
        start.x += dX;
        a.x += dX;
        b.x += dX;
        c.x += dX;
        d.x += dX;
        e.x += dX;
        f.x += dX;
        end.x += dX;

        start.y += dY;
        a.y += dY;
        b.y += dY;
        c.y += dY;
        d.y += dY;
        e.y += dY;
        f.y += dY;
        end.y += dY;
    }


    public Cuboid(int scale, int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;

        start = new Point(0, 0);

        Double scaleMain = scale * 1.0;

        double angle = Math.toRadians(60.0);
        Double r1 = scaleMain * x;

        double sin1 = r1 * Math.sin(angle);
        double cos1 = r1 * Math.cos(angle);

        int xR = (int) Math.round(sin1);
        int yR = (int) Math.round(cos1) * -1;

        b = new Point(xR, yR);

        double r2 = scaleMain * z;

        double sin2 = r2 * Math.sin(angle);
        double cos2 = r2 * Math.cos(angle);

        int xL = (int) Math.round(sin2) * -1;
        int yL = (int) Math.round(cos2) * -1;

        f = new Point(xL, yL);

        Double r3 = scaleMain * y;
        int yD = (int) Math.round(r3);

        d = new Point(0, yD);

        c = new Point(b.x, b.y + d.y);
        e = new Point(f.x, f.y + d.y);
        a = new Point(b.x + f.x, b.y + f.y);

        end = new Point(a.x, d.y + a.y);

        setCorner(new Point(0, 0));
    }

    public BufferedImage trace() {
        BufferedImage img = new BufferedImage(b.x + 1, d.y + 1, 2);

        Graphics2D g = img.createGraphics();

        g.setBackground(new Color(0, 0, 0));

        g.setColor(new Color(0, 0, 255));

        g.drawLine(end.x, end.y, a.x, a.y);
        g.drawLine(end.x, end.y, c.x, c.y);
        g.drawLine(end.x, end.y, e.x, e.y);

        g.setColor(new Color(255, 0, 0));

        g.drawLine(start.x, start.y, b.x, b.y);
        g.drawLine(start.x, start.y, d.x, d.y);
        g.drawLine(start.x, start.y, f.x, f.y);

        g.drawLine(a.x, a.y, b.x, b.y);
        g.drawLine(b.x, b.y, c.x, c.y);
        g.drawLine(c.x, c.y, d.x, d.y);
        g.drawLine(d.x, d.y, e.x, e.y);
        g.drawLine(e.x, e.y, f.x, f.y);
        g.drawLine(f.x, f.y, a.x, a.y);

        g.dispose();

        return img;
    }

    public BufferedImage[] drawTexture(BufferedImage texture) {
        BufferedImage frontImg = new BufferedImage(b.x + 1, d.y + 1, 2);
        BufferedImage backImg = new BufferedImage(b.x + 1, d.y + 1, 2);

        BufferedImage front, side, top, sideBack, back, bottom;

        front = texture.getSubimage(z,z,x,y);
        side = texture.getSubimage(0,z,z,y);
        top = texture.getSubimage(z,0,x,z);

        sideBack = texture.getSubimage(z+x,z,x,y);
        back = texture.getSubimage(z+x+x,z,z,y);
        bottom = texture.getSubimage(z+x,0,x,z);

        BufferedImage warpTop = Api.getWarped(top, f, a, start, b);
        BufferedImage warpSide = Api.getWarped(side, f, start, e, d);
        BufferedImage warpFront = Api.getWarped(front, start, b, d, c);

        BufferedImage warpBottom = Api.getWarped(bottom, e, end, d, c);
        BufferedImage warpSideBack = Api.getWarped(sideBack, b, a, c, end);
        BufferedImage warpBack = Api.getWarped(back, a, f, end, e);

        Graphics2D g1 = frontImg.createGraphics();
        Graphics2D g2 = backImg.createGraphics();

        g1.drawImage(warpTop, 0, 1, null);
        g1.drawImage(warpTop, 0, 0, null);

        g1.drawImage(warpSide, 0, 0, null);
        g1.drawImage(warpFront, 0, 0, null);

        g2.drawImage(warpBack, 0, 0, null);
        g2.drawImage(warpSideBack, 0, 0, null);
        g2.drawImage(warpBottom, 0, 0, null);

        g1.dispose();
        g2.dispose();

        return new BufferedImage[] {backImg, frontImg};
    }

}