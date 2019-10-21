package tech.favs.ebs.tradingplatforms.yamandberu

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import tech.favs.ebs.dao.DeeplinkDao
import tech.favs.ebs.dao.Deeplinks
import tech.favs.ebs.model.Deeplink
import tech.favs.ebs.model.DeeplinkGenerator
import tech.favs.ebs.util.OperationListResult
import tech.favs.ebs.util.OperationValueResult

// TODO: also save it somewhere
private fun generateSubId(streamerId: Int, url: String): Int {
    // TODO: suspiciously same code. think about refactor
    val existentSubId = transaction {
        DeeplinkDao.wrapRows(Deeplinks.select {
            Deeplinks.streamerId eq streamerId and (Deeplinks.url eq url)
        }).toList().let {
            check(it.size <= 1)
            it.singleOrNull()?.subId
        }
    }
    if (existentSubId != null) return existentSubId // already present

    val highest = transaction {
        val maxIdExpression = Deeplinks.subId.max()
        Deeplinks.slice(maxIdExpression)
                .select(Deeplinks.url eq url)
                .single()[maxIdExpression] ?: -1
    }
    if (highest == 999) {
        error("According to YAM API, vid should be less than 1000. " +
                "https://yandex.ru/support/market-distr/partner-links/partner-links-template.html#partners-links-template")
    }
    return highest + 1
}

abstract class YandexDeeplinkGenerator(private val clid: Int) : DeeplinkGenerator {
    protected abstract val fixedPart: String

    override fun generate(streamerId: Int, urls: List<String>): OperationListResult<String, Deeplink> {
        return OperationListResult.fromList(urls) { url ->
            val subId = generateSubId(streamerId, url)
            val deeplink = "$url?$fixedPart&clid=$clid&vid=$subId"
            OperationValueResult.success(Deeplink(url, deeplink))
        }
    }
}

class YAMDeeplinkGenerator(clid: Int) : YandexDeeplinkGenerator(clid) {
    override val fixedPart: String = "pp=900&mclid=1003&distr_type=7"
}

class BeruDeeplinkGenerator(clid: Int) : YandexDeeplinkGenerator(clid) {
    override val fixedPart: String = "pp=1900&mclid=1003&distr_type=7"
}
