package com.puneeth50.ailinguakit.data

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter

@ProvidedTypeConverter
class PackStatusConverter {

    @TypeConverter
    fun fromStatus(status: PackStatus): String = status.name

    @TypeConverter
    fun toStatus(name: String): PackStatus =
        enumValues<PackStatus>().firstOrNull { it.name == name }
            ?: PackStatus.NOT_DOWNLOADED
}