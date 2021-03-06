package com.woocommerce.android.ui.products

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore

class ProductShippingViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    selectedSite: SelectedSite,
    wooCommerceStore: WooCommerceStore,
    private val productRepository: ProductDetailRepository
) : ScopedViewModel(savedState, dispatchers) {
    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    private val navArgs: ProductShippingFragmentArgs by savedState.navArgs()

    var weightUnit: String? = null
        private set
    var dimensionUnit: String? = null
        private set

    init {
        val settings = wooCommerceStore.getProductSettings(selectedSite.get())
        weightUnit = settings?.weightUnit
        dimensionUnit = settings?.dimensionUnit

        loadProduct(navArgs.remoteProductId)
    }

    private fun loadProduct(remoteProductId: Long) {
        launch {
            val productInDb = productRepository.getProduct(remoteProductId)
            if (productInDb != null) {
                viewState = viewState.copy(product = productInDb)
            }
        }
    }

    @Parcelize
    data class ViewState(
        val product: Product? = null,
        val isProductUpdated: Boolean? = null,
        val shouldShowDiscardDialog: Boolean = true
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductShippingViewModel>
}
