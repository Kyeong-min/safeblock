package me.hatena.project.safeblock;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserManager {

    private static UserManager instance;
    public static UserManager getInstance() {
        if (instance == null)
            instance = new UserManager();

        return instance;
    }

    /**
     * 조사를 실행중인 유저 목록
     */
    private List<String> users;

    private UserManager() {
        users = new ArrayList<>();
    }

    /**
     * 플레이어가 조사중인지 반환합니다.
     *
     * @param uuid 플레이어의 uuid
     * @return 조사중이라면 true, 그렇지 않다면 false
     */
    public boolean isChecking(String uuid) {
        return users.stream().anyMatch(x -> x.equals(uuid));
    }

    /**
     * 플레이어가 조사를 시작합니다.
     *
     * @param uuid 플레이어의 uuid
     */
    public void checkStart(String uuid) {
        if (isChecking(uuid))
            return;

        users.add(uuid);
    }

    /**
     * 플레이어가 조사를 중지합니다.
     * @param uuid 플레이어의 uuid
     */
    public void checkStop(String uuid) {
        users.removeIf(x -> x.equals(uuid));
    }

    /**
     * 플레이어의 이름을 가져옵니다.
     * 
     * @param uuid 플레이어의 uuid
     * @return 플레이어의 이름
     */
    public Optional<String> getName(String uuid) {
        UUID uid = UUID.fromString(uuid);

        Optional<Player> player = Sponge.getServer().getPlayer(uid);
        if (player.isPresent())
            return Optional.ofNullable(player.get().getName());

        // 온라인 플레이어 데이터에서 찾을 수 없었으므로 오프라인 데이터에서 찾는다.
        Optional<UserStorageService> userStorage = Sponge.getServiceManager().provide(UserStorageService.class);
        if (!userStorage.isPresent())
            return Optional.empty();

        return userStorage.get().get(uid).map(User::getName);
    }
}
