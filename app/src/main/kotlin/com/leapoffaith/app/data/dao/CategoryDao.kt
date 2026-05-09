package com.leapoffaith.app.data.dao

import androidx.room.*
import com.leapoffaith.app.data.entities.CategoryDefinition
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId=:userId AND type=:type ORDER BY orderIndex ASC")
    fun getCategoriesByType(userId: String, type: String): Flow<List<CategoryDefinition>>

    @Query("SELECT * FROM categories WHERE userId=:userId ORDER BY type ASC, orderIndex ASC")
    fun getAllCategories(userId: String): Flow<List<CategoryDefinition>>

    @Query("SELECT * FROM categories WHERE userId=:userId ORDER BY type ASC, orderIndex ASC")
    suspend fun getAllCategoriesOnce(userId: String): List<CategoryDefinition>

    @Query("SELECT * FROM categories WHERE userId=:userId AND type='PREPARE' ORDER BY orderIndex ASC")
    suspend fun getPrepareCategoriesOnce(userId: String): List<CategoryDefinition>

    @Query("SELECT * FROM categories WHERE userId=:userId AND type='RECORD' ORDER BY orderIndex ASC")
    suspend fun getRecordCategoriesOnce(userId: String): List<CategoryDefinition>

    @Query("SELECT * FROM categories WHERE id=:id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryDefinition?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(cat: CategoryDefinition): Long

    @Update
    suspend fun updateCategory(cat: CategoryDefinition)

    @Query("DELETE FROM categories WHERE id=:id")
    suspend fun deleteCategoryById(id: Long)
}
