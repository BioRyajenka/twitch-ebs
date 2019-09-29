package tech.favs.ebs.model

abstract class UrlAwareException : Throwable() {
    abstract val url: String
}

class ProductIdParseException(override val url: String) : UrlAwareException() {
    override val message = "URL указан неправильно. Обратитесь в техподдержку."
}

class UnknownTradingPlatformException(override val url: String) : UrlAwareException() {
    override val message = "Адрес данного магазина не поддерживается или введен неверно"
}
