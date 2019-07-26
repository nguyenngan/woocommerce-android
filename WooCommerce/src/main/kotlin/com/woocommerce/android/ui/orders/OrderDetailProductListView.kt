package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_DETAIL_FULFILL_ORDER_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_DETAIL_PRODUCT_DETAIL_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_DETAIL_PRODUCT_TAPPED
import com.woocommerce.android.ui.orders.detail.OrderDetailProductItemViewState
import com.woocommerce.android.ui.orders.detail.ProductListViewState
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.util.toVisibility
import com.woocommerce.android.widgets.AlignedDividerDecoration
import kotlinx.android.synthetic.main.order_detail_product_list.view.*

class OrderDetailProductListView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_product_list, this)
    }
    private lateinit var divider: AlignedDividerDecoration
    private lateinit var viewAdapter: ProductListAdapter

    /**
     * Initialize and format this view.
     *
     * @param [viewState] The state information for displaying the product list.
     * @param [productListener] Listener for routing product click actions.
     * @param [orderListener] Listener for routing order click actions. If null, the buttons will be hidden.
     */
    fun initView(
        viewState: ProductListViewState,
        productListener: OrderProductActionListener,
        orderListener: OrderActionListener? = null
    ) {
        divider = AlignedDividerDecoration(context,
                DividerItemDecoration.VERTICAL, R.id.productInfo_name, clipToMargin = false)

        ContextCompat.getDrawable(context, R.drawable.list_divider)?.let { drawable ->
            divider.setDrawable(drawable)
        }

        val viewManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        viewAdapter = ProductListAdapter(viewState.itemViewStates, productListener)

        setupListeners(viewState, orderListener)

        productList_products.apply {
            setHasFixedSize(false)
            layoutManager = viewManager
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            adapter = viewAdapter
        }
    }

    private fun setupListeners(
        viewState: ProductListViewState,
        orderListener: OrderActionListener?
    ) {
        productList_btnFulfill.visibility = viewState.isFulfillButtonVisible.toVisibility()
        productList_btnDetails.visibility = viewState.isDetailsButtonVisible.toVisibility()

        orderListener?.let { listener ->
            productList_btnFulfill.setOnClickListener {
                AnalyticsTracker.track(ORDER_DETAIL_FULFILL_ORDER_BUTTON_TAPPED)
                listener.openOrderFulfillment(viewState.orderNumber, viewState.orderIdentifier)
            }

            productList_btnDetails.setOnClickListener {
                AnalyticsTracker.track(ORDER_DETAIL_PRODUCT_DETAIL_BUTTON_TAPPED)
                listener.openOrderProductList(viewState.orderNumber, viewState.orderIdentifier)
            }
        }

        if (viewState.isExpanded) {
            productList_products.addItemDecoration(divider)
        }
    }

    // called when a product is fetched to ensure we show the correct product image
    fun refreshProductImages() {
        if (::viewAdapter.isInitialized) {
            viewAdapter.notifyDataSetChanged()
        }
    }

    class ProductListAdapter(
        private val orderItems: List<OrderDetailProductItemViewState>,
        private val productListener: OrderProductActionListener
    ) : RecyclerView.Adapter<ProductListAdapter.ViewHolder>() {
        class ViewHolder(val view: OrderDetailProductItemView) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: OrderDetailProductItemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.order_detail_product_list_item, parent, false)
                    as OrderDetailProductItemView
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = orderItems[position]
            val productId = ProductHelper.productOrVariationId(item.productId, item.variationId)
            holder.view.initView(orderItems[position])
            holder.view.setOnClickListener {
                AnalyticsTracker.track(ORDER_DETAIL_PRODUCT_TAPPED)
                productListener.openOrderProductDetail(productId)
            }
        }

        override fun getItemCount() = orderItems.size
    }
}
