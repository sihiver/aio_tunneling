package net.sourceforge.jsocks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Abstract class Proxy, base for classes Socks4Proxy and Socks5Proxy. Defines
 * methods for specifying default proxy, to be used by all classes of this
 * package.
 */

public abstract class Proxy {

	// Data members
	// protected InetRange directHosts = new InetRange();

	public static final int SOCKS_SUCCESS = 0;
	public static final int SOCKS_FAILURE = 1;
	public static final int SOCKS_BADCONNECT = 2;
	public static final int SOCKS_BADNETWORK = 3;

	public static final int SOCKS_HOST_UNREACHABLE = 4;
	public static final int SOCKS_CONNECTION_REFUSED = 5;

	public static final int SOCKS_TTL_EXPIRE = 6;

	public static final int SOCKS_CMD_NOT_SUPPORTED = 7;

	public static final int SOCKS_ADDR_NOT_SUPPORTED = 8;

	public static final int SOCKS_NO_PROXY = 1 << 16;

	public static final int SOCKS_PROXY_NO_CONNECT = 2 << 16;

	public static final int SOCKS_PROXY_IO_ERROR = 3 << 16;

	public static final int SOCKS_AUTH_NOT_SUPPORTED = 4 << 16;

	// Public instance methods
	// ========================

	public static final int SOCKS_AUTH_FAILURE = 5 << 16;

	public static final int SOCKS_JUST_ERROR = 6 << 16;

	public static final int SOCKS_DIRECT_FAILED = 7 << 16;

	// Public Static(Class) Methods
	// ==============================

	public static final int SOCKS_METHOD_NOTSUPPORTED = 8 << 16;

	public static final int SOCKS_CMD_CONNECT = 0x1;

	static final int SOCKS_CMD_BIND = 0x2;

	static final int SOCKS_CMD_UDP_ASSOCIATE = 0x3;

	/**
	 * Get current default proxy.
	 * 
	 * @return Current default proxy, or null if none is set.
	 */
	public static Proxy getDefaultProxy() {
		return defaultProxy;
	}


	/**
	 * Sets default proxy.
	 * 
	 * @param p
	 *            Proxy to use as default proxy.
	 */
	public static void setDefaultProxy(Proxy p) {
		defaultProxy = p;
	}

	protected InetAddress proxyIP = null;

	protected String proxyHost = null;

	protected int proxyPort;

	protected Socket proxySocket = null;

	protected InputStream in;

	protected OutputStream out;

	protected int version;

	protected Proxy chainProxy = null;

	// Protected static/class variables
	protected static Proxy defaultProxy = null;

	Proxy(InetAddress proxyIP, int proxyPort) {
		this.proxyIP = proxyIP;
		this.proxyPort = proxyPort;
	}

	Proxy(Proxy p) {
		this.proxyIP = p.proxyIP;
		this.proxyPort = p.proxyPort;
		this.version = p.version;
	}

	Proxy(Proxy chainProxy, InetAddress proxyIP, int proxyPort) {
		this.chainProxy = chainProxy;
		this.proxyIP = proxyIP;
		this.proxyPort = proxyPort;
	}

	// Private methods
	// ===============

	// Constants

	// Constructors
	// ====================
	Proxy(String proxyHost, int proxyPort) throws UnknownHostException {
		this.proxyHost = proxyHost;
		this.proxyIP = InetAddress.getByName(proxyHost);
		this.proxyPort = proxyPort;
	}
	protected ProxyMessage accept() throws IOException, SocksException {
		ProxyMessage msg;
		try {
			msg = formMessage(in);
		} catch (InterruptedIOException iioe) {
			throw iioe;
		} catch (IOException io_ex) {
			endSession();
			throw new SocksException(SOCKS_PROXY_IO_ERROR,
					"While Trying accept:" + io_ex);
		}
		return msg;
	}
	protected ProxyMessage bind(InetAddress ip, int port) throws SocksException {
		try {
			startSession();
			ProxyMessage request = formMessage(SOCKS_CMD_BIND, ip, port);
			return exchange(request);
		} catch (SocksException se) {
			endSession();
			throw se;
		}
	}
	protected ProxyMessage bind(String host, int port)
			throws UnknownHostException, SocksException {
		try {
			startSession();
			ProxyMessage request = formMessage(SOCKS_CMD_BIND, host, port);
			return exchange(request);
		} catch (SocksException se) {
			endSession();
			throw se;
		}
	}
	protected ProxyMessage connect(InetAddress ip, int port)
			throws SocksException {
		try {
			startSession();
			ProxyMessage request = formMessage(SOCKS_CMD_CONNECT, ip, port);
			return exchange(request);
		} catch (SocksException se) {
			endSession();
			throw se;
		}
	}
	protected ProxyMessage connect(String host, int port)
			throws UnknownHostException, SocksException {
		try {
			startSession();
			ProxyMessage request = formMessage(SOCKS_CMD_CONNECT, host, port);
			return exchange(request);
		} catch (SocksException se) {
			endSession();
			throw se;
		}
	}
	protected abstract Proxy copy();
	protected void endSession() {
		try {
			if (proxySocket != null)
				proxySocket.close();
			proxySocket = null;
		} catch (IOException io_ex) {
		}
	}
	/**
	 * Sends the request reads reply and returns it throws exception if
	 * something wrong with IO or the reply code is not zero
	 */
	protected ProxyMessage exchange(ProxyMessage request) throws SocksException {
		ProxyMessage reply;
		try {
			request.write(out);
			reply = formMessage(in);
		} catch (SocksException s_ex) {
			throw s_ex;
		} catch (IOException ioe) {
			throw (new SocksException(SOCKS_PROXY_IO_ERROR, "" + ioe));
		}
		return reply;
	}

	protected abstract ProxyMessage formMessage(InputStream in)
			throws SocksException, IOException;
	protected abstract ProxyMessage formMessage(int cmd, InetAddress ip,
			int port);
	protected abstract ProxyMessage formMessage(int cmd, String host, int port)
			throws UnknownHostException;
	/**
	 * Get the ip address of the proxy server host.
	 * 
	 * @return Proxy InetAddress.
	 */
	public InetAddress getInetAddress() {
		return proxyIP;
	}
	/**
	 * Get the port on which proxy server is running.
	 * 
	 * @return Proxy port.
	 */
	public int getPort() {
		return proxyPort;
	}
	/**
	 * Reads the reply from the SOCKS server
	 */
	protected ProxyMessage readMsg() throws SocksException, IOException {
		return formMessage(in);
	}

	/**
	 * Sends the request to SOCKS server
	 */
	protected void sendMsg(ProxyMessage msg) throws SocksException, IOException {
		msg.write(out);
	}
	protected void startSession() throws SocksException {
		try {
			proxySocket = new Socket(proxyIP, proxyPort);
			in = proxySocket.getInputStream();
			out = proxySocket.getOutputStream();
		} catch (SocksException se) {
			throw se;
		} catch (IOException io_ex) {
			throw new SocksException(SOCKS_PROXY_IO_ERROR, "" + io_ex);
		}
	}

	/**
	 * Get string representation of this proxy.
	 * 
	 * @returns string in the form:proxyHost:proxyPort \t Version versionNumber
	 */
	@Override
	public String toString() {
		return ("" + proxyIP.getHostName() + ":" + proxyPort + "\tVersion " + version);
	}
	protected ProxyMessage udpAssociate(InetAddress ip, int port)
			throws SocksException {
		try {
			startSession();
			ProxyMessage request = formMessage(SOCKS_CMD_UDP_ASSOCIATE, ip,
					port);
			if (request != null)
				return exchange(request);
		} catch (SocksException se) {
			endSession();
			throw se;
		}
		// Only get here if request was null
		endSession();
		throw new SocksException(SOCKS_METHOD_NOTSUPPORTED,
				"This version of proxy does not support UDP associate, use version 5");
	}
	protected ProxyMessage udpAssociate(String host, int port)
			throws UnknownHostException, SocksException {
		try {
			startSession();
			ProxyMessage request = formMessage(SOCKS_CMD_UDP_ASSOCIATE, host,
					port);
			if (request != null)
				return exchange(request);
		} catch (SocksException se) {
			endSession();
			throw se;
		}
		// Only get here if request was null
		endSession();
		throw new SocksException(SOCKS_METHOD_NOTSUPPORTED,
				"This version of proxy does not support UDP associate, use version 5");
	}

}
