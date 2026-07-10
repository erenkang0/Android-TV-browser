package com.erenkang.tvbrowser.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.erenkang.tvbrowser.R
import kotlin.math.abs

data class SpeedDialItem(val title: String, val url: String)

class SpeedDialAdapter(
    private val items: List<SpeedDialItem>,
    private val onClick: (SpeedDialItem) -> Unit
) : RecyclerView.Adapter<SpeedDialAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val letter: TextView = view.findViewById(R.id.letterView)
        val name: TextView = view.findViewById(R.id.nameView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_speed_dial, parent, false)
        view.setOnFocusChangeListener { v, hasFocus ->
            val scale = if (hasFocus) 1.08f else 1f
            v.animate().scaleX(scale).scaleY(scale).setDuration(120).start()
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.title
        holder.letter.text = item.title.take(1).uppercase()
        val color = PALETTE[abs(item.title.hashCode()) % PALETTE.size]
        holder.letter.background.mutate().setTint(color)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    companion object {
        private val PALETTE = intArrayOf(
            Color.parseColor("#4C8DFF"),
            Color.parseColor("#E5484D"),
            Color.parseColor("#30A46C"),
            Color.parseColor("#F76B15"),
            Color.parseColor("#8E4EC6"),
            Color.parseColor("#00A2C7"),
            Color.parseColor("#D6409F"),
            Color.parseColor("#978365")
        )
    }
}
