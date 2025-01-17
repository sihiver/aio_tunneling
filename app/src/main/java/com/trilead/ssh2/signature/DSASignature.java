package com.trilead.ssh2.signature;

import java.math.BigInteger;

/**
 * DSASignature.
 * 
 * @author Christian Plattner, plattner@trilead.com
 * @version $Id: DSASignature.java,v 1.1 2007/10/15 12:49:57 cplattne Exp $
 * @deprecated signatures are now stored in ray byte[] form.
 */
@Deprecated
public class DSASignature
{
	private BigInteger r;
	private BigInteger s;

	public DSASignature(BigInteger r, BigInteger s)
	{
		this.r = r;
		this.s = s;
	}

	public BigInteger getR()
	{
		return r;
	}

	public BigInteger getS()
	{
		return s;
	}
}
