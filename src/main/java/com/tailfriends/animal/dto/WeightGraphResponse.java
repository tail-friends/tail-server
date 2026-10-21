package com.tailfriends.animal.dto;

import com.tailfriends.log.repository.WeightPoint;
import java.util.List;

/**
 * 그래프용 체중 기록, 날짜 오름차순. 같은 날 기록이 여러 개면 모두 담는다.
 */
public record WeightGraphResponse(
        List<WeightPoint> points
) {
}
