package de.rnoennig.orgaowl.persistence

import androidx.room.Embedded
import androidx.room.Relation

data class TasklistWithTasks(
    @Embedded val tasklist: Tasklist,
    @Relation(
        parentColumn = "uuid",
        entityColumn = "tasklist"
    )
    val tasks: List<Task> = listOf()
)