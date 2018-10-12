package com.cameron.ucfparking

import android.os.Parcel
import android.os.Parcelable

data class Garage(val name: String, var spacesAvailable: Int, val maxSpaces: Int) : Parcelable {

    val percentFull = Math.round((maxSpaces - spacesAvailable).toDouble() / maxSpaces * 100)

    val spacesFilled = maxSpaces - spacesAvailable

    constructor(parcel: Parcel) : this(parcel.readString(), parcel.readInt(), parcel.readInt())

    override fun toString(): String = "$name: $spacesAvailable/$maxSpaces"

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(spacesAvailable)
        parcel.writeInt(maxSpaces)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Garage> {
        override fun createFromParcel(parcel: Parcel): Garage = Garage(parcel)
        override fun newArray(size: Int): Array<Garage?> = arrayOfNulls(size)
    }
}