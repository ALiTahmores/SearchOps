package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SeoDao {
    // --- Clients ---
    @Query("SELECT * FROM clients ORDER BY createdAt DESC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :clientId")
    fun getClientByIdFlow(clientId: Int): Flow<Client?>

    @Query("SELECT * FROM clients WHERE id = :clientId")
    suspend fun getClientById(clientId: Int): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)

    @Query("DELETE FROM clients WHERE id = :clientId")
    suspend fun deleteClientById(clientId: Int)

    // --- Keywords ---
    @Query("SELECT * FROM keywords WHERE clientId = :clientId ORDER BY currentRank ASC")
    fun getKeywordsForClient(clientId: Int): Flow<List<Keyword>>

    @Query("SELECT * FROM keywords ORDER BY searchVolume DESC")
    fun getAllKeywords(): Flow<List<Keyword>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyword(keyword: Keyword)

    @Delete
    suspend fun deleteKeyword(keyword: Keyword)

    @Query("DELETE FROM keywords WHERE clientId = :clientId")
    suspend fun deleteKeywordsForClient(clientId: Int)

    // --- Tasks ---
    @Query("SELECT * FROM tasks WHERE clientId = :clientId ORDER BY id DESC")
    fun getTasksForClient(clientId: Int): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)

    @Query("DELETE FROM tasks WHERE clientId = :clientId")
    suspend fun deleteTasksForClient(clientId: Int)

    // --- Audits ---
    @Query("SELECT * FROM audits WHERE clientId = :clientId ORDER BY lastUpdated DESC LIMIT 1")
    fun getLatestAuditForClient(clientId: Int): Flow<AuditResult?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditResult(auditResult: AuditResult)

    @Query("DELETE FROM audits WHERE clientId = :clientId")
    suspend fun deleteAuditsForClient(clientId: Int)

    // --- Chat History ---
    @Query("SELECT * FROM chat_history WHERE clientId = :clientId ORDER BY timestamp ASC")
    fun getChatHistoryForClient(clientId: Int): Flow<List<ChatHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatHistory)

    @Query("DELETE FROM chat_history WHERE clientId = :clientId")
    suspend fun deleteChatHistoryForClient(clientId: Int)

    // --- Crawled Pages ---
    @Query("SELECT * FROM crawled_pages WHERE clientId = :clientId")
    fun getCrawledPages(clientId: Int): Flow<List<CrawledPage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrawledPage(page: CrawledPage)

    @Query("DELETE FROM crawled_pages WHERE clientId = :clientId")
    suspend fun deleteCrawledPagesForClient(clientId: Int)

    // --- Competitors ---
    @Query("SELECT * FROM competitors WHERE clientId = :clientId")
    fun getCompetitors(clientId: Int): Flow<List<Competitor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompetitor(competitor: Competitor)

    @Query("DELETE FROM competitors WHERE clientId = :clientId")
    suspend fun deleteCompetitorsForClient(clientId: Int)

    @Query("DELETE FROM clients")
    suspend fun deleteAllClients()

    @Query("DELETE FROM keywords")
    suspend fun deleteAllKeywords()

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM audits")
    suspend fun deleteAllAudits()

    @Query("DELETE FROM chat_history")
    suspend fun deleteAllChatHistory()

    @Query("DELETE FROM crawled_pages")
    suspend fun deleteAllCrawledPages()

    @Query("DELETE FROM competitors")
    suspend fun deleteAllCompetitors()
}
