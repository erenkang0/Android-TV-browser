package com.erenkang.tvbrowser.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.erenkang.tvbrowser.R

data class RowItem(val title: String, val subtitle: String, val showAction: Boolean = true)

class RowAdapter(
    val items: MutableList<RowItem>,
    private val onClick: (Int) -> Unit,
    private val onAction: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<RowAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.rowTitle)
        val subtitle: TextView = view.findViewById(R.id.rowSubtitle)
        val action: ImageButton = view.findViewById(R.id.rowAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        holder.subtitle.visibility = if (item.subtitle.isBlank()) View.GONE else View.VISIBLE
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onClick(pos)
        }
        if (item.showAction && onAction != null) {
            holder.action.visibility = View.VISIBLE
            holder.action.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onAction.invoke(pos)
            }
        } else {
            holder.action.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size
}
