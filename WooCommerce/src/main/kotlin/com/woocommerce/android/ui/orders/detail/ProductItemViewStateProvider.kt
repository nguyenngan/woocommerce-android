package com.woocommerce.android.ui.orders.detail

import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.model.order.Order
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.utility.ResourceProvider
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import java.math.BigDecimal
import java.text.NumberFormat
import javax.inject.Inject

class ProductItemViewStateProvider @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter
) {
    fun provide(
        item: Order.Item,
        image: String?,
        currency: String,
        isExpanded: Boolean
    ): OrderDetailProductItemViewState {
        val formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(currency)

        val numberFormatter = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 2
        }

        val isSkuVisible = item.sku.isNotEmpty() && isExpanded

        val orderTotal = formatCurrencyForDisplay(item.total)
        val productPrice = formatCurrencyForDisplay(item.price)
        val total = if (item.quantity > 1) {
            resourceProvider.getString(
                    R.string.orderdetail_product_lineitem_total_multiple,
                    orderTotal,
                    productPrice,
                    numberFormatter.format(item.quantity)
            )
        } else {
            resourceProvider.getString(R.string.orderdetail_product_lineitem_total_single, orderTotal)
        }

        val totalTax = formatCurrencyForDisplay(item.totalTax)

        return OrderDetailProductItemViewState(
                item.productId,
                item.variationId,
                item.name,
                numberFormatter.format(item.quantity),
                isExpanded,
                isSkuVisible,
                item.sku,
                total,
                totalTax,
                image
            )
    }
}

@Parcelize
data class OrderDetailProductItemViewState(
    val productId: Long,
    val variationId: Long,
    val name: String,
    val quantity: String,
    val isExpanded: Boolean,
    val isSkuVisible: Boolean,
    val sku: String,
    val total: String,
    val totalTax: String,
    val imageUrl: String?
) : Parcelable
