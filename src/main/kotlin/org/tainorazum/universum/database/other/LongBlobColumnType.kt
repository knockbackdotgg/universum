package org.tainorazum.universum.database.other

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import java.sql.Blob

class LongBlobColumnType : ColumnType<ExposedBlob>() {
    override fun sqlType(): String = "LONGBLOB" // Changed from MEDIUMBLOB to LONGBLOB

    override fun valueFromDB(value: Any): ExposedBlob = when (value) {
        is ExposedBlob -> value
        is Blob -> ExposedBlob(value.binaryStream)
        is ByteArray -> ExposedBlob(value)
        else -> error("Unexpected value of type Blob: $value of ${value::class.qualifiedName}")
    }

    override fun notNullValueToDB(value: ExposedBlob): Any = value.inputStream

    override fun nonNullValueToString(value: ExposedBlob): String = "?"
}

fun Table.longBlob(name: String): Column<ExposedBlob> =
    registerColumn(name, LongBlobColumnType())