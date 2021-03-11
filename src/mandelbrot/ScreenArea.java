package mandelbrot;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class ScreenArea {
    private final int width;
    private final int height;
    private final int screenWidth;
    private final int screenHeight;
    private final int offsetX;
    private final int offsetY;
    private final int[] buffer;
    private BufferedImage image;

    public ScreenArea(int width, int height, int offsetX, int offsetY, int screenWidth, int screenHeight) {
        this.width = width;
        this.height = height;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.buffer = new int[width*height];
    }

    public void calculate(double centerX, double centerY, double scaleX, double scaleY, int maxN, double maxRadius, Color[] colors) {
        final double invX1 = (double)offsetX * scaleX / (double)screenWidth - scaleX / 2.0d + centerX;
        final double invX2 = scaleX / (double)screenWidth;
        final double invY = (double)offsetY * scaleY / (double)screenHeight - scaleY / 2.0d + centerY;
        final double maxRadiusSq = maxRadius * maxRadius;

        for (int y = 0; y < height; y++) {
            final int offsetRow = y * width;
            final double ci = (double)y * scaleY / (double)screenHeight + invY;

            for (int x = y % 2; x < width; x += 2) {
                // final double cr = (double) (x + offsetX) * scaleX / (double) screenWidth - scaleX / 2.0d + centerX;
                // final double ci = (double) (y + offsetY) * scaleY / (double) screenHeight - scaleY / 2.0d + centerY;
                final double cr = (double)x * invX2 + invX1;

                int n = 1;
                double zr = 0;
                double zi = 0;
                do {
                    final double zr1 = zr * zr - zi * zi;
                    final double zi1 = 2.0d * zr * zi;
                    zr = zr1 + cr;
                    zi = zi1 + ci;
                    if ((zr > maxRadius) || (zi > maxRadius) || (zr * zr + zi * zi > maxRadiusSq)) {
                        break;
                    }
                    n++;
                } while (n <= maxN);

                buffer[offsetRow + x] = n > maxN ? 0 : n;
            }
        }

        for (int y = 0; y < height; y++) {
            final int offsetRow = y * width;
            final int offsetPrevRow = (y - 1) * width;
            final int offsetNextRow = (y + 1) * width;
            for (int x = 1 - y % 2; x < width; x += 2) {
                final int offset = offsetRow + x;
                if (x == 0) {
                    buffer[offset] = buffer[offset + 1];
                } else if (x == width - 1) {
                    buffer[offset] = buffer[offset - 1];
                } else {
                    if (y > 0 && y < height - 1) {
                        buffer[offset] = (buffer[offset - 1] + buffer[offset + 1] + buffer[offsetPrevRow + x] + buffer[offsetNextRow + x]) / 4;
                    } else {
                        buffer[offset] = (buffer[offset - 1] + buffer[offset + 1]) / 2;
                    }
                }
            }
        }

        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();

        for (int y = 0; y < height; y++) {
            final int offsetRow = y * width;
            for (int x = 0; x < width; x++) {
                final int offset = offsetRow + x;
                pixels[offset] = colors[buffer[offset]].getRGB();
            }
        }
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public BufferedImage getImage() {
        return image;
    }
}
