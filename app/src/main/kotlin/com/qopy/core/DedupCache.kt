package com.qopy.core

class DedupCache(
    private val ttlMs: Long = 5000L,
    private val maxSize: Int = 1000
) {
    private val cache = object : LinkedHashMap<String, Long>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    fun insert(hash: ByteArray, nowMs: Long = System.currentTimeMillis()) {
        val hexKey = hash.joinToString("") { "%02x".format(it) }
        cache[hexKey] = nowMs
    }

    @Synchronized
    fun contains(hash: ByteArray, nowMs: Long = System.currentTimeMillis()): Boolean {
        val hexKey = hash.joinToString("") { "%02x".format(it) }
        val insertedAt = cache[hexKey] ?: return false
        if (nowMs - insertedAt > ttlMs) {
            cache.remove(hexKey)
            return false
        }
        return true
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }
}
