package com.example.a206730_caoyunhan_cikguizwan_project2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tree_orders")
data class TreeOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,
    val tree: String,
    val location: String,
    val price: Int
)