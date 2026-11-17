package com.tailfriends.photo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.repository.AnimalThumbnail;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CharacterThumbnailIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Test
    void 개체마다_가장_최근_캐릭터용_사진_한_장만_준다() {
        User user = userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
        Animal mango = saveAnimal(user, "망고");
        Animal choco = saveAnimal(user, "초코");
        Animal kiwi = saveAnimal(user, "키위");
        savePhoto(user, mango, PhotoPurpose.CHARACTER, "mango/old.jpg");
        savePhoto(user, mango, PhotoPurpose.CHARACTER, "mango/new.jpg");
        savePhoto(user, mango, PhotoPurpose.ALBUM, "mango/album.jpg");
        savePhoto(user, choco, PhotoPurpose.CHARACTER, "choco/only.jpg");
        savePhoto(user, kiwi, PhotoPurpose.HEALTH, "kiwi/health.jpg");

        List<AnimalThumbnail> thumbnails = photoRepository.findCharacterThumbnails(
                List.of(mango.getId(), choco.getId(), kiwi.getId()));

        assertThat(thumbnails).containsExactlyInAnyOrder(
                new AnimalThumbnail(mango.getId(), "mango/new.jpg"),
                new AnimalThumbnail(choco.getId(), "choco/only.jpg"));
    }

    private Animal saveAnimal(User user, String name) {
        return animalRepository.save(Animal.builder()
                .user(user)
                .speciesName("레오파드 게코")
                .name(name)
                .feedIntervalDays(4)
                .build());
    }

    private void savePhoto(User user, Animal animal, PhotoPurpose purpose, String thumbnailKey) {
        photoRepository.save(Photo.builder()
                .user(user)
                .animal(animal)
                .purpose(purpose)
                .thumbnailKey(thumbnailKey)
                .build());
    }
}
