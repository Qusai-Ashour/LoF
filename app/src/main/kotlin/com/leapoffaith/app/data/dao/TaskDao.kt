package com.leapoffaith.app.data.dao

import androidx.room.*
import com.leapoffaith.app.data.entities.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId=:userId AND date=:date ORDER BY hourOfDay ASC, insertOrder ASC")
    fun getTasksByDate(userId: String, date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE userId=:userId AND date=:date ORDER BY hourOfDay ASC, insertOrder ASC")
    suspend fun getTasksByDateOnce(userId: String, date: String): List<Task>

    @Query("SELECT * FROM tasks WHERE userId=:userId AND date>=:start AND date<=:end ORDER BY date ASC, hourOfDay ASC")
    fun getTasksByDateRange(userId: String, start: String, end: String): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id=:id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET isCompleted=:done WHERE id=:id")
    suspend fun setTaskCompleted(id: Long, done: Boolean)
}
