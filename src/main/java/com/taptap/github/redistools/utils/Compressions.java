package com.taptap.github.redistools.utils;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;


/**
 * @author : kl (http://kailing.pub)
 * @since : 2023-10-27 22:41
 */
public class Compressions {

    static final GzipParameters parameters = new GzipParameters();
    private final static Logger logger = LoggerFactory.getLogger(Compressions.class);

    public static byte[] gzipCompress(byte[] data) {
        // parameters.setCompressionLevel(5);
        //parameters.setBufferSize(1024);
        return gzipCompress(data, parameters);
    }

    public static byte[] gzipCompress(byte[] data, GzipParameters parameters) {

        try (ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
             GzipCompressorOutputStream gzipOutput = new GzipCompressorOutputStream(byteArrayOutput, parameters)) {
            gzipOutput.write(data);
            gzipOutput.close();
            return byteArrayOutput.toByteArray();
        } catch (Exception e) {
            logger.error("compressError:{} ,  data:{}", e.getMessage(), data);
        }
        return null;
    }

    public static byte[] gzipUnCompress(byte[] data) {
        try (GzipCompressorInputStream gzipInput = new GzipCompressorInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[2048];
            int n;
            while (-1 != (n = gzipInput.read(buffer))) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (Exception e) {
            logger.error("unCompressError:{} ,  data:{}", e.getMessage(), data);
        }
        return null;
    }

    public static String snappyUnCompress(byte[] data) {
        try (FramedSnappyCompressorInputStream zIn = new FramedSnappyCompressorInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[2048];
            int n;
            while (-1 != (n = zIn.read(buffer))) {
                out.write(buffer, 0, n);
            }
            return out.toString();
        } catch (Exception e) {
            logger.error("snappyUnCompressError:{} ,  data:{}", e.getMessage(), data);
        }
        return null;
    }

    public static byte[] snappyCompress(String data) {
        try (ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
             FramedSnappyCompressorOutputStream snOut = new FramedSnappyCompressorOutputStream(byteArrayOutput)) {
            snOut.write(data.getBytes());
            snOut.close();
            return byteArrayOutput.toByteArray();
        } catch (Exception e) {
            logger.error("snappyCompressError:{} ,  data:{}", e.getMessage(), data);
        }
        return null;
    }

    public static byte[] zstdUnCompress(byte[] data) {
        try (ZstdCompressorInputStream inputStream = new ZstdCompressorInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[2048];
            int n;
            while (-1 != (n = inputStream.read(buffer))) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (Exception e) {
            logger.error("zstdUnCompressError:{} ,  data:{}", e.getMessage(), data);
        }
        return null;
    }

    public static byte[] zstdCompress(byte[] data) {
        try (ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
             ZstdCompressorOutputStream snOut = new ZstdCompressorOutputStream(byteArrayOutput)) {
            snOut.write(data);
            snOut.close();
            return byteArrayOutput.toByteArray();
        } catch (Exception e) {
            logger.error("zstdCompressError:{} ,  data:{}", e.getMessage(), data);
        }
        return null;
    }

    /**
     * @deprecated use {@link #zstdCompress(byte[])} instead
     */
    @Deprecated
    public static byte[] zstdCompressNew(byte[] data) {
        try (ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream(data.length);
             ZstdCompressorOutputStream snOut = new ZstdCompressorOutputStream(byteArrayOutput)) {
            snOut.write(data);
            snOut.close();
            return byteArrayOutput.toByteArray();
        } catch (Exception e) {
            logger.error("zstdCompressError:{} ,  data:{}", e.getMessage(), data);
        }
        return null;
    }

    static final int GZIP_MAGIC = 0x1f8b0800;
    public static boolean isGzipData(byte[] data) {
        if (data.length < 4) return false;
        return ByteBuffer.wrap(data, 0, 4).getInt() == GZIP_MAGIC;
    }

    static final int ZSTD_MAGIC = 0x28b52ffd;
    public static boolean isZstdData(byte[] data) {
        if(data.length < 4) return false;
        return ByteBuffer.wrap(data, 0, 4).getInt() == ZSTD_MAGIC;
    }

    static final int SNAPPY_MAGIC = -16384000;
    public static boolean isSnappyData(byte[] data) {
        if(data.length < 4) return false;

        return ByteBuffer.wrap(data, 0, 4).getInt() == SNAPPY_MAGIC;
    }

}
