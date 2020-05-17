package io.blockhash.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;

import io.blockhash.core.BlockhashCore;
import io.blockhash.core.RGBAImageDataFromARGBPixelsSimple;
import io.blockhash.core.RGBAImageDataInterface;

public class BlockhashAndroid {
    public static RGBAImageDataInterface fromImagePath(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

        if (bitmap == null) {
            throw new RuntimeException(new IOException("Failed to decode. Path is incorrect or image is corrupted"));
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] argbPixels = new int[width * height];
        bitmap.getPixels(argbPixels, 0, width, 0, 0, width, height);
        return new RGBAImageDataFromARGBPixelsSimple(argbPixels, width, height);
    }

    public static String computeBlockHash(String imagePath, int bits) {
        return BlockhashCore.blockHashHex(fromImagePath(imagePath), bits);
    }
}
