package com.trilead.ssh2.signature;

import java.math.BigInteger;

/**
 * RSAPublicKey.
 * 
 * @author Christian Plattner, plattner@trilead.com
 * @version $Id: RSAPublicKey.java,v 1.1 2007/10/15 12:49:57 cplattne Exp $
 * @deprecated use {@link java.security.interfaces.RSAPublicKey}
 * @see java.security.interfaces.RSAPublicKey
 */
public class RSAPublicKey
{
	BigInteger e;
	BigInteger n;

	public RSAPublicKey(BigInteger e, BigInteger n)
	{
		this.e = e;
		this.n = n;
	}

	public BigInteger getE()
	{
		return e;
	}

	public BigInteger getN()
	{
		return n;
	}
}