package tech.favs.ebs.model

import tech.favs.ebs.util.OperationValueResult

interface ProductInformationExtractor {
    fun extract(urls: List<String>): OperationValueResult<out List<ProductInformation>>
}

data class ProductInformation(val productUrl: String, val imageUrl: String, val name: String, val formattedPrice: String)
