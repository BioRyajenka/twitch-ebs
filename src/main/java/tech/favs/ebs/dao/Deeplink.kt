package tech.favs.ebs.dao

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

private const val MAX_URL_LENGTH = 300

object Deeplinks : IntIdTable() {
    val streamerId = integer("streamer_id")
    val url = varchar("url", MAX_URL_LENGTH)
    val subId = integer("sub_id")
    val deeplink = varchar("deeplink", MAX_URL_LENGTH)

    init {
        index(true, streamerId, url)
    }
}

class DeeplinkDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DeeplinkDao>(Deeplinks)

    val streamerId by Deeplinks.streamerId
    val url by Deeplinks.url
    val subId by Deeplinks.subId
}
