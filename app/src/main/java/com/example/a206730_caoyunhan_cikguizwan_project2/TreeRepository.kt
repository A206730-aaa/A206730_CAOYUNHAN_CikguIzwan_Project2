package com.example.a206730_caoyunhan_cikguizwan_project2

import kotlinx.coroutines.flow.Flow

class TreeRepository(
    private val dao: TreeDao
) {
    val allOrders: Flow<List<TreeOrderEntity>> = dao.getAllOrders()

    suspend fun insert(order: TreeOrderEntity) {
        dao.insertOrder(order)
    }
}