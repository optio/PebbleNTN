package com.pebblentn.app.rules

import com.pebblentn.app.notification.NotificationSnapshot

/** Resolves a rule `field` name to a value from a [NotificationSnapshot]. Unknown fields → null. */
object SnapshotFields {
    fun resolve(snapshot: NotificationSnapshot, field: String): String? = when (field) {
        "packageName" -> snapshot.packageName
        "title" -> snapshot.title
        "text" -> snapshot.text
        "subText" -> snapshot.subText
        "bigText" -> snapshot.bigText
        "summaryText" -> snapshot.summaryText
        "infoText" -> snapshot.infoText
        "category" -> snapshot.category
        "channelId" -> snapshot.channelId
        "template" -> snapshot.template
        "combinedText" -> snapshot.combinedText
        else -> null
    }
}
