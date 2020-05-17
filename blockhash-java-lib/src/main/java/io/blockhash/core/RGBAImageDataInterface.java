package io.blockhash.core;

/**
 * This represents a pixel data interface which blockhash algorithm expects.
 * The interface is equivalent to one returned by getImageData on browser canvas.
 *
 * Group of four indices represent one pixel. e.g
 * Calling value(0 through 3) would return R,G,B and A values of pixel (0,0).
 * Calling value(4 through 8) would return R,G,B and A values of pixel (0,1).
 * .. and so on in a row first manner.
 *
 */
public interface RGBAImageDataInterface {
    public int value(int index);
    public int getWidth();
    public int getHeight();
}
