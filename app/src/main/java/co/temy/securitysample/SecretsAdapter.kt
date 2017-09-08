package co.temy.securitysample

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import co.temy.securitysample.extentions.inflate
import kotlinx.android.synthetic.main.item_secret.view.*
import java.text.DateFormat
import java.text.SimpleDateFormat

class SecretsAdapter(
        private var secrets: List<Storage.SecretData>,
        private val listener: (Storage.SecretData) -> Unit) : RecyclerView.Adapter<SecretsAdapter.Holder>() {

    private val dateFormatter = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM)

    override fun getItemCount() = secrets.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(parent.inflate(R.layout.item_secret))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(secrets[position], listener, dateFormatter)

    fun update(secrets: List<Storage.SecretData>) {
        this.secrets = secrets
        notifyDataSetChanged()
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: Storage.SecretData, listener: (Storage.SecretData) -> Unit, dateFormatter: DateFormat) = with(itemView) {
            titleView.text = item.alias
            dateView.text = dateFormatter.format(item.createDate)
            setOnClickListener { listener(item) }
        }
    }
}