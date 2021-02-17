package fr.jydet.roboto;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GarlicScreenTaker {

    public static final int PSEUDO_OFFSET = 40;
    public static final int TOP_Y = 391;
    public static final int BOTTOM_Y = 999;
    public static final int HEIGHT = BOTTOM_Y - TOP_Y;

    public static int scrollAmount = 0;
    public static Robot robot;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        robot.mouseMove(Direction.RIGHT.x, TOP_Y + 30);
        ArrayList<DetectedBox> rect2 = getRect2(Direction.LEFT);

        robot.mouseMove(1713,369 + scrollAmount);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        scroll(50);

        Thread.sleep(50);
        ArrayList<DetectedBox> rect3 = getRect2(Direction.LEFT);

        double scrollRatio = ((double) (rect2.get(0).miny - rect3.get(0).miny)) / scrollAmount;
        System.out.println("scrollRatio = " + scrollRatio);

        scroll(-60);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        scrollAmount = 0;

        while (scrollAmount < BOTTOM_Y) {
            Thread.sleep(50);
            ArrayList<DetectedBox> rightRect = getRect2(Direction.LEFT);
            rightRect.addAll(getRect2(Direction.RIGHT));
            rightRect.forEach(System.out :: println);

            int bottomScreen = rightRect.stream().mapToInt(b -> b.maxy).max().getAsInt();
            int topScreen = rightRect.stream().mapToInt(b -> b.miny).min().getAsInt();
            System.out.println(bottomScreen);
            takeScreen(topScreen + TOP_Y, (bottomScreen - topScreen));

            robot.mouseMove(1713,369 + scrollAmount);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            scroll(bottomScreen);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }

        if (roundImages.size() > 1) {
            int heightTotal = 0;
            for (BufferedImage roundImage : roundImages) {
                heightTotal = heightTotal + roundImage.getHeight();
            }
            int heightCurr = 0;
            BufferedImage concatImage = new BufferedImage(roundImages.get(0).getWidth(), heightTotal, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = concatImage.createGraphics();
            for (BufferedImage image : roundImages) {
                g2d.drawImage(image, 0, heightCurr, null);
                heightCurr += image.getHeight();
            }
            g2d.dispose();
            ImageIO.write(concatImage, "png", new File("final.png"));
        }
    }

    private static final int OFFSET = 10;

    private static final List<BufferedImage> roundImages = new ArrayList<>();

    private static void takeScreen(int y, int height) {
        BufferedImage screenCapture = robot.createScreenCapture(new Rectangle(853, y - OFFSET - PSEUDO_OFFSET, 870, height + OFFSET + OFFSET + PSEUDO_OFFSET));
        roundImages.add(screenCapture);
    }

    static void scroll(int amount) {
        scrollAmount = scrollAmount + amount;
        Point location = MouseInfo.getPointerInfo().getLocation();
        robot.mouseMove(location.x, location.y + amount);
    }

    public static Color nameColor = new Color(234,204,252);

    public static ArrayList<DetectedBox> getRect2(Direction direction) {
        robot.waitForIdle();
        BufferedImage screenCapture = robot.createScreenCapture(new Rectangle(direction.x, TOP_Y, 1, HEIGHT));
        int[] pixels = ((DataBufferInt) screenCapture.getRaster().getDataBuffer()).getData();
        ArrayList<DetectedBox> res = new ArrayList<>();
        boolean isWhite = similarTo(Color.white, new Color(pixels[0]));
        int last = pixels[0];
        for (int i = 1; i < pixels.length; i++) {
            int pixel = pixels[i];
            if (last != pixel) {
                Color pixelColor = new Color(pixel);
                if (isWhite) {
                    if (! similarTo(Color.white, pixelColor)) {
                        isWhite = false;
                        if (! res.isEmpty()) {
                            res.get(res.size() - 1).maxy = i ;
                        }
                    }
                } else {
                    if (! similarTo(nameColor, pixelColor)) {
                        if (similarTo(Color.white, pixelColor)) {
                            isWhite = true;
                            res.add(new DetectedBox(i));
                        }
                    }
                }
                last = pixel;
            }
        }
        return res;
    }

    @ToString
    @RequiredArgsConstructor
    @Setter
    public static class DetectedBox {
        final int miny;
        int maxy = -1;
    }

    enum Direction {
        LEFT(976),
        RIGHT(1600);

        Direction(int x) {
            this.x = x;
        }

        final int x;
    }

    static boolean similarTo(Color c, Color c2){
        double distance = (c.getRed() - c2.getRed())*(c.getRed() - c2.getRed()) + (c.getGreen() - c2.getGreen())*(c.getGreen() - c2.getGreen()) + (c.getBlue() - c2.getBlue())*(c.getBlue() - c2.getBlue());
        return distance < 2500;
    }
}
