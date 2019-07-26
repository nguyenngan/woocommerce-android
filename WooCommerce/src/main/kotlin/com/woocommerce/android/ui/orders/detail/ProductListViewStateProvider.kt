package com.woocommerce.android.ui.orders.detail

import android.os.Parcelable
import com.woocommerce.android.model.order.Order
import com.woocommerce.android.tools.ProductImageMap
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus.PROCESSING
import javax.inject.Inject

class ProductListViewStateProvider @Inject constructor(
    private val productImageMap: ProductImageMap,
    private val listItemProvider: ProductItemViewStateProvider
) {
    fun provide(
        order: Order,
        isExpanded: Boolean,
        hideAllButtons: Boolean
    ): ProductListViewState {
        return ProductListViewState(
            order.number,
            order.identifier,
            isExpanded,
            order.items.map { item ->
                listItemProvider.provide(
                        item,
                        productImageMap.get(item.productId),
                        order.currency,
                        isExpanded
                )
            },
            order.status == PROCESSING && !hideAllButtons,
            order.status != PROCESSING && !hideAllButtons
        )
    }
}

@Parcelize
data class ProductListViewState(
    val orderNumber: String,
    val orderIdentifier: OrderIdentifier,
    val isExpanded: Boolean,
    val itemViewStates: List<OrderDetailProductItemViewState>,
    val isFulfillButtonVisible: Boolean,
    val isDetailsButtonVisible: Boolean
) : Parcelable
