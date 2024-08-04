package de.rnoennig.orgaowl.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Tasklist(
    @PrimaryKey val uuid: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "name") val name: String = "Unnamed List",
    @ColumnInfo(name = "modified_at") val modifiedAt: Long = System.currentTimeMillis()
)