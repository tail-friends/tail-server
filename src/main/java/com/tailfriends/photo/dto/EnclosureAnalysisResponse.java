package com.tailfriends.photo.dto;

import com.tailfriends.photo.ai.EnclosureAnalysis;
import java.util.List;

/**
 * 노션 명세 형식. 저장하지 않으며, 앱이 확인·수정한 뒤 사진 테마 저장 API로 저장한다.
 */
public record EnclosureAnalysisResponse(
        String floorColor,
        String wallColor,
        List<ObjectResponse> objects
) {

    public record ObjectResponse(String type, double x, double y) {
    }

    public static EnclosureAnalysisResponse from(EnclosureAnalysis analysis) {
        return new EnclosureAnalysisResponse(
                analysis.floorColor(),
                analysis.wallColor(),
                analysis.objects().stream()
                        .map(object -> new ObjectResponse(
                                object.type().name(), object.x().doubleValue(), object.y().doubleValue()))
                        .toList());
    }
}
