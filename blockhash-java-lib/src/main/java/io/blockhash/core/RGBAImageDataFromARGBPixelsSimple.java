package io.blockhash.core;

public class RGBAImageDataFromARGBPixelsSimple implements RGBAImageDataInterface {
    private final int[] argbPixels;
    private final int width;
    private final int height;

    public RGBAImageDataFromARGBPixelsSimple(int[] argbPixels, int width, int height) {
        this.argbPixels = argbPixels;
        this.width = width;
        this.height = height;
    }

    @Override
    public int value(int index) {
        int pixelIndex = index / 4;
        int rgbaIndex = index % 4;
        int pixel = this.argbPixels[pixelIndex];
        if (rgbaIndex == 0) {
            // Red
            return (pixel >> 16) & 0xff;
        } else if (rgbaIndex == 1) {
            // Green
            return (pixel >>  8) & 0xff;
        } else if (rgbaIndex == 2) {
            // Blue
            return pixel & 0xff;
        } else {
            // Alpha
            return (pixel >> 24) & 0xff;
        }
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }
}
