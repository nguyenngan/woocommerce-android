package com.woocommerce.android.ui.products

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_LOADED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_UPDATE_ERROR
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_UPDATE_SUCCESS
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.model.toDataModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.PRODUCTS
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT
import org.wordpress.android.fluxc.action.WCProductAction.UPDATED_PRODUCT
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductUpdated
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@OpenClassOnDebug
class ProductDetailRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
    }

    private var continuation: Continuation<Boolean>? = null
    private var continuationUpdateProduct: Continuation<Boolean>? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun fetchProduct(remoteProductId: Long): Product? {
        suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
            continuation = it

            val payload = WCProductStore.FetchSingleProductPayload(selectedSite.get(), remoteProductId)
            dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductAction(payload))
        }

        continuation = null
        return getProduct(remoteProductId)
    }

    /**
     * Fires the request to update the product
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun updateProduct(updatedProduct: Product): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationUpdateProduct = it

                val payload = WCProductStore.UpdateProductPayload(selectedSite.get(), updatedProduct.toDataModel())
                dispatcher.dispatch(WCProductActionBuilder.newUpdateProductAction(payload))
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(PRODUCTS, "Exception encountered while updating product", e)
            false
        }
    }

    fun getProduct(remoteProductId: Long): Product? =
            productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)?.toAppModel()

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.causeOfChange == FETCH_SINGLE_PRODUCT) {
            if (event.isError) {
                continuation?.resume(false)
            } else {
                AnalyticsTracker.track(PRODUCT_DETAIL_LOADED)
                continuation?.resume(true)
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductUpdated(event: OnProductUpdated) {
        if (event.causeOfChange == UPDATED_PRODUCT) {
            if (event.isError) {
                AnalyticsTracker.track(PRODUCT_DETAIL_UPDATE_ERROR)
                continuationUpdateProduct?.resume(false)
            } else {
                AnalyticsTracker.track(PRODUCT_DETAIL_UPDATE_SUCCESS)
                continuationUpdateProduct?.resume(true)
            }
            continuationUpdateProduct = null
        }
    }
}
