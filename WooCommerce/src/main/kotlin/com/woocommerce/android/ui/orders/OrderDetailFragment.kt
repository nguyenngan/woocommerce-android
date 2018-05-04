package com.woocommerce.android.ui.orders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.main.MainActivity
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_detail.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import javax.inject.Inject

class OrderDetailFragment : Fragment(), OrderDetailContract.View {
    companion object {
        const val FIELD_ORDER_ID = "order-id"
        const val FIELD_ORDER_NUMBER = "order-number"
        fun newInstance(order: WCOrderModel): Fragment {
            val args = Bundle()
            args.putInt(FIELD_ORDER_ID, order.id)

            // Use for populating the title only, not for record retrieval
            args.putString(FIELD_ORDER_NUMBER, order.number)
            val fragment = OrderDetailFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var presenter: OrderDetailContract.Presenter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_order_detail, container, false)

        arguments?.let { arguments ->
            // Set activity title
            val orderNumber = arguments.getString(FIELD_ORDER_NUMBER, "")
            activity?.title = getString(R.string.orderdetail_orderstatus_ordernum, orderNumber.toString())
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)
        val orderId = arguments?.getInt(FIELD_ORDER_ID, 0)
        orderId?.let {
            presenter.loadOrderDetail(it)
        }
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun showOrderDetail(order: WCOrderModel?, notes: List<WCOrderNoteModel>) {
        order?.let {
            // Populate the Order Status Card
            orderDetail_orderStatus.initView(order)

            // Populate the Order Product List Card
            orderDetail_productList.initView(order)

            // Populate the Customer Information Card
            orderDetail_customerInfo.initView(order, this)

            // Populate the Payment Information Card
            orderDetail_paymentInfo.initView(order)

            // Check for customer note, show if available
            if (order.customerNote.isEmpty()) {
                orderDetail_customerNote.visibility = View.GONE
            } else {
                orderDetail_customerNote.visibility = View.VISIBLE
                orderDetail_customerNote.initView(order)
            }
        }
        // Populate order notes card
        orderDetail_noteList.initView(notes)
    }

    override fun updateOrderNotes(notes: List<WCOrderNoteModel>) {
        // Update the notes in the notes card
        orderDetail_noteList.updateView(notes)
    }

    override fun getSelectedSite() = (activity as? MainActivity)?.getSelectedSite()

    // region OrderActionListener
    override fun dialPhone(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phone")
        startActivity(intent)
    }

    override fun createEmail(emailAddr: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:$emailAddr") // only email apps should handle this
        context?.let { context ->
            if (intent.resolveActivity(context.packageManager) != null) {
                startActivity(intent)
            }
        }
    }

    override fun sendSms(phone: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:$phone")
        context?.let { context ->
            if (intent.resolveActivity(context.packageManager) != null) {
                startActivity(intent)
            }
        }
    }
    // endregion
}