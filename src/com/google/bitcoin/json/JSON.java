package com.google.bitcoin.json;

import static com.google.bitcoin.core.Utils.uint32ToByteStreamLE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.VarInt;

/**
 * Helper class to transform between Bitcoin data structures and JSON
 * representations
 * 
 * @author Thilo Planz
 * 
 */

public class JSON {

	public static Block getBlockFromJSON(NetworkParameters params, String json)
			throws JSONException, ProtocolException {
		return getBlockFromJSON(params, new JSONObject(json));
	}

	public static Block getBlockFromJSON(NetworkParameters params,
			JSONObject json) throws ProtocolException, JSONException {
		byte[] bytes = getBlockBytesFromJSON(json);
		return new Block(params, bytes);
	}

	public static byte[] getBlockBytesFromJSON(JSONObject json)
			throws JSONException {

		ByteArrayOutputStream stream = new ByteArrayOutputStream(1000);

		try {
			Utils.uint32ToByteStreamLE(json.getLong("ver"), stream);
			stream.write(Utils.reverseBytes(Utils.hexStringTo32Bytes(json
					.getString("prev_block"))));
			stream.write(Utils.reverseBytes(Utils.hexStringTo32Bytes(json
					.getString("mrkl_root"))));
			Utils.uint32ToByteStreamLE(json.getLong("time"), stream);
			Utils.uint32ToByteStreamLE(json.getLong("bits"), stream);
			Utils.uint32ToByteStreamLE(json.getLong("nonce"), stream);

			int n = json.getInt("n_tx");
			if (n > 0) {
				JSONArray txs = json.getJSONArray("tx");
				stream.write(new VarInt(n).encode());
				for (int i = 0; i < n; i++) {
					writeTransaction(txs.getJSONObject(i), stream);
				}

			}

		} catch (IOException e) {
			throw new RuntimeException(e); // Cannot happen, writing to memory
			// stream.
		}

		return stream.toByteArray();
	}

	private final static Charset ascii = Charset.forName("ASCII");

	private static void writeTransaction(JSONObject json, OutputStream stream)
			throws IOException, JSONException {
		Utils.uint32ToByteStreamLE(json.getLong("ver"), stream);
		stream.write(new VarInt(json.getLong("vin_sz")).encode());
		{
			JSONArray in = json.getJSONArray("in");
			for (int i = 0; i < in.length(); i++) {
				JSONObject tis = in.getJSONObject(i);
				JSONObject outpoint = tis.getJSONObject("prev_out");
				stream.write(Utils.reverseBytes(Utils
						.hexStringTo32Bytes(outpoint.getString("hash"))));
				Utils.uint32ToByteStreamLE(outpoint.getLong("n"), stream);
				if (tis.has("scriptSig")) {
					String scriptSig = tis.getString("scriptSig");
					byte[] scriptBytes = scriptSig.getBytes(ascii);
					stream.write(new VarInt(scriptBytes.length).encode());
					stream.write(scriptBytes);
				} else {
					stream.write(new VarInt(0).encode());
				}
				Utils.uint32ToByteStreamLE(0xFFFFFFFFL, stream);
			}
		}
		stream.write(new VarInt(json.getLong("vout_sz")).encode());
		{
			JSONArray out = json.getJSONArray("out");
			for (int i = 0; i < out.length(); i++) {
				JSONObject tout = out.getJSONObject(i);
				Utils.uint64ToByteStreamLE(new BigDecimal(tout
						.getString("value")).movePointRight(9)
						.toBigIntegerExact(), stream);
				if (tout.has("scriptPubKey")) {
					String scriptSig = tout.getString("scriptPubKey");
					byte[] scriptBytes = scriptSig.getBytes(ascii);
					stream.write(new VarInt(scriptBytes.length).encode());
					stream.write(scriptBytes);
				} else {
					stream.write(new VarInt(0).encode());
				}
			}
		}
		uint32ToByteStreamLE(json.getLong("lock_time"), stream);
	}

	public static JSONObject toJSON(Block block) {
		JSONObject r = new JSONObject();
		try {
			r.put("hash", block.getHashAsString());
			r.put("ver", block.getVersion());
			r.put("prev_block", Utils
					.bytesToHexString(block.getPrevBlockHash()));
			r.put("mrkl_root", block.getMerkleRootAsString());
			r.put("time", block.getTime());
			r.put("nonce", block.getNonce());
			r.put("bits", block.getDifficultyTarget());
			List<Transaction> tx = block.getTransactions();
			r.put("n_tx", tx.size());
			JSONArray txs = new JSONArray();
			for (Transaction t : tx) {
				txs.put(toJSON(t));
			}
			r.put("tx", txs);
			r.put("size", block.getSize());
			r.put("mrkl_tree", new JSONArray(block.getMerkleTreeAsStrings()));
		} catch (JSONException e) {
			// should not happen
			throw new RuntimeException(
					"failed to create JSON encoding for block "
							+ block.getHashAsString(), e);
		}
		return r;
	}

	public static JSONObject toJSON(Transaction t) {
		JSONObject r = new JSONObject();
		try {
			r.put("hash", t.getHashAsString());
			r.put("ver", t.getVersion());
			r.put("lock_time", t.getLockTime());
			r.put("size", t.getSize());
			List<TransactionInput> in = t.getInputs();
			r.put("vin_sz", in.size());
			JSONArray jin = new JSONArray();
			for (TransactionInput i: in){
				JSONObject o = new JSONObject();
				JSONObject po = new JSONObject();
				po.put("hash", i.getOutPoint().getHashAsString());
				po.put("n", i.getOutPoint().getIndex());
				o.put("prev_out", po);
				if (i.isCoinBase()){
					o.put("coinbase", i.getScriptSig());
				}else{
					o.put("scriptSig", i.getScriptSig());
				}
				jin.put(o);
			}
			r.put("in", jin);
			List<TransactionOutput> out = t.getOutputs();
			JSONArray jout = new JSONArray();
			for (TransactionOutput o: out){
				JSONObject j = new JSONObject();
				j.put("value", new BigDecimal(o.getValue()).movePointLeft(9).toPlainString());
				j.put("scriptPubKey", o.getScriptPubKey().toString());
				jout.put(j);
			}
			r.put("out", jout);
			r.put("vout_sz", out.size());
		} catch (JSONException e) {
			// should not happen
			throw new RuntimeException(
					"failed to create JSON encoding for transaction "
							+ t.getHashAsString(), e);
		} catch (ScriptException e) {
			throw new RuntimeException(
					"failed to create JSON encoding for transaction "
							+ t.getHashAsString(), e);
		}
		return r;
	}
}
