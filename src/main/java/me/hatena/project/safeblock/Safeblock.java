package me.hatena.project.safeblock;

import com.flowpowered.math.vector.Vector3i;
import com.google.inject.Inject;
import lombok.Getter;
import lombok.var;
import me.hatena.project.safeblock.commands.BaseCommand;
import me.hatena.project.safeblock.commands.CheckCommand;
import me.hatena.project.safeblock.commands.CheckLocationCommand;
import me.hatena.project.safeblock.database.H2EmbeddedDB;
import me.hatena.project.safeblock.database.entities.BLOCKLOG;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Plugin(
        id = "safeblock",
        name = "Safeblock",
        description = "This plugin provides simple block protection.",
        authors = {
                "HATENA"
        }
)
public class Safeblock {

    private static Safeblock instance;
    @Inject
    @Getter
    private Logger logger;
    @Inject
    private PluginContainer plugin;
    @Inject
    private Game game;
    @Getter
    private H2EmbeddedDB db;

    public Safeblock() {
        instance = this;
    }

    public static Safeblock getInstance() {
        return instance;
    }

    /**
     * Register commands
     */
    private void registerCommands() {
        logger.debug("Safeblock#registerCommands");
        CommandSpec spec = CommandSpec.builder()
                .description(Text.of("Safeblock command"))
                .permission(BaseCommand.PERMISSION_BASE)
                .child(new CheckCommand().buildSelf(), CheckCommand.ALIAS)
                .child(new CheckLocationCommand().buildSelf(), CheckLocationCommand.ALIAS)
                .build();

        Sponge.getCommandManager().register(this, spec, "safeblock", "sb");
    }

    private void initDbAsset() {
        String dbFile = "safeblock.h2.mv.db";
        Path path = Paths.get(game.getGameDirectory().toString(), "safeblock");
        Path filepath = Paths.get(path.toString(), dbFile);
        if (Files.notExists(filepath)) {
            Optional<Asset> asset = plugin.getAsset(dbFile);
            if (!asset.isPresent()) {
                logger.error("DB Asset not founded!!!");
            }

            try {
                asset.get().copyToFile(filepath);
            } catch (IOException e) {
                logger.error("Failed copy db asset to disk: " + e);
                e.printStackTrace();
            }
        }
    }

    /**
     * 트랜젝션에서 위치정보를 가져와 해당 위치의 블록 배치, 파괴 로그를 가져옵니다.
     *
     * @param trs 트랜젝션 리스트
     * @param player 플레이어
     */
    private void check(List<Transaction<BlockSnapshot>> trs, Player player) {
        var optTr = trs.stream().findFirst();
        if (!optTr.isPresent()) {
            player.sendMessage(Text.of(TextSerializers.FORMATTING_CODE.deserialize("[&cERROR&r] 트랜젝션 정보가 없습니다.")));
            return;
        }

        Transaction<BlockSnapshot> tr = optTr.get();
        BlockSnapshot snapshot = tr.getFinal();
        Optional<Location<World>> optLoc = snapshot.getLocation();
        if (!optLoc.isPresent()) {
            player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize("[&cERROR&r] 로케이션 정보가 없습니다."));
            return;
        }

        Location<World> loc = optLoc.get();
        UUID worlduuid = loc.getExtent().getUniqueId();
        Vector3i pos = loc.getBlockPosition();
        checkAndSendMessage(worlduuid.toString(), pos, player);

        return;
    }

    public void checkAndSendMessage(String world_uuid, Vector3i pos, Player player) {
        try {
            List<BLOCKLOG> logList = db.selectLog(world_uuid, pos);
            int count = 0;
            UserManager um = UserManager.getInstance();
            player.sendMessage(Text.of("---------------------"));
            for (BLOCKLOG log : logList) {
                // #1 [2021-05-21 18:49:00] (username) -> 파괴
                StringBuilder sb = new StringBuilder();
                sb.append("&c#").append(++count).
                        append("&r [&a").append(getDateTimeString(log.getDatetime())).append("&r] ")
                        .append("(&6").append(um.getName(log.getUuid()).orElse("Unknown")).append("&r) -> ")
                        .append(eventTypeToString(log.getType()));

                player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(sb.toString()));
            }
            player.sendMessage(Text.of("---------------------"));
        } catch (SQLException e) {
            player.sendMessage(Text.of("[&cERROR&r] 로그 조회 과정에서 예외가 발생했습니다."));
            logger.error("select log error: " + e);
            e.printStackTrace();
        }
    }

    /**
     * 블록 배치, 파괴 이벤트를 로깅
     *
     * @param trs 트랜젝션
     * @param uuid 행위자의 uuid
     * @param type 이벤트 타입
     */
    private void logging(List<Transaction<BlockSnapshot>> trs, String uuid, BLOCKLOG.EventType type) {
        List<BLOCKLOG> list = new ArrayList<>();
        for (Transaction<BlockSnapshot> tr : trs) {
            BLOCKLOG log = new BLOCKLOG();
            log.setUuid(uuid);
            // BlockSnapshot snapshot = tr.getFinal();
            BlockSnapshot snapshot = tr.getOriginal();

            Optional<Location<World>> optLoc = snapshot.getLocation();
            if (optLoc.isPresent()) {
                Location<World> loc = optLoc.get();
                UUID worldUuid = loc.getExtent().getUniqueId();
                log.setWorld_uuid(worldUuid.toString());
                Vector3i pos = loc.getBlockPosition();
                log.setPosition(pos);
            }
            log.setType(type);
            log.setDatetime(Instant.now());

            list.add(log);
        }

        try {
            int size = list.size();
            if (size == 1) {
                db.insertLog(list.get(0));
            } else if(size > 1) {
                db.insertLog(list);
            }
        } catch (SQLException e) {
            logger.error("Block log insert error: " + e);
            e.printStackTrace();
        }
    }

    /**
     * GameInitializationEvent handler
     *
     * @param event {@link GameInitializationEvent}
     */
    @Listener
    public void onInitialization(GameInitializationEvent event) {
        logger.debug("Safeblock#onInitialization");
        initDbAsset();

        db = new H2EmbeddedDB();
        db.connect(Paths.get(game.getGameDirectory().toString(), "safeblock", "safeblock.h2"));

        registerCommands();
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        logger.info("Safeblock is run");
    }

    @Listener
    public void onServerStopped(GameStoppedServerEvent event) {
        if (db != null)
            db.disconnect();
    }

    @Listener
    public void onBlockBreak(ChangeBlockEvent.Break event, @Root Player player) {
        logger.debug("SafeBlock#onBlockBreak");
        String uuid = player.getUniqueId().toString();

        List<Transaction<BlockSnapshot>> trs = event.getTransactions();

        if (UserManager.getInstance().isChecking(uuid)) {
            event.setCancelled(true);
            check(trs, player);
            return;
        }

        logging(trs, uuid, BLOCKLOG.EventType.BREAK);
    }

    @Listener
    public void onBlockPlace(ChangeBlockEvent.Place event, @Root Player player) {
        logger.debug("SafeBlock#onBlockPlace");
        String uuid = player.getUniqueId().toString();

        List<Transaction<BlockSnapshot>> trs = event.getTransactions();

        if (UserManager.getInstance().isChecking(uuid)) {
            event.setCancelled(true);
            check(trs, player);
            return;
        }

        logging(trs, uuid, BLOCKLOG.EventType.PLACE);
    }

    /**
     * Get datetime format string
     *
     * @param instant instant
     * @return datetime format string
     */
    public String getDateTimeString(Instant instant) {
        if (instant == null)
            return null;

        String format = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter dateTimePattern;
        try {
            dateTimePattern = DateTimeFormatter.ofPattern(format);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid DateTime format: " + format);
            dateTimePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        }

        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(dateTimePattern);
    }

    private String eventTypeToString(BLOCKLOG.EventType evt) {
        switch (evt) {
            case BREAK:
                return "&c파괴&r";
            case PLACE:
                return "&a배치&r";
            default:
                return "&0???&r";
        }
    }


}
