package ru.soft.ecoin;

import ru.soft.ecoin.model.Block;
import ru.soft.ecoin.model.Transaction;
import ru.soft.ecoin.model.Wallet;
import ru.soft.ecoin.serviceData.BlockchainData;
import ru.soft.ecoin.serviceData.WalletData;
import ru.soft.ecoin.threads.MiningThread;
import ru.soft.ecoin.threads.PeerClient;
import ru.soft.ecoin.threads.PeerServer;
import ru.soft.ecoin.threads.UI;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import ru.soft.ecoin.util.PropertiesUtils;

import java.security.*;
import java.sql.*;
import java.time.LocalDateTime;

@Slf4j
public class ECoin extends Application {

    private static final String WALLET_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS WALLET ( " +
            " PRIVATE_KEY BLOB NOT NULL UNIQUE, " +
            " PUBLIC_KEY BLOB NOT NULL UNIQUE, " +
            " PRIMARY KEY (PRIVATE_KEY, PUBLIC_KEY)" +
            ") ";
    private static final String WALLET_INSERT = "INSERT INTO WALLET(PRIVATE_KEY, PUBLIC_KEY) VALUES (?,?)";
    private static final String WALLET_GET_ALL = "SELECT * FROM WALLET";
    private static final String BLOCKCHAIN_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS BLOCKCHAIN ( " +
            " ID INTEGER NOT NULL UNIQUE, " +
            " PREVIOUS_HASH BLOB UNIQUE, " +
            " CURRENT_HASH BLOB UNIQUE, " +
            " LEDGER_ID INTEGER NOT NULL UNIQUE, " +
            " CREATED_ON  TEXT, " +
            " CREATED_BY  BLOB, " +
            " MINING_POINTS  TEXT, " +
            " LUCK  NUMERIC, " +
            " PRIMARY KEY( ID AUTOINCREMENT) " +
            ")";
    private static final String BLOCKCHAIN_GET_ALL = "SELECT * FROM BLOCKCHAIN";
    private static final String BLOCKCHAIN_INSERT = "INSERT INTO BLOCKCHAIN(PREVIOUS_HASH, CURRENT_HASH , LEDGER_ID," +
            " CREATED_ON, CREATED_BY,MINING_POINTS,LUCK ) " +
            " VALUES (?,?,?,?,?,?,?)";
    private static final String TRANSACTIONS_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS TRANSACTIONS ( " +
            " ID INTEGER NOT NULL UNIQUE, " +
            " \"FROM\" BLOB, " +
            " \"TO\" BLOB, " +
            " LEDGER_ID INTEGER, " +
            " VALUE INTEGER, " +
            " SIGNATURE BLOB UNIQUE, " +
            " CREATED_ON TEXT, " +
            " PRIMARY KEY(ID AUTOINCREMENT) " +
            ")";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        new UI().start(primaryStage);
        new PeerClient().start();
        new PeerServer(6000).start();
        new MiningThread().start();
    }

    @Override
    public void init() {
        try {
            //This creates your wallet if there is none and gives you a KeyPair.
            //We will create it in separate db for better security and ease of portability.
            Connection walletConnection = DriverManager
                    .getConnection(PropertiesUtils.getProperty("db.url.wallet"));
            Statement walletStatement = walletConnection.createStatement();
            walletStatement.executeUpdate(WALLET_CREATE_TABLE);
            ResultSet resultSet = walletStatement.executeQuery(WALLET_GET_ALL);
            if (!resultSet.next()) {
                Wallet newWallet = new Wallet();
                byte[] pubBlob = newWallet.getPublicKey().getEncoded();
                byte[] prvBlob = newWallet.getPrivateKey().getEncoded();
                PreparedStatement pstmt = walletConnection.prepareStatement(WALLET_INSERT);
                pstmt.setBytes(1, prvBlob);
                pstmt.setBytes(2, pubBlob);
                pstmt.executeUpdate();
            }
            resultSet.close();
            walletStatement.close();
            walletConnection.close();
            WalletData.getInstance().loadWallet();

//          This will create the db tables with columns for the Blockchain.
            Connection blockchainConnection = DriverManager
                    .getConnection(PropertiesUtils.getProperty("db.url.blockchain"));
            Statement blockchainStmt = blockchainConnection.createStatement();
            blockchainStmt.executeUpdate(BLOCKCHAIN_CREATE_TABLE);
            ResultSet resultSetBlockchain = blockchainStmt.executeQuery(BLOCKCHAIN_GET_ALL);
            Transaction initBlockRewardTransaction = null;
            if (!resultSetBlockchain.next()) {
                Block firstBlock = new Block();
                firstBlock.setMinedBy(WalletData.getInstance().getWallet().getPublicKey().getEncoded());
                firstBlock.setTimeStamp(LocalDateTime.now().toString());
                //helper class.
                Signature signing = Signature.getInstance("SHA256withDSA");
                signing.initSign(WalletData.getInstance().getWallet().getPrivateKey());
                signing.update(firstBlock.toString().getBytes());
                firstBlock.setCurrHash(signing.sign());
                PreparedStatement pstmt = blockchainConnection.prepareStatement(BLOCKCHAIN_INSERT);
                pstmt.setBytes(1, firstBlock.getPrevHash());
                pstmt.setBytes(2, firstBlock.getCurrHash());
                pstmt.setInt(3, firstBlock.getLedgerId());
                pstmt.setString(4, firstBlock.getTimeStamp());
                pstmt.setBytes(5, WalletData.getInstance().getWallet().getPublicKey().getEncoded());
                pstmt.setInt(6, firstBlock.getMiningPoints());
                pstmt.setDouble(7, firstBlock.getLuck());
                pstmt.executeUpdate();
                Signature transSignature = Signature.getInstance("SHA256withDSA");
                initBlockRewardTransaction = new Transaction(WalletData.getInstance().getWallet(), WalletData.getInstance().getWallet().getPublicKey().getEncoded(), 100, 1, transSignature);
            }
            resultSetBlockchain.close();

            blockchainStmt.executeUpdate(TRANSACTIONS_CREATE_TABLE);
            if (initBlockRewardTransaction != null) {
                BlockchainData.getInstance().addTransaction(initBlockRewardTransaction, true);
                BlockchainData.getInstance().addTransactionState(initBlockRewardTransaction);
            }
            blockchainStmt.close();
            blockchainConnection.close();
        } catch (SQLException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            log.error("db failed: {}", e.getMessage());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        BlockchainData.getInstance().loadBlockChain();
    }
}

