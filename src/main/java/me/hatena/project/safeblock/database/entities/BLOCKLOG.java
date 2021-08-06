package me.hatena.project.safeblock.database.entities;

import com.flowpowered.math.vector.Vector3i;
import lombok.Getter;
import lombok.Setter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;

public class BLOCKLOG {
    @Getter
    @Setter
    private int idx;
    @Getter
    @Setter
    private String uuid;
    @Getter
    @Setter
    private String world_uuid;
    @Getter
    @Setter
    private int x;
    @Getter
    @Setter
    private int y;
    @Getter
    @Setter
    private int z;
    @Getter
    @Setter
    private EventType type;
    @Getter
    @Setter
    private Instant datetime;

    public BLOCKLOG() {}
    public BLOCKLOG(ResultSet rs) throws SQLException {
        idx = rs.getInt(1);
        uuid = rs.getString(2);
        world_uuid = rs.getString(3);
        x = rs.getInt(4);
        y = rs.getInt(5);
        z = rs.getInt(6);
        type = EventType.valueOf(rs.getInt(7)).orElse(EventType.UNKNOWN);
        datetime = rs.getTimestamp(8).toInstant();
    }

    public void setPosition(Vector3i pos) {
        x = pos.getX();
        y = pos.getY();
        z = pos.getZ();
    }

    public Vector3i getPosition() {
        return Vector3i.from(x, y, z);
    }

    public enum EventType {
        UNKNOWN(-1),
        BREAK(0),
        PLACE(1);

        @Getter
        private final int value;

        EventType(int value) {
            this.value = value;
        }

        public static Optional<EventType> valueOf(int value) {
            return Arrays.stream(values()).filter(v -> v.value == value).findFirst();
        }

        @Override
        public String toString() {
            return name();
        }
    }
}
