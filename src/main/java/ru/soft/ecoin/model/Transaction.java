package ru.soft.ecoin.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Base64;

import static ru.soft.ecoin.util.KeyUtil.generateDsaKey;
import static java.time.LocalDateTime.now;

@Getter
@Setter
public class Transaction implements Serializable {

    private byte[] from;
    private byte[] to;
    private Integer value;
    private String timestamp;
    private byte[] signature;
    private Integer ledgerId;
    //ui view
    private String toFX;
    private String fromFX;
    private String signatureFX;

    //Constructor for loading with existing signature
    public Transaction(byte[] from, byte[] to, Integer value, byte[] signature, Integer ledgerId,
                       String timeStamp) {
        this.from = from;
        this.to = to;
        this.value = value;
        this.signature = signature;
        this.ledgerId = ledgerId;
        this.timestamp = timeStamp;

        //ui view
        Base64.Encoder encoder = Base64.getEncoder();
        this.toFX = encoder.encodeToString(to);
        this.fromFX = encoder.encodeToString(from);
        this.signatureFX = encoder.encodeToString(signature);
    }

    //Constructor for creating a new transaction and signing it.
    public Transaction(Wallet fromWallet, byte[] toAddress, Integer value, Integer ledgerId,
                       Signature signing) throws InvalidKeyException, SignatureException {
        byte[] encoded = fromWallet.getPublicKey().getEncoded();
        this.from = encoded;
        this.to = toAddress;
        this.value = value;
        this.ledgerId = ledgerId;
        this.timestamp = now().toString();

        signing.initSign(fromWallet.getPrivateKey());
        String sr = this.toString();
        signing.update(sr.getBytes());
        this.signature = signing.sign();

        //ui view
        Base64.Encoder encoder = Base64.getEncoder();
        this.toFX = encoder.encodeToString(toAddress);
        this.fromFX = encoder.encodeToString(encoded);
        this.signatureFX = encoder.encodeToString(this.signature);
    }

    public Boolean isVerified(Signature signing)
            throws InvalidKeyException, SignatureException {
        signing.initVerify(generateDsaKey(this.getFrom()));
        signing.update(this.toString().getBytes());
        return signing.verify(this.signature);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "from=" + Arrays.toString(from) +
                ", to=" + Arrays.toString(to) +
                ", value=" + value +
                ", timeStamp= " + timestamp +
                ", ledgerId=" + ledgerId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction that = (Transaction) o;
        return Arrays.equals(getSignature(), that.getSignature());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getSignature());
    }

}
