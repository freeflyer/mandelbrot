package mandelbrot;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.util.Arrays;

public class Mandelbrot implements KeyListener, MouseMotionListener, MouseWheelListener, MouseListener {

    private static int MAX_N = 50; // higher values --> more details, but probably lower fps
    private static float MAX_RADIUS = 50.0f;
    private static int AREAS = 20; // number of screen areas calculated in parallel --> it's better if equals to number of CPU hardware threads

    private static Color[] outColors = new Color[MAX_N+1];

    private GraphicsEnvironment env;
    private GraphicsDevice device;
    private GraphicsConfiguration gc;
    private Frame mainFrame;
    private BufferStrategy bufferStrategy;
    private Rectangle bounds;

    private ScreenArea[] areas = new ScreenArea[AREAS];

    private Object lock = new Object();

    private boolean isWorking = true;
    double centerX = 0.0d;
    double centerY = 0.0d;
    double scaleX = 8.0d;
    double scaleY = 4.0d;
    double speed =  0.0001d;

    double clickSpeed;
    double clickCenterX;
    double clickCenterY;
    double clickX;
    double clickY;

    private long[] frameTimes = new long[100];
    private int frames = 0;
    private int fps = 0;

    public Mandelbrot() {
        try {
            env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            device = env.getDefaultScreenDevice();

            gc = device.getDefaultConfiguration();
            mainFrame = new Frame(gc);
            mainFrame.setUndecorated(true);
            mainFrame.setIgnoreRepaint(true);
            device.setFullScreenWindow(mainFrame);

            bounds = mainFrame.getBounds();
            mainFrame.createBufferStrategy(2);
            bufferStrategy = mainFrame.getBufferStrategy();

            mainFrame.addKeyListener(this);
            mainFrame.addMouseMotionListener(this);
            mainFrame.addMouseListener(this);
            mainFrame.addMouseWheelListener(this);

            for (int n = 0; n <= MAX_N; n++) {
                float br = (float)n / (float)MAX_N;
                outColors[n] = Color.getHSBColor(0.5f + br / 2.0f, 0.5f + br / 2.0f, br);
                // outColors[n] = new Color(0,n * 255 / MAX_N, 0); // alternative green colors palette
            }

            Arrays.fill(frameTimes, 0);

            final int areaWidth = bounds.width / AREAS;
            for (int i = 0; i < AREAS; i++) {
                areas[i] = new ScreenArea(areaWidth, bounds.height, i*areaWidth, 0, bounds.width, bounds.height);
            }
        } catch (Exception e) {
            if (device != null) {
                device.setFullScreenWindow(null);
            }
            throw e;
        }
    }

    private void drawPicture() {
        try {
            if (!bufferStrategy.contentsLost()) {
                long lastTime = System.currentTimeMillis();

                while (isWorking) {
                    final double centerX1;
                    final double centerY1;
                    final double scaleX1;
                    final double scaleY1;
                    synchronized (lock) {
                        centerX1 = centerX;
                        centerY1 = centerY;
                        scaleX1 = scaleX;
                        scaleY1 = scaleY;
                    }
                    Arrays.stream(areas).parallel()
                            .forEach(area -> area.calculate(centerX1, centerY1, scaleX1, scaleY1, MAX_N, MAX_RADIUS, outColors));

                    Graphics g = bufferStrategy.getDrawGraphics();

                    for (int i = 0; i < AREAS; i++) {
                        final ScreenArea area = areas[i];
                        g.drawImage(area.getImage(), area.getOffsetX(), area.getOffsetY(), null);
                    }

                    if (fps > 0) {
                        g.setColor(Color.LIGHT_GRAY);
                        g.drawString("FPS: " + fps, 20, 20);
                    }

                    bufferStrategy.show();
                    g.dispose();

                    long currentTime = System.currentTimeMillis();
                    synchronized (lock) {
                        long frameTime = currentTime - lastTime;

                        for (int i = 1; i < frameTimes.length; i++) {
                            frameTimes[i-1] = frameTimes[i];
                        }
                        frameTimes[frameTimes.length - 1] = frameTime;

                        if (frames++ >= frameTimes.length) {
                            long sumTime = 0;
                            for (int i = 0; i < frameTimes.length; i++) {
                                sumTime += frameTimes[i];
                            }
                            fps = (int) (1000 * frameTimes.length / sumTime);
                        }

                        double ratio = frameTime * speed;
                        scaleX -= scaleX * ratio;
                        scaleY -= scaleY * ratio;
                    }
                    lastTime = currentTime;
                }
            }
        } finally {
            if (device != null) {
                device.setFullScreenWindow(null);
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == 27) { // ESC
            isWorking = false;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        synchronized (lock) {
            double currentX = getMappedX(e.getX(), clickCenterX);
            double currentY = getMappedY(e.getY(), clickCenterY);
            centerX = clickCenterX - (currentX - clickX);
            centerY = clickCenterY - (currentY - clickY);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        speed -= e.getWheelRotation() * e.getScrollAmount() * 0.00002d;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        synchronized (lock) {
            clickX = getMappedX(e.getX(), centerX);
            clickY = getMappedY(e.getY(), centerY);
            clickCenterX = centerX;
            clickCenterY = centerY;
            clickSpeed = speed;
            speed = 0;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        synchronized (lock) {
            speed = clickSpeed;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    private double getMappedX(int screenX, double centerX) {
        return  (double)screenX * scaleX / (double)bounds.width - scaleX / 2.0d + centerX;
    }

    private double getMappedY(int screenY, double centerY) {
        return (double)screenY * scaleY / (double)bounds.height - scaleY / 2.0d + centerY;
    }

    public static void main(String args[]) {
        try {
            Mandelbrot mandelbrot = new Mandelbrot();
            mandelbrot.drawPicture();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
