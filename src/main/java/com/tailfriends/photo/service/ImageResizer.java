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
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.ImageFilter;
import net.coobird.thumbnailator.util.exif.ExifFilterUtils;
import net.coobird.thumbnailator.util.exif.ExifUtils;
import net.coobird.thumbnailator.util.exif.Orientation;
import org.springframework.stereotype.Component;

/**
 * 사진을 JPG로 줄인다. 휴대폰 사진의 회전 정보(EXIF)를 반영하고, 투명 배경은 흰색으로 채운다.
 */
@Component
public class ImageResizer {

    private static final String JPEG = "jpeg";
    private static final Set<String> FORMATS = Set.of(JPEG, "png");

    /**
     * 픽셀 수가 너무 큰 사진은 풀 때 메모리를 많이 써서 받지 않는다. (48MP 휴대폰 사진까지)
     */
    static final long MAX_PIXELS = 50_000_000L;

    /**
     * 사진을 한 번 풀어 maxSides마다 긴 변을 그 길이 이하로 줄인 JPG를 만든다. 원래 더 작으면 키우지 않는다.
     */
    public List<byte[]> toJpegs(byte[] image, int... maxSides) {
        BufferedImage decoded = decode(image, Arrays.stream(maxSides).max().orElseThrow());
        return Arrays.stream(maxSides)
                .mapToObj(maxSide -> toJpeg(decoded, maxSide))
                .toList();
    }

    /**
     * 형식과 픽셀 수를 확인하고 푼다. 큰 사진은 줄여 읽어(subsampling) 다 풀지 않으므로,
     * 메모리는 원래 크기가 아니라 만들 크기(largest)에 비례한다. 휴대폰 사진의 회전 정보(EXIF)를 반영한다.
     */
    private BufferedImage decode(byte[] image, int largest) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(image))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidFile();
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!FORMATS.contains(format)) {
                    throw invalidFile();
                }
                reader.setInput(input);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if ((long) width * height > MAX_PIXELS) {
                    throw invalidFile();
                }
                ImageReadParam param = reader.getDefaultReadParam();
                int step = subsamplingStep(Math.max(width, height), largest);
                param.setSourceSubsampling(step, step, 0, 0);
                BufferedImage decoded = reader.read(0, param);
                // 회전 정보(EXIF)는 JPG에만 있다. PNG 메타데이터는 ExifUtils가 읽지 못한다
                return JPEG.equals(format) ? applyOrientation(decoded, ExifUtils.getExifOrientation(reader, 0)) : decoded;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw invalidFile();
        }
    }

    /**
     * 줄여 읽은 긴 변이 largest 이상 2배 이하가 되는 간격. 2배까지 여유를 둬서 이어서 줄일 때 화질을 지킨다.
     * 긴 변이 largest의 2배 이하면 줄여 읽지 않는다(1).
     */
    static int subsamplingStep(int longSide, int largest) {
        return Math.max(1, (int) Math.ceil(longSide / (2.0 * largest)));
    }

    private static BufferedImage applyOrientation(BufferedImage image, Orientation orientation) {
        if (orientation == null) {
            return image;
        }
        ImageFilter filter = ExifFilterUtils.getFilterForOrientation(orientation);
        return filter == null ? image : filter.apply(image);
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
