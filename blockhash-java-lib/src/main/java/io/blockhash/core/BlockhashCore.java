package io.blockhash.core;

import java.util.Arrays;

public class BlockhashCore {

    private static double median(final int[] data, final int start, final int size) {
        int[] dataCpy = Arrays.copyOfRange(data, start, start + size);
        Arrays.sort(dataCpy);
        if (dataCpy.length % 2 == 0)
            return ((double) dataCpy[dataCpy.length/2] + (double)dataCpy[dataCpy.length/2 - 1])/2;
        else
            return (double) dataCpy[dataCpy.length/2];
    }

    private static double medianDouble(final double[] data, final int start, final int size) {
        double[] dataCpy = Arrays.copyOfRange(data, start, start + size);
        Arrays.sort(dataCpy);
        if (dataCpy.length % 2 == 0)
            return (dataCpy[dataCpy.length/2] + dataCpy[dataCpy.length/2 - 1])/2;
        else
            return dataCpy[dataCpy.length/2];
    }

    /** Compare medians across four horizontal bands
     *
     * Output a 1 if the block is brighter than the median.
     * With images dominated by black or white, the median may
     * end up being 0 or the max value, and thus having a lot
     * of blocks of value equal to the median.  To avoid
     * generating hashes of all zeros or ones, in that case output
     * 0 if the median is in the lower value space, 1 otherwise
     */
    private static void translateBlocksToBits(int[] blocks, int pixelsPerBlock) {
        int nBlocks = blocks.length;
        double halfBlockValue = pixelsPerBlock * 256 * 3 / 2;
        int bandSize = nBlocks / 4;

        for (int i = 0; i < 4; i++) {
            int m = (int) median(blocks, i * bandSize, bandSize);
            for (int j = i * bandSize; j < (i + 1) * bandSize; j++) {
                int v = blocks[j];
                blocks[j] = (v > m || (Math.abs(v - m) < 1 && m > halfBlockValue)) ? 1 : 0;
            }
        }
    }

    private static void translateBlocksToBitsDouble(double[] blocks, int[] result, int pixelsPerBlock) {
        int nBlocks = blocks.length;
        double halfBlockValue = pixelsPerBlock * 256 * 3 / 2;
        int bandSize = nBlocks / 4;

        for (int i = 0; i < 4; i++) {
            double m = medianDouble(blocks, i * bandSize, bandSize);
            for (int j = i * bandSize; j < (i + 1) * bandSize; j++) {
                double v = blocks[j];
                result[j] = (v > m || (Math.abs(v - m) < 1 && m > halfBlockValue)) ? 1 : 0;
            }
        }
    }

    private static String bitsToHexHash(int[] binary) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < binary.length; i+=4) {
            StringBuilder nibble = new StringBuilder();
            for (int j = 0; j < 4; j++) {
                if (i + j >= binary.length) {
                    break;
                }
                nibble.append(binary[i + j]);
            }
            hex.append(Integer.toHexString(Integer.parseInt(nibble.toString(), 2)));
        }
        return hex.toString();
    }

    /**
     * Calculate perceptual hash for an RGBA image using quick method.
     *
     * Quick method uses rounded block sizes and is less accurate in case image
     * width and height are not divisible by the number of bits.
     *
     * @param data RGBA image data.
     * @param bits Number of blocks to divide the image by horizontally and vertically.
     * @return Block hash bit array where each element in array is either 0 or 1.
     */
    private static int[] blockHashQuick(RGBAImageDataInterface data, int bits) {
        int width = data.getWidth();
        int height = data.getHeight();
        int blockWidth = width / bits;
        int blockHeight = height / bits;

        int[] blocks = new int[bits * bits];
        for (int y = 0; y < bits; y++) {
            for (int x = 0; x < bits; x++) {
                int value = 0;

                for (int iy = 0; iy < blockHeight; iy++) {
                    for (int ix = 0; ix < blockWidth; ix++) {
                        int ii = ((y * blockHeight + iy) * width + (x * blockWidth + ix)) * 4;

                        int alpha = data.value(ii+3);
                        if (alpha == 0) {
                            value += 765;
                        } else {
                            value += data.value(ii) + data.value(ii+1) + data.value(ii+2);
                        }
                    }
                }

                blocks[y * bits + x] = value;
            }
        }

        translateBlocksToBits(blocks, blockWidth * blockHeight);
        return blocks;
    }

    /**
     * Calculate perceptual hash for an RGBA image using precise method.
     *
     * Precise method puts weighted pixel values to blocks according to pixel
     * area falling within a given block and provides more accurate results
     * in case width and height are not divisible by the number of bits.
     *
     * @param data RGBA image data.
     * @param bits number of blocks to divide the image by horizontally and vertically.
     * @return Block hash bit array where each element in array is either 0 or 1.
     */
    private static int[] blockHash(RGBAImageDataInterface data, int bits) {
        int width = data.getWidth();
        int height = data.getHeight();

        boolean evenX = width % bits == 0;
        boolean evenY = height % bits == 0;

        if (evenX && evenY) {
            return blockHashQuick(data, bits);
        }

        double blockWidth = (double) width / (double) bits;
        double blockHeight = (double) height / (double) bits;

        double[] blocks = new double[bits * bits];
        int[] result = new int[bits * bits];

        for (int y = 0; y < height; y++) {
            int blockTop, blockBottom;
            double weightTop, weightBottom;
            if (evenY) {
                // don't bother dividing y, if the size evenly divides by bits
                blockTop = blockBottom = (int) Math.floor((double)y / blockHeight);
                weightTop = 1.0;
                weightBottom = 0.0;
            } else {
                double yMod = (y + 1) % blockHeight;
                double yFrac = yMod - Math.floor(yMod);
                double yInt = yMod - yFrac;

                weightTop = 1 - yFrac;
                weightBottom = yFrac;

                // yInt will be 0 on bottom/right borders and on block boundaries
                if (yInt > 0 || (y + 1) == height) {
                    blockTop = blockBottom = (int) Math.floor((double)y / blockHeight);
                } else {
                    blockTop = (int) Math.floor((double) y / blockHeight);
                    blockBottom = (int) Math.ceil((double) y / blockHeight);
                }
            }

            for (int x = 0; x < width; x++) {
                int blockLeft, blockRight;
                double weightLeft, weightRight;
                int ii = (y * width + x) * 4;

                int alpha = data.value(ii + 3);
                double value = (alpha == 0) ? 765 : data.value(ii) + data.value(ii + 1) + data.value(ii + 2);

                if (evenX) {
                    blockLeft = blockRight = (int) Math.floor((double)x / blockWidth);
                    weightLeft = 1;
                    weightRight = 0;
                } else {
                    double xMod = (x + 1) % blockWidth;
                    double xFrac = xMod - Math.floor(xMod);
                    double xInt = xMod - xFrac;

                    weightLeft = 1 - xFrac;
                    weightRight = xFrac;

                    // xInt will be 0 on bottom/right borders and on block boundaries
                    if (xInt > 0 || (x + 1) == width) {
                        blockLeft = blockRight = (int) Math.floor((double)x / blockWidth);
                    } else {
                        blockLeft = (int) Math.floor((double)x / blockWidth);
                        blockRight = (int) Math.ceil((double)x / blockWidth);
                    }
                }

                // add weighted pixel value to relevant blocks
                blocks[blockTop * bits + blockLeft] += value * weightTop * weightLeft;
                blocks[blockTop * bits + blockRight] += value * weightTop * weightRight;
                blocks[blockBottom * bits + blockLeft] += value * weightBottom * weightLeft;
                blocks[blockBottom * bits + blockRight] += value * weightBottom * weightRight;
            }
        }
        translateBlocksToBitsDouble(blocks, result, (int) (blockWidth * blockHeight));
        return result;
    }

    /**
     * Computes block hash and returns hex value.
     *
     * Equivalent to calling blockHashHex(data, 8)
     *
     * @param data RGBA image data.
     * @param bits Block 1-d dimensions.
     * @return Block hash value hex string.
     */
    public static String blockHashHex(RGBAImageDataInterface data, int bits) {
        return bitsToHexHash(blockHash(data, bits));
    }

    /**
     * Computes block hash using quick algo and returns hex value.
     *
     * @param data RGBA image data.
     * @param bits Block 1-d dimensions.
     * @return Block hash value hex string.
     */
    public static String blockHashHexQuick(RGBAImageDataInterface data, int bits) {
        return bitsToHexHash(blockHashQuick(data, bits));
    }

    public static int hammingDistance(String hash1, String hash2) {
        int[] oneBits = new int[]{0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4};
        int d = 0;

        if (hash1.length() != hash2.length()) {
            throw new RuntimeException("Can't compare hashes with different length");
        }

        for (int i = 0; i < hash1.length(); i++) {
            int n1 = Integer.parseInt(hash1.substring(i, i+1), 16);
            int n2 = Integer.parseInt(hash2.substring(i, i+1), 16);
            d += oneBits[n1 ^ n2];
        }
        return d;
    }
}

