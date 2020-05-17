package io.blockhash.core;

import io.blockhash.server.BlockHashJava;
import org.junit.Test;

import org.junit.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class BlockhashCoreTest {

    private static String[] getExtension(String fileName) {
        String[] parts = fileName.split("\\.");
        String name = String.join(".", Arrays.copyOfRange(parts,0, parts.length - 1));
        return  new String[]{name, parts[parts.length - 1]};
    }

    private static String expectedHash(Path expectedHashFile) {
        File file = expectedHashFile.toFile();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            String fileData = new String(data, StandardCharsets.UTF_8);
            return fileData.trim().split(" ")[0];
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test() {
        int bits = 16;
        ClassLoader classLoader = getClass().getClassLoader();
        File testData = new File(classLoader.getResource("test_data").getFile());
        File[] files = testData.listFiles();
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String[] f = getExtension(file.getName());
            if (f[1].compareToIgnoreCase("jpg") == 0 ||
                    f[1].compareToIgnoreCase("jpeg") == 0 ||
                    f[1].compareToIgnoreCase("png") == 0) {
                for (int m = 1; m <= 2; m++) {
                    String expectedHashFilename = String.format("%s_%d_%d.txt", f[0], bits, m);
                    String expectedHash = expectedHash(Paths.get(testData.getAbsolutePath(), expectedHashFilename));
                    RGBAImageDataInterface imageData = BlockHashJava.fromImagePath(file.getAbsolutePath());
                    String hash;

                    if (m == 1) {
                        hash = BlockhashCore.blockHashHexQuick(imageData, bits);
                    } else {
                        hash = BlockhashCore.blockHashHex(imageData, 16);
                    }
                    int hammingDistance = BlockhashCore.hammingDistance(hash, expectedHash);
                    Assert.assertTrue(String.format("Hamming distance for image %s exceeds 2", file.getName()),
                            hammingDistance <= 2);
                }
            }
        }
    }
}
