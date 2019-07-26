package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.ui.orders.detail.OrderDetailProductItemViewState
import com.woocommerce.android.util.toVisibility
import kotlinx.android.synthetic.main.order_detail_product_item.view.*
import org.wordpress.android.util.PhotonUtils
import java.text.NumberFormat

class OrderDetailProductItemView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_product_item, this)
    }

    fun initView(viewState: OrderDetailProductItemViewState) {
        productInfo_name.text = viewState.name

        productInfo_qty.text = viewState.quantity

        // Modify views for expanded or collapsed mode
        productInfo_productTotal.visibility = viewState.isExpanded.toVisibility()
        productInfo_totalTax.visibility = viewState.isExpanded.toVisibility()
        productInfo_lblTax.visibility = viewState.isExpanded.toVisibility()
        productInfo_name.setSingleLine(!viewState.isExpanded)

        productInfo_lblSku.visibility = viewState.isSkuVisible.toVisibility()
        productInfo_sku.visibility = viewState.isSkuVisible.toVisibility()
        productInfo_sku.text = viewState.sku

        if (viewState.isExpanded) {
            // Populate formatted total and tax values
            productInfo_productTotal.text = viewState.total
            productInfo_totalTax.text = viewState.total
        } else {
            // vertically center the product name and quantity since they're the only text showing
            val set = ConstraintSet()
            set.clone(this)
            set.centerVertically(productInfo_name.id, ConstraintSet.PARENT_ID)
            set.centerVertically(productInfo_qty.id, ConstraintSet.PARENT_ID)
            set.applyTo(this)
        }

        viewState.imageUrl?.let {
            val imageSize = context.resources.getDimensionPixelSize(R.dimen.product_icon_sz)
            val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
            GlideApp.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(productInfo_icon)
        } ?: productInfo_icon.setImageResource(R.drawable.ic_product)
    }
}
