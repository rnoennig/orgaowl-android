package de.rnoennig.orgaowl.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Task(
    @PrimaryKey val uuid: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "name") val name: String = "Unnamed Task",
    @ColumnInfo(name = "extra", defaultValue = "") val extra: String = "",
    @ColumnInfo(name = "done") val done: Boolean = false,
    @ColumnInfo(name = "modified_at") val modifiedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "tasklist") val tasklist: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001"),
    @ColumnInfo(name = "imagepath", defaultValue = "") val imagePath: String = ""
)