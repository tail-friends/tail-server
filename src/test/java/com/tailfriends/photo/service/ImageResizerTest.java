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
