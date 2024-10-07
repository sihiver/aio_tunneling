package com.sihiver.aiotunneling.util

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.UIKeyboardInteractive
import com.jcraft.jsch.UserInfo
import com.sihiver.aiotunneling.util.String2Forward.ParseException
import java.util.LinkedList

@SuppressLint("ParcelCreator")
class ConnectionInfo : UserInfo, Parcelable, UIKeyboardInteractive {
    var host: String? = null
        private set
    var user: String? = null
        private set
    private var passphrase: String? = null
    private var password: String? = null
    var keypath: String? = null
    var port: Int = 22 // Pastikan port default adalah 22
        private set
    var dynamic_port: Int = 1080 // Set default dynamic_port ke 1080
        private set
    var compression: Boolean = true
        private set
    var remote_accept: Boolean = false
        private set
    var local_accept: Boolean = false
        private set
    var show_notifications: Boolean = true
        private set
    private val forwards: MutableList<Forward?> = LinkedList()

    inner class InvalidException : Exception {
        constructor() : super()
        constructor(detailMessage: String?) : super(detailMessage)
    }

    private class Forward() : Parcelable {
        private var localport: Int = 0
        private var remoteport: Int = 0
        private var remotehost: String? = null
        private var direction: String? = null
        private var local_accept: Boolean? = null
        private var remote_accept: Boolean? = null

        constructor(parcel: Parcel) : this() {
            localport = parcel.readInt()
            remoteport = parcel.readInt()
            remotehost = parcel.readString()
            direction = parcel.readString()
            local_accept = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
            remote_accept = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        }

        constructor(localport: Int, remoteport: Int, remotehost: String?) : this() {
            this.localport = localport
            this.remoteport = remoteport
            this.remotehost = remotehost
        }

        constructor(fwd: String?, remote_accept: Boolean?, local_accept: Boolean?) : this() {
            val s2f = fwd?.let { String2Forward(it) }!!

            localport = s2f.localport
            remotehost = s2f.remotehost
            remoteport = s2f.remoteport
            direction = s2f.direction
            this.remote_accept = remote_accept
            this.local_accept = local_accept
        }

        @Throws(JSchException::class)
        fun setPortForwardingL(s: Session) {
            if ("L" == direction) {
                s.setPortForwardingL(
                    if (local_accept!!) "0.0.0.0" else "127.0.0.1",
                    localport,
                    remotehost,
                    remoteport
                )
            } else {
                s.setPortForwardingR(
                    if (remote_accept!!) "*" else null,
                    localport,
                    remotehost,
                    remoteport
                )
            }
        }

        @Throws(JSchException::class)
        fun delPortForwardingL(s: Session) {
            if ("L" == direction) {
                s.delPortForwardingL(localport)
            } else {
                s.delPortForwardingR(localport)
            }
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(localport)
            dest.writeInt(remoteport)
            dest.writeString(remotehost)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Forward> {
            override fun createFromParcel(parcel: Parcel): Forward {
                return Forward(parcel)
            }

            override fun newArray(size: Int): Array<Forward?> {
                return arrayOfNulls(size)
            }
        }
    }

    @Throws(InvalidException::class)
    fun Validate() {
        // Validation logic
    }

    fun AddForward(localport: Int, remoteport: Int, remotehost: String?) {
        // Add forward logic
    }

    @Throws(InvalidException::class)
    fun AddForwards(fwds: String) {
        // Add forwards logic
    }

    @Throws(JSchException::class)
    fun setPortForwardingL(s: Session) {
        // Set port forwarding logic
    }

    @Throws(JSchException::class)
    fun delPortForwardingL(s: Session) {
        // Delete port forwarding logic
    }

    val portForwards: String
        get() = forwards.joinToString(",") { it.toString() }

    override fun getPassphrase(): String {
        return passphrase ?: ""
    }

    override fun getPassword(): String {
        return password ?: ""
    }

    override fun promptPassphrase(message: String): Boolean {
        return true
    }

    override fun promptPassword(message: String): Boolean {
        return true
    }

    override fun promptYesNo(message: String): Boolean {
        return true
    }

    override fun showMessage(message: String) {
        // Show message logic
    }

    override fun promptKeyboardInteractive(
        destination: String,
        name: String,
        instruction: String,
        prompt: Array<String>,
        echo: BooleanArray
    ): Array<String> {
        return arrayOf()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(host)
        dest.writeString(user)
        dest.writeString(passphrase)
        dest.writeString(password)
        dest.writeString(keypath)
        dest.writeInt(port)
        dest.writeInt(dynamic_port)
        dest.writeByte(if (compression) 1 else 0)
        dest.writeByte(if (remote_accept) 1 else 0)
        dest.writeByte(if (local_accept) 1 else 0)
        dest.writeByte(if (show_notifications) 1 else 0)
        dest.writeTypedList(forwards)
    }

    private constructor(parcel: Parcel) {
        host = parcel.readString()
        user = parcel.readString()
        passphrase = parcel.readString()
        password = parcel.readString()
        keypath = parcel.readString()
        port = parcel.readInt()
        dynamic_port = parcel.readInt()
        compression = parcel.readByte() != 0.toByte()
        remote_accept = parcel.readByte() != 0.toByte()
        local_accept = parcel.readByte() != 0.toByte()
        show_notifications = parcel.readByte() != 0.toByte()
        parcel.readTypedList(forwards, Forward.CREATOR)
    }

    override fun describeContents(): Int {
        return 0
    }

    // Tambahkan setter untuk dynamic_port jika diperlukan
    fun setDynamicPort(port: Int) {
        this.dynamic_port = port
    }

    companion object {
        private const val serialVersionUID = -5040005532652987428L

        @JvmField
        val CREATOR: Parcelable.Creator<ConnectionInfo> = object : Parcelable.Creator<ConnectionInfo> {
            override fun createFromParcel(parcel: Parcel): ConnectionInfo {
                return ConnectionInfo(parcel)
            }

            override fun newArray(size: Int): Array<ConnectionInfo?> {
                return arrayOfNulls(size)
            }
        }
    }
}