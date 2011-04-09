package com.google.bitcoin.core;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import org.junit.Test;

public class UtilsTest {

	@Test
	public void testToNanoCoins() {

		// String version

		assertEquals(Utils.CENT, Utils.toNanoCoins("0.01"));
		assertEquals(Utils.CENT, Utils.toNanoCoins("1E-2"));
		assertEquals(Utils.COIN.add(Utils.CENT), Utils.toNanoCoins("1.01"));
		try {
			Utils.toNanoCoins("2E-20");
			fail("should not have accepted fractional nanocoins");
		} catch (ArithmeticException e) {
		}

		// int version
		assertEquals(Utils.CENT, Utils.toNanoCoins(0, 1));
		
		// TODO: should this really pass?
		assertEquals(Utils.COIN.subtract(Utils.CENT), Utils.toNanoCoins(1, -1));
		assertEquals(Utils.COIN.negate(), Utils.toNanoCoins(-1, 0));
		assertEquals(Utils.COIN.negate(), Utils.toNanoCoins("-1"));
	}

}
