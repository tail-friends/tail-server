package com.tailfriends.photo.service;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

/**
 * 사진을 JPG로 줄인다. 휴대폰 사진의 회전 정보(EXIF)를 반영하고, 투명 배경은 흰색으로 채운다.
 */
@Component
public class ImageResizer {

    private static final Set<String> FORMATS = Set.of("jpeg", "png");

    /**
     * 픽셀 수가 너무 큰 사진은 풀 때 메모리를 많이 써서 받지 않는다. (48MP 휴대폰 사진까지)
     */
    static final long MAX_PIXELS = 50_000_000L;

    /**
     * 사진을 한 번 풀어 maxSides마다 긴 변을 그 길이 이하로 줄인 JPG를 만든다. 원래 더 작으면 키우지 않는다.
     */
    public List<byte[]> toJpegs(byte[] image, int... maxSides) {
        checkFormatAndSize(image);
        BufferedImage decoded = decode(image);
        return Arrays.stream(maxSides)
                .mapToObj(maxSide -> toJpeg(decoded, maxSide))
                .toList();
    }

    private void checkFormatAndSize(byte[] image) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(image))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidFile();
            }
            ImageReader reader = readers.next();
            try {
                if (!FORMATS.contains(reader.getFormatName().toLowerCase(Locale.ROOT))) {
                    throw invalidFile();
                }
                reader.setInput(input);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
                    throw invalidFile();
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw invalidFile();
        }
    }

    private BufferedImage decode(byte[] image) {
        try {
            return Thumbnails.of(new ByteArrayInputStream(image)).scale(1.0).asBufferedImage();
        } catch (IOException e) {
            throw invalidFile();
        }
    }

    private byte[] toJpeg(BufferedImage image, int maxSide) {
        try {
            BufferedImage resized = Math.max(image.getWidth(), image.getHeight()) <= maxSide
                    ? image
                    : Thumbnails.of(image).size(maxSide, maxSide).asBufferedImage();
            BufferedImage rgb = new BufferedImage(resized.getWidth(), resized.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgb.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(resized, 0, 0, null);
            graphics.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(rgb, "jpg", output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static BusinessException invalidFile() {
        return new BusinessException(ErrorCode.INVALID_FILE);
    }
}
