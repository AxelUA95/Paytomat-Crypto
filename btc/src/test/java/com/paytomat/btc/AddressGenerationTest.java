package com.paytomat.btc;

import com.google.gson.Gson;
import com.paytomat.btc.network.prod.BTCProdNetParams;
import com.paytomat.btc.network.prod.ZENProdNetParams;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * created by Alex Ivanov on 2019-02-12.
 */
public class AddressGenerationTest {

    @Test
    public void testBTCGen() throws IOException {
        String fileData = FileUtil.readFile("set.txt", getClass());
        Gson gson = new Gson();
        String[] data = fileData.split("\n");
        for (int i = 0; i < data.length; i++) {
            String json = data[i];
            if (json.equals("{") || json.equals("}")) continue;
            AddressModel testModel = gson.fromJson(json, AddressModel.class);
            System.out.println((i + 1) + " of " + data.length);
            PrivateKey pk = new PrivateKey(testModel.privateKey);
            KeyPair pair = new KeyPair(pk);
            Assert.assertEquals(testModel.publicKey, pair.publicKey.toString());
            Assert.assertEquals(testModel.address, pair.getAddress(BTCProdNetParams.getParams(), false).toString());
        }
    }

    @Test
    public void testBTCGenSegwit() throws IOException {
        String fileData = FileUtil.readFile("set_segwit.txt", getClass());
        Gson gson = new Gson();
        String[] data = fileData.split("\n");
        for (int i = 0; i < data.length; i++) {
            String json = data[i];
            if (json.equals("{") || json.equals("}")) continue;
            AddressModel testModel = gson.fromJson(json, AddressModel.class);
            System.out.println((i + 1) + " of " + data.length);
            PrivateKey pk = new PrivateKey(testModel.privateKey);
            KeyPair pair = new KeyPair(pk);
            Assert.assertEquals(testModel.publicKey, pair.publicKey.toString());
            Assert.assertEquals(testModel.address, pair.getAddress(BTCProdNetParams.getParams(), true).toString());
        }
    }

    @Test
    public void testZenAddressParse() {
        String addressBtcStr = "3Mr2oyAu8826xvngbxqC3QhrGHA7oT88Lp";
        Address addressBtc = Address.fromString(addressBtcStr, BTCProdNetParams.getParams());
        Assert.assertEquals(addressBtcStr, addressBtc.toString());
        String addressZenStr = "zngzNC9QGPz8n8pduve8BfN2kDfsgVxwjYP";
        Address addressZen = Address.fromString(addressZenStr, ZENProdNetParams.getParams());
        Assert.assertEquals(addressZenStr, addressZen.toString());
    }
}
