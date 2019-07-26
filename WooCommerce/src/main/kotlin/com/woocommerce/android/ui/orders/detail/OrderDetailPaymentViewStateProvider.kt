package com.woocommerce.android.ui.orders.detail

import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.model.order.Order
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.utility.ResourceProvider
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import java.math.BigDecimal
import javax.inject.Inject

class OrderDetailPaymentViewStateProvider @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter
) {
    fun provide(order: Order): OrderDetailPaymentViewState {
        val formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(order.currency)

        // Populate or hide payment message
        val isPaymentMessageVisible = order.paymentMethodTitle.isNotEmpty()
        val paymentMessage = if (isPaymentMessageVisible) {
            when (order.status) {
                CoreOrderStatus.PENDING,
                CoreOrderStatus.ON_HOLD -> resourceProvider.getString(
                        R.string.orderdetail_payment_summary_onhold,
                        order.paymentMethodTitle
                )
                else -> resourceProvider.getString(
                        R.string.orderdetail_payment_summary_completed,
                        formatCurrencyForDisplay(order.total),
                        order.paymentMethodTitle
                )
            }
        } else {
            ""
        }

        // Populate or hide payment message
        val title: String
        var refundTotal = ""
        var totalAfterRefunds = ""
        val isRefundSectionVisible = order.refundTotal.abs() > BigDecimal.ZERO
        if (isRefundSectionVisible) {
            title = resourceProvider.getString(R.string.orderdetail_payment_refunded)
            refundTotal = formatCurrencyForDisplay(order.refundTotal)
            totalAfterRefunds = formatCurrencyForDisplay(order.total + order.refundTotal)
        } else {
            title = resourceProvider.getString(R.string.payment)
        }

        // Populate or hide discounts section
        var discountTotal = ""
        var discountItems = ""
        val isDiscountSectionVisible = order.discountTotal > BigDecimal.ZERO
        if (isDiscountSectionVisible) {
            discountTotal = formatCurrencyForDisplay(order.discountTotal)
            discountItems = resourceProvider.getString(R.string.orderdetail_discount_items, order.discountCodes)
        }

        return OrderDetailPaymentViewState(
                title,
                formatCurrencyForDisplay(order.items.sumBy { it.subtotal.toInt() }.toBigDecimal()),
                formatCurrencyForDisplay(order.shippingTotal),
                formatCurrencyForDisplay(order.totalTax),
                formatCurrencyForDisplay(order.total),
                refundTotal,
                isPaymentMessageVisible,
                paymentMessage,
                isRefundSectionVisible,
                totalAfterRefunds,
                isDiscountSectionVisible,
                discountTotal,
                discountItems
        )
    }
}

@Parcelize
data class OrderDetailPaymentViewState(
    val title: String,
    val subtotal: String,
    val shippingTotal: String,
    val taxesTotal: String,
    val total: String,
    val refundTotal: String,
    val isPaymentMessageVisible: Boolean,
    val paymentMessage: String,
    val isRefundSectionVisible: Boolean,
    val totalAfterRefunds: String,
    val isDiscountSectionVisible: Boolean,
    val discountTotal: String,
    val discountItems: String
) : Parcelable
