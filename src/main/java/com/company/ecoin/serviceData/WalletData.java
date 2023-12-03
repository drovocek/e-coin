package com.company.ecoin.serviceData;

import com.company.ecoin.model.Wallet;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.*;

import static com.company.ecoin.util.PropertiesUtils.getProperty;

public class WalletData {

    private Wallet wallet;
    private static final WalletData INSTANCE;

    static {
        INSTANCE = new WalletData();
    }

    public static WalletData getInstance(){
        return INSTANCE;
    }

    //This will load your wallet from the database.
    public void loadWallet() throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        Connection walletConnection = DriverManager.getConnection(getProperty("db.url.wallet"));
        Statement walletStatment = walletConnection.createStatement();
        ResultSet resultSet;
        resultSet = walletStatment.executeQuery(" SELECT * FROM WALLET ");
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        PublicKey pub2 = null;
        PrivateKey prv2 = null;
        while (resultSet.next()) {
            pub2 = keyFactory.generatePublic(
                    new X509EncodedKeySpec(resultSet.getBytes("PUBLIC_KEY")));
            prv2 = keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(resultSet.getBytes("PRIVATE_KEY")));
        }
        this.wallet = new Wallet(pub2, prv2);
    }

    public Wallet getWallet() {
        return wallet;
    }
}
