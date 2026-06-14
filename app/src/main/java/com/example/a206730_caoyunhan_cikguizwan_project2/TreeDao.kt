package com.example.a206730_caoyunhan_cikguizwan_project2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TreeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: TreeOrderEntity)

    @Query("SELECT * FROM tree_orders ORDER BY id ASC")
    fun getAllOrders(): Flow<List<TreeOrderEntity>>
}