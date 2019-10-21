package tech.favs.ebs.model

import tech.favs.ebs.util.OperationListResult

interface ProductInformationExtractor {
    fun extract(urls: List<String>): OperationListResult<String, out ProductInformation>
}

data class ProductInformation(val productUrl: String, val imageUrl: String, val name: String, val formattedPrice: String)
