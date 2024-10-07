package com.sihiver.aiotunneling.ui.account

import android.os.Parcel
import android.os.Parcelable

data class Profile(
    val id: String,
    val profileName: String,
    val server: String,
    val port: Int,
    val username: String,
    val password: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(profileName)
        parcel.writeString(server)
        parcel.writeInt(port)
        parcel.writeString(username)
        parcel.writeString(password)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Profile> {
        override fun createFromParcel(parcel: Parcel): Profile = Profile(parcel)
        override fun newArray(size: Int): Array<Profile?> = arrayOfNulls(size)
    }
}