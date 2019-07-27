package com.woocommerce.android.ui.orders.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.order.Order
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.OrderDetailPresenter
import com.woocommerce.android.ui.orders.OrderDetailPresenter.Companion
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import com.woocommerce.android.viewmodel.utility.map
import com.woocommerce.android.viewmodel.utility.merge
import kotlinx.coroutines.CoroutineDispatcher
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.NotificationAction.MARK_NOTIFICATIONS_READ
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import javax.inject.Inject
import javax.inject.Named

class OrderDetailViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val paymentInfoViewStateProvider: OrderDetailPaymentViewStateProvider,
    private val productListViewStateProvider: ProductListViewStateProvider,
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
    private val uiMessageResolver: UIMessageResolver,
    private val networkStatus: NetworkStatus,
    private val notificationStore: NotificationStore
) : ScopedViewModel(mainDispatcher) {
    companion object {
        private val TAG: String = OrderDetailViewModel::class.java.simpleName
    }
    private var pendingMarkReadNotification: NotificationModel? = null

    private val _order = MutableLiveData<Order>()
    val order: LiveData<Order> = _order

    private val _orderStatus = MutableLiveData<Pair<Order, WCOrderStatusModel>>()
    val orderStatus: LiveData<Pair<Order, WCOrderStatusModel>> = merge(_orderStatus, order.map { order ->
        return@map getOrderStatus(order)
    })

    private val _paymentInfoData = MutableLiveData<OrderDetailPaymentViewState>()
    val paymentInfoData: LiveData<OrderDetailPaymentViewState> = _paymentInfoData

    private val _productListData = MutableLiveData<ProductListViewState>()
    val productListData: LiveData<ProductListViewState> = _productListData

    private val _onRefreshProductImages = SingleLiveEvent<Unit>()
    val onRefreshProductImages: LiveData<Unit> = _onRefreshProductImages

//    private val _onRefreshProductImages = SingleLiveEvent<Unit>()
//    val onRefreshProductImages: LiveData<Unit> = _onRefreshProductImages

    private val _customerInfoData = SingleLiveEvent<Order>()
    val customerInfoData: LiveData<Order> = _customerInfoData

    private var isStarted = false

    fun updatePaymentInfo(order: Order) {
        _paymentInfoData.value = paymentInfoViewStateProvider.provide(order)
    }

    fun updateProductList(order: Order, isExpanded: Boolean = productListData.value?.isExpanded ?: true) {
        _productListData.value = productListViewStateProvider.provide(order, isExpanded, false)
    }

    fun start() {
        if (!isStarted) {
            isStarted = true
            dispatcher.register(this)
        }
    }

    override fun onCleared() {
        super.onCleared()

        dispatcher.unregister(this)
    }

    private fun getOrderStatus(order: Order): Pair<Order, WCOrderStatusModel> {
        val key = order.status.value
        val status = orderStore.getOrderStatusForSiteAndKey(selectedSite.get(), key) ?: WCOrderStatusModel().apply {
            statusKey = key
            label = key
        }
        return Pair(order, status)
    }

    private fun onNotificationMarkedRead(event: OnNotificationChanged) {
        pendingMarkReadNotification?.let {
            // We only care about logging an error
            if (event.changedNotificationLocalIds.contains(it.noteId)) {
                if (event.isError) {
                    WooLog.e(NOTIFICATIONS, "$TAG - Error marking new order notification as read!")
                    pendingMarkReadNotification = null
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        if (event.causeOfChange == MARK_NOTIFICATIONS_READ) {
             onNotificationMarkedRead(event)
        }
    }

    @Suppress
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderStatusOptionsChanged(event: OnOrderStatusOptionsChanged) {
        if (event.isError) {
            WooLog.e(
                    WooLog.T.ORDERS, "$TAG " +
                    "- Error fetching order status options from the api : ${event.error.message}")
            return
        }

        order.value?.let {
            _orderStatus.value = getOrderStatus(it)
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductChanged(event: OnProductChanged) {
        // product was just fetched, show its image
        if (event.causeOfChange == FETCH_SINGLE_PRODUCT && !event.isError) {
            _onRefreshProductImages.call()

            // Refresh the customer info section, once the product information becomes available
            _customerInfoData.value = order.value
        }
    }
}
