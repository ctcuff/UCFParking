package com.cameron.ucfparking

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_garage.view.*
import java.util.*

class GarageViewAdapter(private var garages: ArrayList<Garage>) : RecyclerView.Adapter<GarageViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_garage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val garage = garages[position]

        with(holder.itemView) {
            tvGarageName.text = garage.name
            tvSpacesAvailable.text = context.getString(
                    R.string.garage_spaces_available,
                    garage.spacesAvailable, garage.maxSpaces
            )
            tvPercentFull.text =
                    if (garage.percentFull < 0) {
                        context.getString(R.string.garage_empty_percentage)
                    } else {
                        context.getString(R.string.garage_percent_full, garage.percentFull)
                    }
            garageProgressBar.max = garage.maxSpaces.toFloat()
            garageProgressBar.progress = garage.spacesFilled.toFloat()
            garageProgressBar.progressColor = getProgressColor(context, garage.percentFull)
        }
    }

    override fun getItemCount(): Int = garages.size

    fun updateList(newData: ArrayList<Garage>) {
        garages = newData
        notifyDataSetChanged()
    }

    private fun getProgressColor(context: Context, percentage: Long): Int {
        return when (percentage) {
            in 0..9 -> ContextCompat.getColor(context, R.color.garageColorOne)
            in 10..19 -> ContextCompat.getColor(context, R.color.garageColorTwo)
            in 20..29 -> ContextCompat.getColor(context, R.color.garageColorThree)
            in 30..39 -> ContextCompat.getColor(context, R.color.garageColorFour)
            in 40..49 -> ContextCompat.getColor(context, R.color.garageColorFive)
            in 50..59 -> ContextCompat.getColor(context, R.color.garageColorSix)
            in 60..69 -> ContextCompat.getColor(context, R.color.garageColorSeven)
            in 70..79 -> ContextCompat.getColor(context, R.color.garageColorEight)
            in 80..89 -> ContextCompat.getColor(context, R.color.garageColorNine)
            in 90..100 -> ContextCompat.getColor(context, R.color.garageColorTen)
            else -> 0
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}