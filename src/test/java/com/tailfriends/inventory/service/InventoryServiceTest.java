package com.tailfriends.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.inventory.domain.InventoryItem;
import com.tailfriends.inventory.domain.InventoryUnit;
import com.tailfriends.inventory.dto.InventoryCreateRequest;
import com.tailfriends.inventory.dto.InventoryItemResponse;
import com.tailfriends.inventory.dto.InventoryUpdateRequest;
import com.tailfriends.inventory.repository.InventoryItemRepository;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogDetails;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class InventoryServiceTest {

    private static final Long USER_ID = 1L;

    private final InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final InventoryService service =
            new InventoryService(CurrentUserStub.of(USER_ID), inventoryItemRepository, userRepository);

    @Test
    void 이름의_앞뒤_공백을_빼고_등록한다() {
        given(inventoryItemRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 5L));

        assertThat(service.create(new InventoryCreateRequest(" 귀뚜라미 ", 100, InventoryUnit.COUNT, 30)).id())
                .isEqualTo(5L);

        ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryItemRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("귀뚜라미");
    }

    @Test
    void 같은_이름이_있으면_DUPLICATE_ITEM이고_동시에_먼저_들어가도_같다() {
        given(inventoryItemRepository.existsByUserIdAndName(USER_ID, "귀뚜라미")).willReturn(true);
        assertError(() -> service.create(new InventoryCreateRequest("귀뚜라미", 1, InventoryUnit.COUNT, null)),
                ErrorCode.DUPLICATE_ITEM);

        given(inventoryItemRepository.existsByUserIdAndName(USER_ID, "밀웜")).willReturn(false);
        given(inventoryItemRepository.save(any())).willThrow(new DataIntegrityViolationException("uk"));
        assertError(() -> service.create(new InventoryCreateRequest("밀웜", 1, InventoryUnit.COUNT, null)),
                ErrorCode.DUPLICATE_ITEM);
    }

    @Test
    void add는_더하고_quantity는_그_값으로_바꾼다() {
        InventoryItem item = item(20);
        given(inventoryItemRepository.findByIdAndUserId(5L, USER_ID)).willReturn(Optional.of(item));

        InventoryItemResponse added = service.update(5L, new InventoryUpdateRequest(100, null));
        assertThat(added.quantity()).isEqualTo(120);
        assertThat(added.low()).isFalse();

        InventoryItemResponse replaced = service.update(5L, new InventoryUpdateRequest(null, 10));
        assertThat(replaced.quantity()).isEqualTo(10);
        assertThat(replaced.low()).isTrue();
    }

    @Test
    void add와_quantity를_둘_다_보내거나_둘_다_없거나_최댓값을_넘으면_400() {
        given(inventoryItemRepository.findByIdAndUserId(5L, USER_ID)).willReturn(Optional.of(item(99_990)));

        assertError(() -> service.update(5L, new InventoryUpdateRequest(1, 1)), ErrorCode.INVALID_INPUT);
        assertError(() -> service.update(5L, new InventoryUpdateRequest(null, null)), ErrorCode.INVALID_INPUT);
        assertError(() -> service.update(5L, new InventoryUpdateRequest(10, null)), ErrorCode.INVALID_INPUT);
    }

    @Test
    void 남의_품목은_INVENTORY_ITEM_NOT_FOUND() {
        given(inventoryItemRepository.findByIdAndUserId(5L, USER_ID)).willReturn(Optional.empty());

        assertError(() -> service.delete(5L), ErrorCode.INVENTORY_ITEM_NOT_FOUND);
        verify(inventoryItemRepository, never()).delete(any());
    }

    @Test
    void 먹은_급여_기록이면_먹이_이름의_마리_품목에서_개수만큼_뺀다() {
        service.deductForFeed(feed(" 귀뚜라미 ", 6, false));

        verify(inventoryItemRepository).deduct(USER_ID, "귀뚜라미", InventoryUnit.COUNT, 6);
    }

    @Test
    void 안_먹음_개수_없음_먹이_이름_없음_급여가_아닌_기록은_빼지_않는다() {
        service.deductForFeed(feed("귀뚜라미", 6, true));
        service.deductForFeed(feed("귀뚜라미", null, false));
        service.deductForFeed(feed(" ", 6, false));
        service.deductForFeed(Log.create(animal(), user(), null, LogType.CLEAN, LocalDate.of(2026, 11, 1), null,
                new LogDetails(null, null, null, null, null, null, null, true, null, null, null, null)));

        verify(inventoryItemRepository, never()).deduct(anyLong(), anyString(), any(), anyInt());
    }

    private static Log feed(String food, Integer count, boolean refused) {
        return Log.create(animal(), user(), null, LogType.FEED, LocalDate.of(2026, 11, 1), null,
                new LogDetails(food, count, refused, null, null, null, null, null, null, null, null, null));
    }

    private static InventoryItem item(int quantity) {
        return withId(InventoryItem.builder().user(user()).name("귀뚜라미").quantity(quantity)
                .unit(InventoryUnit.COUNT).alertBelow(30).build(), 5L);
    }

    private static Animal animal() {
        return withId(Animal.builder().user(user()).speciesName("레오파드 게코").name("망고").feedIntervalDays(4).build(),
                12L);
    }

    private static User user() {
        return withId(User.builder().provider(Provider.KAKAO).providerId("kakao-1").nickname("사용자").build(), USER_ID);
    }

    private static void assertError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
