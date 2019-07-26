package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.order.OrderIdentifier

/**
 * Interface for handling individual order actions from a child fragment.
 */
interface OrderActionListener {
    fun openOrderFulfillment(orderNumber: String, orderIdentifier: OrderIdentifier)
    fun openOrderProductList(orderNumber: String, orderIdentifier: OrderIdentifier)
}
