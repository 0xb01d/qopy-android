package com.qopy.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey
    @ColumnInfo(name = "device_id")
    val deviceId: String,

    @ColumnInfo(name = "device_name")
    val deviceName: String,

    @ColumnInfo(name = "os_type")
    val osType: String,

    @ColumnInfo(name = "public_key")
    val publicKey: ByteArray,

    @ColumnInfo(name = "tls_cert_sha256")
    val tlsCertSha256: ByteArray,

    @ColumnInfo(name = "last_known_ip")
    val lastKnownIp: String?,

    @ColumnInfo(name = "last_known_port")
    val lastKnownPort: Int?,

    @ColumnInfo(name = "is_trusted")
    val isTrusted: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
