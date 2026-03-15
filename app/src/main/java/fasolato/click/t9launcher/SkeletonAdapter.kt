package fasolato.click.t9launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class SkeletonAdapter(private val count: Int) : RecyclerView.Adapter<SkeletonAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_skeleton, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    override fun getItemCount() = count
}
