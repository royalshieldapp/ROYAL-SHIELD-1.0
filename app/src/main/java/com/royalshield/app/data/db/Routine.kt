package com.royalshield.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val isEnabled: Boolean,
    val triggerType: TriggerType,
    val triggerParams: String,
    val actionsJson: String,
    val iconName: String,
    val colorHex: String
)
