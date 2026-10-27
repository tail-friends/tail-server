package com.tailfriends.photo.dto;

import com.tailfriends.photo.ai.CharacterAnalysis;

/**
 * 노션 명세 형식. 저장하지 않으며, 앱이 확인·수정한 뒤 캐릭터 저장 API로 저장한다.
 */
public record CharacterAnalysisResponse(
        String shape,
        String patternType,
        CharacterAnalysis.Colors colors,
        CharacterAnalysis.Extra extra,
        String guess
) {

    public static CharacterAnalysisResponse from(CharacterAnalysis analysis) {
        return new CharacterAnalysisResponse(
                analysis.shape(), analysis.patternType(), analysis.colors(), analysis.extra(), analysis.guess());
    }
}
