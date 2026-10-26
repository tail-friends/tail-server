package com.tailfriends.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageResizerTest {

    private final ImageResizer imageResizer = new ImageResizer();

    @Test
    void 긴_변을_정한_길이로_줄여_JPG로_만든다() throws IOException {
        byte[] png = image(400, 200, BufferedImage.TYPE_INT_RGB, "png");

        List<byte[]> jpegs = imageResizer.toJpegs(png, 128, 300);

        assertThat(formatOf(jpegs.get(0))).isEqualTo("JPEG");
        assertThat(sizeOf(jpegs.get(0))).containsExactly(128, 64);
        assertThat(sizeOf(jpegs.get(1))).containsExactly(300, 150);
    }

    @Test
    void 원래_더_작으면_키우지_않는다() throws IOException {
        byte[] jpeg = image(100, 50, BufferedImage.TYPE_INT_RGB, "jpg");

        List<byte[]> jpegs = imageResizer.toJpegs(jpeg, 128);

        assertThat(sizeOf(jpegs.get(0))).containsExactly(100, 50);
    }

    @Test
    void 휴대폰_사진의_회전_정보를_반영한다() throws IOException {
        // 회전 정보 6 = 시계 방향 90도. 가로 40x20으로 찍혔지만 세로 20x40으로 보여야 한다
        byte[] jpeg = withExifOrientation(image(40, 20, BufferedImage.TYPE_INT_RGB, "jpg"), 6);

        assertThat(sizeOf(imageResizer.toJpegs(jpeg, 128).get(0))).containsExactly(20, 40);
    }

    @Test
    void 큰_사진은_줄여_읽어도_정한_길이와_비율과_회전을_지킨다() throws IOException {
        // 1000x500을 128로 줄일 때는 4칸마다 읽어 250x125만 푼다. 회전하면 125x250 → 64x128
        byte[] jpeg = withExifOrientation(image(1000, 500, BufferedImage.TYPE_INT_RGB, "jpg"), 6);

        List<byte[]> jpegs = imageResizer.toJpegs(jpeg, 128, 300);

        assertThat(sizeOf(jpegs.get(0))).containsExactly(64, 128);
        assertThat(sizeOf(jpegs.get(1))).containsExactly(150, 300);
    }

    @Test
    void 줄여_읽는_간격은_읽은_긴_변이_만들_길이의_1배에서_2배_사이가_되게_정한다() {
        assertThat(ImageResizer.subsamplingStep(3200, 1600)).isEqualTo(1);
        assertThat(ImageResizer.subsamplingStep(3201, 1600)).isEqualTo(2);
        assertThat(ImageResizer.subsamplingStep(8000, 1600)).isEqualTo(3);
        assertThat(ImageResizer.subsamplingStep(8000, 128)).isEqualTo(32);
        assertThat(ImageResizer.subsamplingStep(100, 128)).isEqualTo(1);
    }

    @Test
    void 투명한_배경은_흰색으로_채운다() throws IOException {
        byte[] png = image(10, 10, BufferedImage.TYPE_INT_ARGB, "png");

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(imageResizer.toJpegs(png, 128).get(0)));

        Color pixel = new Color(result.getRGB(5, 5));
        assertThat(pixel.getRed()).isGreaterThan(245);
        assertThat(pixel.getGreen()).isGreaterThan(245);
        assertThat(pixel.getBlue()).isGreaterThan(245);
    }

    @Test
    void 사진이_아니면_INVALID_FILE() {
        byte[] text = "not an image".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> imageResizer.toJpegs(text, 128))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);
    }

    @Test
    void JPG_PNG가_아닌_형식은_INVALID_FILE() throws IOException {
        byte[] gif = image(10, 10, BufferedImage.TYPE_INT_RGB, "gif");

        assertThatThrownBy(() -> imageResizer.toJpegs(gif, 128))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);
    }

    @Test
    void 픽셀_수가_너무_크면_풀지_않고_INVALID_FILE() {
        byte[] huge = pngHeader(10_000, 10_000);

        assertThatThrownBy(() -> imageResizer.toJpegs(huge, 128))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);
    }

    private static byte[] image(int width, int height, int type, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, type);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }

    private static String formatOf(byte[] image) throws IOException {
        return ImageIO.getImageReaders(ImageIO.createImageInputStream(new ByteArrayInputStream(image)))
                .next().getFormatName().toUpperCase();
    }

    private static int[] sizeOf(byte[] image) throws IOException {
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(image));
        return new int[] {decoded.getWidth(), decoded.getHeight()};
    }

    /**
     * JFIF(APP0) 뒤에 회전 정보(Orientation)만 담은 EXIF(APP1)를 끼워 넣는다.
     */
    private static byte[] withExifOrientation(byte[] jpeg, int orientation) {
        ByteBuffer tiff = ByteBuffer.allocate(26)
                .put(new byte[] {'M', 'M', 0, 42}).putInt(8)
                .putShort((short) 1)
                .putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientation).putShort((short) 0)
                .putInt(0);
        byte[] exif = ByteBuffer.allocate(6 + tiff.capacity()).put("Exif\0\0".getBytes(StandardCharsets.US_ASCII))
                .put(tiff.array()).array();
        int app0End = 4 + (((jpeg[4] & 0xFF) << 8) | (jpeg[5] & 0xFF));
        return ByteBuffer.allocate(jpeg.length + 4 + exif.length)
                .put(jpeg, 0, app0End)
                .put((byte) 0xFF).put((byte) 0xE1).putShort((short) (2 + exif.length)).put(exif)
                .put(jpeg, app0End, jpeg.length - app0End)
                .array();
    }

    /**
     * 크기 정보(IHDR)만 있는 PNG. 실제 픽셀을 만들지 않고 큰 사진을 흉내 낸다.
     */
    private static byte[] pngHeader(int width, int height) {
        ByteBuffer ihdr = ByteBuffer.allocate(17);
        ihdr.put("IHDR".getBytes(StandardCharsets.US_ASCII))
                .putInt(width).putInt(height)
                .put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0);
        CRC32 crc = new CRC32();
        crc.update(ihdr.array());
        return ByteBuffer.allocate(8 + 4 + 17 + 4)
                .put(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})
                .putInt(13)
                .put(ihdr.array())
                .putInt((int) crc.getValue())
                .array();
    }
}
