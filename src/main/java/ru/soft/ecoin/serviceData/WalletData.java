package ru.soft.ecoin.serviceData;

import lombok.Getter;
import lombok.SneakyThrows;
import ru.soft.ecoin.model.Wallet;
import ru.soft.ecoin.util.PropertiesUtils;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static ru.soft.ecoin.util.KeyUtil.generateDsaPrivateKey;
import static ru.soft.ecoin.util.KeyUtil.generateDsaPublicKey;

public class WalletData {

    private static final String WALLET_GET_ALL = "SELECT * FROM WALLET";

    @Getter
    private Wallet wallet;
    private static final WalletData INSTANCE;

    static {
        INSTANCE = new WalletData();
    }

    public static WalletData getInstance() {
        return INSTANCE;
    }

    //This will load your wallet from the database.
    @SneakyThrows
    public void loadWallet() {
        try (Connection walletConnection = DriverManager.getConnection(PropertiesUtils.getProperty("db.url.wallet"));
             Statement walletStatement = walletConnection.createStatement()) {
            ResultSet resultSet;
            resultSet = walletStatement.executeQuery(WALLET_GET_ALL);
            PublicKey pub2 = null;
            PrivateKey prv2 = null;
            while (resultSet.next()) {
                pub2 = generateDsaPublicKey(resultSet.getBytes("PUBLIC_KEY"));
                prv2 = generateDsaPrivateKey(resultSet.getBytes("PRIVATE_KEY"));
            }
            this.wallet = new Wallet(pub2, prv2);
        }
    }
}
