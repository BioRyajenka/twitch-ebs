package tech.favs.ebs.dao

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import tech.favs.ebs.model.Deeplink

private const val MAX_URL_LENGTH = 300

object Deeplinks : IntIdTable() {
    val streamerId = integer("streamer_id")
    // TODO if I store url as is, I am vulnerable to SQL injection
    val url = varchar("url", MAX_URL_LENGTH)
    val subId = integer("sub_id")
    val deeplink = varchar("deeplink", MAX_URL_LENGTH)

    init {
        index(true, streamerId, url)
    }

    fun selectByStreamerIdAndUrl(streamerId: Int, url: String): DeeplinkDao? = transaction {
        DeeplinkDao.wrapRows(
                Deeplinks.select { (Deeplinks.streamerId eq streamerId) and (Deeplinks.url eq url) }
        ).toList().let {
            check(it.size <= 1) // index should be unique
            it.singleOrNull()
        }
    }

    fun insertOrUpdateDeeplink(deeplink: Deeplink): Unit = transaction {
        val existent = selectByStreamerIdAndUrl(deeplink.streamerId, deeplink.originalUrl)
        if (existent == null) {
            Deeplinks.insert {
                it[streamerId] = deeplink.streamerId
                it[url] = deeplink.originalUrl
                it[subId] = deeplink.subId
                it[Deeplinks.deeplink] = deeplink.deeplink
            }
        } else {
            Deeplinks.update({(streamerId eq deeplink.streamerId) and (url eq deeplink.originalUrl)}) {
                it[subId] = deeplink.subId
                it[Deeplinks.deeplink] = deeplink.deeplink
            }
        }
    }
}

class DeeplinkDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DeeplinkDao>(Deeplinks)

//    val streamerId by Deeplinks.streamerId
    val url by Deeplinks.url
    val subId by Deeplinks.subId
}
