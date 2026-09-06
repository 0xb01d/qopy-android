package com.qopy.core

object LwwOrdering {
    /**
     * Determines whether incoming item should overwrite existing item based on LWW rules:
     * 1. Higher timestampMs wins.
     * 2. If timestamps are equal, lexicographically greater sourceDeviceId wins.
     * 3. If deviceIds are equal, lexicographically greater eventId wins.
     */
    fun shouldOverwrite(incoming: ClipboardItem, existing: ClipboardItem): Boolean {
        if (incoming.timestampMs != existing.timestampMs) {
            return incoming.timestampMs > existing.timestampMs
        }
        val devCmp = incoming.sourceDeviceId.compareTo(existing.sourceDeviceId)
        if (devCmp != 0) {
            return devCmp > 0
        }
        return incoming.eventId > existing.eventId
    }
}
