package me.hatena.project.safeblock.database;

import com.flowpowered.math.vector.Vector3i;
import me.hatena.project.safeblock.Safeblock;
import me.hatena.project.safeblock.database.entities.BLOCKLOG;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class H2EmbeddedDB {
    private static final String DRIVER = "org.h2.Driver";
    private static final String CONNECTION = "jdbc:h2:";
    private static final String USER = "";
    private static final String PASSWORD = "";
    private final Logger logger;
    private Connection connection;

    public H2EmbeddedDB() {
        this.logger = Safeblock.getInstance().getLogger();
    }

    /**
     * 좌표에 해당하는 위치에서 발생한 로그를 출력
     *
     * @param v 좌표
     * @return {@link BLOCKLOG} 리스트
     */
    public List<BLOCKLOG> selectLog(String world_uuid, Vector3i v) throws SQLException {
        String qry = "SELECT * FROM BLOCKLOG WHERE WORLD_UUID = ?, X = ? AND Y = ? AND Z = ?";

        List<BLOCKLOG> logs = new ArrayList<>();
        PreparedStatement pstat = null;
        ResultSet rs = null;
        try {
            pstat = connection.prepareStatement(qry);
            pstat.setString(1, world_uuid);
            pstat.setInt(2, v.getX());
            pstat.setInt(3, v.getY());
            pstat.setInt(4, v.getZ());

            rs = pstat.executeQuery();
            while (rs.next()) {
                logs.add(new BLOCKLOG(rs));
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null)
                rs.close();
            if (pstat != null)
                pstat.close();
        }

        return logs;
    }

    public void insertLog(List<BLOCKLOG> logs) throws SQLException {
        connection.setAutoCommit(false);
        for (BLOCKLOG log : logs) {
            insertLog(log.getUuid(), log.getWorld_uuid(), log.getPosition(), log.getType(), log.getDatetime());
        }
        connection.commit();
        connection.setAutoCommit(true);
    }

    public void insertLog(BLOCKLOG log) throws SQLException {
        insertLog(log.getUuid(), log.getWorld_uuid(), log.getPosition(), log.getType(), log.getDatetime());
    }

    /**
     * 블록 파괴, 배치 로그 insert
     *
     * @param uuid 행위자 플레이어의 uuid
     * @param v 좌표
     * @param evt 이벤트 타입
     * @param instant 이벤트 시각
     * @throws SQLException SQL Exception
     */
    public void insertLog(String uuid, String world_uuid, Vector3i v, BLOCKLOG.EventType evt, Instant instant) throws SQLException {
        String qry = "INSERT INTO BLOCKLOG (UUID, WORLD_UUID, X, Y, Z, EVENTTYPE, DATETIME) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pstat = null;
        try {

            pstat = connection.prepareStatement(qry);
            pstat.setString(1, uuid);
            pstat.setString(2, world_uuid);
            pstat.setInt(3, v.getX());
            pstat.setInt(4, v.getY());
            pstat.setInt(5, v.getZ());
            pstat.setInt(6, evt.getValue());
            pstat.setTimestamp(7, Timestamp.from(instant));

            int result = pstat.executeUpdate();
            if (result == 0)
                logger.error("Log insert failed");
        } catch (SQLException e) {
            throw e;
        } finally {
            if (pstat != null)
                pstat.close();
        }
    }

    /**
     * Create connection to database
     *
     * @param file Database file path
     */
    public void connect(Path file) {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            logger.error("H2FileDB#connect ClassNotFoundException");
            e.printStackTrace();
        }

        try {
            connection = DriverManager.getConnection(CONNECTION + file + ";TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0", USER, PASSWORD);
        } catch (SQLException e) {
            logger.error("H2FileDB#connect SQLException");
            e.printStackTrace();
        }
    }

    /**
     * Disconnection
     */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            Safeblock.getInstance().getLogger().error("H2FileDB#disconnect SQLException");
            e.printStackTrace();
        }
    }

}
