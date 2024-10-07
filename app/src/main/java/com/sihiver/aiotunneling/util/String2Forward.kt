package com.sihiver.aiotunneling.util

class String2Forward(fwd: String) {
    /**
     * @return the remotehost
     */
    var remotehost: String? = null
    val direction: String

    /**
     * @return the localport
     */
    var localport: Int = 0

    /**
     * @return the remoteport
     */
    var remoteport: Int = 0

    inner class ParseException : Exception {
        constructor() : super()
        constructor(detailMessage: String?) : super(detailMessage)
    }

    companion object {
        private const val serialVersionUID = 1L
    }

    /**
     * @param fwd
     * a string of the format XLocalPort=RemoteHost:RemotePort where X is either L, or R indicating Local, Remote, or Dynamic forwards.
     * (e.g. L7000=mail:143) similar to the PuTTY port forward
     * format. NOTE: currently all forwards are created as local forwards.
     */
    init {
        var fwd = fwd
        // TODO implement R forwards
        //
        direction = fwd.substring(0, 1)
        if (!("L" == direction || ("R" == direction))) {
            throw ParseException("illegal Remote/Local specifier")
        }
        fwd = fwd.replace("L", "").replace("R", "")
        val eq = fwd.indexOf('=')
        val cl = fwd.indexOf(':')

        if (eq == -1) {
            throw ParseException("missing =")
        }
        if (cl == -1) {
            throw ParseException("missing :")
        }
        if (eq < 1) {
            throw ParseException("missing local port")
        }
        if (cl <= eq + 1) {
            throw ParseException("missing remote host")
        }
        if (cl + 1 >= fwd.length) {
            throw ParseException("missing remote port")
        }

        try {
            localport = fwd.substring(0, eq).toInt()
            remotehost = fwd.substring(eq + 1, cl)
            remoteport = fwd.substring(cl + 1).toInt()
        } catch (e: NumberFormatException) {
            throw ParseException("one of the ports is non numeric")
        }
    }

    override fun toString(): String {
        return "$direction$localport=$remotehost:$remoteport"
    }
}