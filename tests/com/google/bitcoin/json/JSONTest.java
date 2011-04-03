package com.google.bitcoin.json;

import static junit.framework.Assert.assertEquals;

import java.io.IOException;

import org.json.JSONException;
import org.junit.Test;

import com.google.bitcoin.bouncycastle.util.Strings;
import com.google.bitcoin.bouncycastle.util.io.Streams;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ProtocolException;


public class JSONTest {

	@Test
	public void testFromJSON() throws IOException, JSONException, ProtocolException{
		String json = Strings.fromUTF8ByteArray(Streams.readAll(getClass().getResourceAsStream("000000000000c2626140d9efb9a554337e6704d5279899c41949a248f16d2d6d.json")));
		// remove comment line 
		json = json.substring(json.indexOf('{'));
		Block block = JSON.getBlockFromJSON(NetworkParameters.prodNet(), json);
		assertEquals("000000000000c2626140d9efb9a554337e6704d5279899c41949a248f16d2d6d",block.getHashAsString());
	}
	
}
