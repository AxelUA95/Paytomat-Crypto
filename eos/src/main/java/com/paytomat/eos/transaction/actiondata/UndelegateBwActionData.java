package com.paytomat.eos.transaction.actiondata;

import com.paytomat.eos.transaction.EosAsset;
import com.paytomat.core.util.ByteSerializer;

import static com.paytomat.eos.Eos.encodeName;

/**
 * created by Alex Ivanov on 2019-02-12.
 */
public class UndelegateBwActionData extends ActionData {

    private final String from;
    private final String receiver;
    private final EosAsset stakeNetQuantity;
    private final EosAsset stakeCpuQuantity;

    public UndelegateBwActionData(String from, String receiver, EosAsset stakeNetQuantity, EosAsset stakeCpuQuantity) {
        this.from = from;
        this.receiver = receiver;
        this.stakeNetQuantity = stakeNetQuantity;
        this.stakeCpuQuantity = stakeCpuQuantity;
    }

    @Override
    public String getAuthorization() {
        return receiver;
    }

    @Override
    public byte[] serialize() {
        return new ByteSerializer()
                .write(encodeName(from))
                .write(encodeName(receiver))
                .write(stakeNetQuantity.serialize())
                .write(stakeCpuQuantity.serialize())
                .serialize();
    }
}
