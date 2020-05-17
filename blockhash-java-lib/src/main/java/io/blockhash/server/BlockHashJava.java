package io.blockhash.server;

import io.blockhash.core.BlockhashCore;
import io.blockhash.core.RGBAImageDataFromARGBPixelsSimple;
import io.blockhash.core.RGBAImageDataInterface;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class BlockHashJava {
    private static RGBAImageDataInterface fromBufferedImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int[] argbPixels = bufferedImage.getRGB(0, 0, width, height, null, 0, width);
        return new RGBAImageDataFromARGBPixelsSimple(argbPixels, width, height);
    }

    public static RGBAImageDataInterface fromImagePath(String imagePath) {
        return fromImagePath(imagePath, false);
    }

    public static RGBAImageDataInterface fromImagePath(String imagePath, boolean useBuiltinImageReader) {
        int[] argbPixels;
        int width;
        int height;
        try (InputStream inputStream = new FileInputStream(imagePath)) {
            /*
            Earlier, we were using Java built-in ImageIO to read image.
            But for many images, pixel values returned by built-in library
            were way off from what nodejs or Android bitmap return.
            Apache's imaging is much more closer to other platforms.
             */
            if (useBuiltinImageReader) {
                return fromBufferedImage(ImageIO.read(inputStream));
            } else {
                return fromBufferedImage(Imaging.getBufferedImage(inputStream));
            }
        } catch (IOException | ImageReadException e) {
            throw new RuntimeException(e);
        }
    }

    public static String computeBlockHash(String imagePath, int bits) {
        return BlockhashCore.blockHashHex(fromImagePath(imagePath), bits);
    }

    public static String computeBlockHash(String imagePath, int bits, boolean useBuiltInImageReader) {
        return BlockhashCore.blockHashHex(fromImagePath(imagePath, useBuiltInImageReader), bits);
    }
}
