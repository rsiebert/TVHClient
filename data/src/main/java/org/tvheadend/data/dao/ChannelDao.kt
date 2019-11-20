package org.tvheadend.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.EpgChannel

@Dao
interface ChannelDao {

    @get:Query("SELECT COUNT (*) FROM channels AS c " +
            " WHERE $CONNECTION_IS_ACTIVE")
    val itemCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM channels AS c " +
            " WHERE $CONNECTION_IS_ACTIVE")
    val itemCountSync: Int

    @Query("SELECT c.* FROM channels AS c " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND c.id = :id")
    fun loadChannelByIdSync(id: Int): Channel

    @Transaction
    @Query(CHANNEL_BASE_QUERY +
            " LEFT JOIN programs AS program ON program.start <= :time AND program.stop > :time AND program.channel_id = c.id " +
            " LEFT JOIN programs AS next_program ON next_program.start = program.stop AND next_program.channel_id = c.id " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND c.id = :id")
    fun loadChannelByIdWithProgramsSync(id: Int, time: Long): Channel

    @Query("SELECT c.* FROM channels AS c " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " GROUP BY c.id " +
            ORDER_BY)
    fun loadAllChannelsSync(sortOrder: Int): List<Channel>

    @Transaction
    @Query(CHANNEL_BASE_QUERY +
            " LEFT JOIN programs AS program ON program.start <= :time AND program.stop > :time AND program.channel_id = c.id " +
            " LEFT JOIN programs AS next_program ON next_program.start = program.stop AND next_program.channel_id = c.id " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " GROUP BY c.id " +
            ORDER_BY)
    fun loadAllChannelsByTime(time: Long, sortOrder: Int): LiveData<List<Channel>>

    @Transaction
    @Query(CHANNEL_BASE_QUERY +
            " LEFT JOIN programs AS program ON program.start <= :time AND program.stop > :time AND program.channel_id = c.id " +
            " LEFT JOIN programs AS next_program ON next_program.start = program.stop AND next_program.channel_id = c.id " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND c.id IN (SELECT channel_id FROM tags_and_channels WHERE tag_id IN (:tagIds)) " +
            " GROUP BY c.id " +
            ORDER_BY)
    fun loadAllChannelsByTimeAndTag(time: Long, sortOrder: Int, tagIds: List<Int>): LiveData<List<Channel>>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(channel: Channel)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(channels: List<Channel>)

    @Update
    fun update(channel: Channel)

    @Delete
    fun delete(channel: Channel)

    @Query("DELETE FROM channels " +
            " WHERE id = :id " +
            " AND connection_id IN (SELECT id FROM connections WHERE active = 1)")
    fun deleteById(id: Int)

    @Query("DELETE FROM channels")
    fun deleteAll()

    @Query(EPG_CHANNEL_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            ORDER_BY)
    fun loadAllEpgChannels(sortOrder: Int): LiveData<List<EpgChannel>>

    @Query(EPG_CHANNEL_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            ORDER_BY)
    suspend fun loadAllEpgChannelsSync(sortOrder: Int): List<EpgChannel>

    @Query(EPG_CHANNEL_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND c.id IN (SELECT channel_id FROM tags_and_channels WHERE tag_id IN (:tagIds)) " +
            ORDER_BY)
    fun loadAllEpgChannelsByTag(sortOrder: Int, tagIds: List<Int>): LiveData<List<EpgChannel>>

    companion object {

        const val CHANNEL_BASE_QUERY = "SELECT DISTINCT c.*, " +
                "program.id AS program_id, " +
                "program.title AS program_title, " +
                "program.subtitle AS program_subtitle, " +
                "program.start AS program_start, " +
                "program.stop AS program_stop, " +
                "program.content_type AS program_content_type, " +
                "next_program.id AS next_program_id, " +
                "next_program.title AS next_program_title " +
                "FROM channels AS c "

        const val EPG_CHANNEL_BASE_QUERY = "SELECT c.id, " +
                "c.name, " +
                "c.icon, " +
                "c.number, " +
                "c.number_minor, " +
                "c.display_number " +
                "FROM channels AS c "

        const val ORDER_BY = " ORDER BY " +
                "CASE :sortOrder WHEN 0 THEN c.server_order END ASC," +
                "CASE :sortOrder WHEN 1 THEN c.server_order END DESC," +
                "CASE :sortOrder WHEN 2 THEN c.id END ASC," +
                "CASE :sortOrder WHEN 3 THEN c.id END DESC," +
                "CASE :sortOrder WHEN 4 THEN c.name END ASC," +
                "CASE :sortOrder WHEN 5 THEN c.name END DESC," +
                "CASE :sortOrder WHEN 6 THEN " +
                "   CASE (c.display_number + 0) WHEN 0 THEN 999999999 ELSE (c.display_number + 0) END " +
                "END ASC," +
                "CASE :sortOrder WHEN 7 THEN (c.display_number + 0) END DESC"

        const val CONNECTION_IS_ACTIVE = " c.connection_id IN (SELECT id FROM connections WHERE active = 1) "
    }
}
