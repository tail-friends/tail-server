package com.tailfriends.animal.dto;

import java.time.LocalDate;

/**
 * startDate가 null이면 쿨링을 끈다. endDate가 null이면 끝나는 날 없이 계속이다.
 * 종료일이 시작일보다 빠르거나 startDate 없이 endDate만 보내면 서비스가 INVALID_INPUT을 던진다.
 */
public record AnimalCoolingRequest(LocalDate startDate, LocalDate endDate) {
}
