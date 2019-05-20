package org.tvheadend.tvhclient.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.tvheadend.tvhclient.domain.entity.EpgProgram
import org.tvheadend.tvhclient.domain.entity.Program
import org.tvheadend.tvhclient.domain.entity.SearchResultProgram

@Dao
interface ProgramDao {

    @get:Query("SELECT COUNT (*) FROM programs AS p " +
            "WHERE " + CONNECTION_IS_ACTIVE)
    val itemCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM programs AS p " +
            "WHERE " + CONNECTION_IS_ACTIVE)
    val itemCountSync: Int

    @Transaction
    @Query(BASE_PROGRAM_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND ((p.start >= :time) " +
            "  OR (p.start <= :time AND p.stop >= :time)) " +
            "GROUP BY p.id " +
            "ORDER BY p.start, p.channel_name ASC")
    fun loadProgramsFromTime(time: Long): LiveData<List<SearchResultProgram>>

    @Transaction
    @Query(PROGRAM_BASE_QUERY +
            " WHERE " + CONNECTION_IS_ACTIVE +
            " AND p.channel_id = :channelId " +
            " AND ((p.start >= :time) " +
            "  OR (p.start <= :time AND p.stop >= :time)) " +
            "GROUP BY p.id " +
            "ORDER BY p.start ASC")
    fun loadProgramsFromChannelFromTime(channelId: Int, time: Long): LiveData<List<Program>>

    @Transaction
    @Query(EPG_PROGRAM_BASE_QUERY +
            " WHERE " + CONNECTION_IS_ACTIVE +
            " AND channel_id = :channelId " +
            // Program is within time slot
            " AND ((start >= :startTime AND stop <= :endTime) " +
            // Program is at the beginning of time slot
            "  OR (start <= :startTime AND stop > :startTime) " +
            // Program is at the end of the time slot
            "  OR (start < :endTime AND stop >= :endTime)) " +
            "GROUP BY p.id " +
            "ORDER BY start ASC")
    fun loadProgramsFromChannelBetweenTimeSync(channelId: Int, startTime: Long, endTime: Long): List<EpgProgram>

    @Transaction
    @Query(EPG_PROGRAM_BASE_QUERY +
            " WHERE " + CONNECTION_IS_ACTIVE +
            " AND channel_id = :channelId " +
            // Program is within time slot
            " AND ((start >= :startTime AND stop <= :endTime) " +
            // Program is at the beginning of time slot
            "  OR (start <= :startTime AND stop > :startTime) " +
            // Program is at the end of the time slot
            "  OR (start < :endTime AND stop >= :endTime)) " +
            "GROUP BY p.id " +
            "ORDER BY start ASC")
    suspend fun loadProgramsFromChannelBetweenTimeSyncSuspendable(channelId: Int, startTime: Long, endTime: Long): List<EpgProgram>

    @Transaction
    @Query(PROGRAM_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            "GROUP BY p.id " +
            "ORDER BY p.start, p.channel_name ASC")
    fun loadPrograms(): LiveData<List<Program>>

    @Transaction
    @Query(PROGRAM_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            "GROUP BY p.id " +
            "ORDER BY p.start, p.channel_name ASC")
    fun loadProgramsSync(): List<Program>

    @Transaction
    @Query(PROGRAM_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND p.id = :id")
    fun loadProgramById(id: Int): LiveData<Program>

    @Transaction
    @Query(PROGRAM_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND p.id = :id")
    fun loadProgramByIdSync(id: Int): Program

    @Query(PROGRAM_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND p.channel_id = :channelId " +
            "ORDER BY start DESC LIMIT 1")
    fun loadLastProgramFromChannelSync(channelId: Int): Program

    @Query("DELETE FROM programs " + "WHERE stop < :time")
    fun deleteProgramsByTime(time: Long)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(programs: List<Program>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(program: Program)

    @Update
    fun update(programs: List<Program>)

    @Update
    fun update(vararg program: Program)

    @Delete
    fun delete(programs: List<Program>)

    @Delete
    fun delete(program: Program)

    @Query("DELETE FROM programs " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND id = :id")
    fun deleteById(id: Int)

    @Query("DELETE FROM programs")
    fun deleteAll()

    companion object {

        const val BASE_PROGRAM_BASE_QUERY = "SELECT p.id, p.title, p.subtitle, p.summary, p.description, " +
                "p.content_type, p.channel_id, " +
                "p.start, p.stop, " +
                "p.episode_on_screen, p.season_number, p.episode_number, p.part_number," +
                "c.name AS channel_name, " +
                "c.icon AS channel_icon " +
                "FROM programs AS p " +
                "LEFT JOIN channels AS c ON c.id = p.channel_id "

        const val PROGRAM_BASE_QUERY = "SELECT p.*," +
                "c.name AS channel_name, " +
                "c.icon AS channel_icon " +
                "FROM programs AS p " +
                "LEFT JOIN channels AS c ON c.id = p.channel_id "

        const val EPG_PROGRAM_BASE_QUERY = "SELECT p.id, " +
                "p.title, p.subtitle, " +
                "p.channel_id, " +
                "p.connection_id, " +
                "p.start, p.stop, " +
                "p.content_type " +
                "FROM programs AS p " +
                "LEFT JOIN channels AS c ON c.id = channel_id "

        const val CONNECTION_IS_ACTIVE = " p.connection_id IN (SELECT id FROM connections WHERE active = 1) "
    }
}
