package ru.soft.ecoin.serviceData;

import ru.soft.ecoin.model.Block;
import ru.soft.ecoin.model.Transaction;
import ru.soft.ecoin.model.Wallet;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;
import ru.soft.ecoin.util.KeyUtil;
import ru.soft.ecoin.util.PropertiesUtils;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

import static java.lang.String.format;

public class BlockchainData {

    //helper class.
    private final Signature signing;
    private final ObservableList<Transaction> newBlockTransactionsFX;
    private final ObservableList<Transaction> newBlockTransactions;
    @Setter
    @Getter
    private LinkedList<Block> currentBlockChain;
    private Block latestBlock;
    @Setter
    @Getter
    private boolean exit = false;
    @Setter
    @Getter
    private int miningPoints;
    private static final int TIMEOUT_INTERVAL = 65;
    private static final int MINING_INTERVAL = 60;

    //singleton class
    private static final BlockchainData INSTANCE;

    static {
        try {
            INSTANCE = new BlockchainData();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static BlockchainData getInstance() {
        return INSTANCE;
    }

    private BlockchainData() throws NoSuchAlgorithmException {
        signing = Signature.getInstance("SHA256withDSA");
        newBlockTransactions = FXCollections.observableArrayList();
        newBlockTransactionsFX = FXCollections.observableArrayList();
        currentBlockChain = new LinkedList<>();
    }

    Comparator<Transaction> transactionComparator = Comparator.comparing(Transaction::getTimestamp);

    public ObservableList<Transaction> getTransactionLedgerFX() {
        newBlockTransactionsFX.clear();
        newBlockTransactions.sort(transactionComparator);
        newBlockTransactionsFX.addAll(newBlockTransactions);
        return FXCollections.observableArrayList(newBlockTransactionsFX);
    }

    public String getWalletBallanceFX() {
        return getBalance(currentBlockChain, newBlockTransactions,
                WalletData.getInstance().getWallet().getPublicKey()).toString();
    }

    private Integer getBalance(LinkedList<Block> blockChain,
                               ObservableList<Transaction> currentLedger, PublicKey walletAddress) {
        Integer balance = 0;
        for (Block block : blockChain) {
            for (Transaction transaction : block.getTransactionLedger()) {
                if (Arrays.equals(transaction.getFrom(), walletAddress.getEncoded())) {
                    balance -= transaction.getValue();
                }
                if (Arrays.equals(transaction.getTo(), walletAddress.getEncoded())) {
                    balance += transaction.getValue();
                }
            }
        }
        for (Transaction transaction : currentLedger) {
            if (Arrays.equals(transaction.getFrom(), walletAddress.getEncoded())) {
                balance -= transaction.getValue();
            }
        }
        return balance;
    }

    private void verifyBlockChain(LinkedList<Block> currentBlockChain) throws GeneralSecurityException {
        for (Block block : currentBlockChain) {
            if (!block.isVerified(signing)) {
                throw new GeneralSecurityException(format("Block validation failed: %s", block));
            }
            ArrayList<Transaction> transactions = block.getTransactionLedger();
            for (Transaction transaction : transactions) {
                if (!transaction.isVerified(signing)) {
                    throw new GeneralSecurityException("Transaction validation failed");
                }
            }
        }
    }

    public void addTransactionState(Transaction transaction) {
        newBlockTransactions.add(transaction);
        newBlockTransactions.sort(transactionComparator);
    }

    public void addTransaction(Transaction transaction, boolean blockReward) throws GeneralSecurityException {
        try {
            if (getBalance(currentBlockChain, newBlockTransactions,
                    KeyUtil.generateDsaPublicKey(transaction.getFrom())) < transaction.getValue() && !blockReward) {
                throw new GeneralSecurityException("Not enough funds by sender to record transaction");
            } else {
                Connection connection = DriverManager.getConnection(PropertiesUtils.getProperty("db.url.blockchain"));

                PreparedStatement pstmt;
                pstmt = connection.prepareStatement("INSERT INTO TRANSACTIONS" +
                        "(\"FROM\", \"TO\", LEDGER_ID, VALUE, SIGNATURE, CREATED_ON) " +
                        " VALUES (?,?,?,?,?,?) ");
                pstmt.setBytes(1, transaction.getFrom());
                pstmt.setBytes(2, transaction.getTo());
                pstmt.setInt(3, transaction.getLedgerId());
                pstmt.setInt(4, transaction.getValue());
                pstmt.setBytes(5, transaction.getSignature());
                pstmt.setString(6, transaction.getTimestamp());
                pstmt.executeUpdate();

                pstmt.close();
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void loadBlockChain() {
        try {
            Connection connection = DriverManager.getConnection(PropertiesUtils.getProperty("db.url.blockchain"));
            Statement stmt = connection.createStatement();
            ResultSet resultSet = stmt.executeQuery(" SELECT * FROM BLOCKCHAIN ");
            while (resultSet.next()) {
                this.currentBlockChain.add(new Block(
                        resultSet.getBytes("PREVIOUS_HASH"),
                        resultSet.getBytes("CURRENT_HASH"),
                        resultSet.getString("CREATED_ON"),
                        resultSet.getBytes("CREATED_BY"),
                        resultSet.getInt("LEDGER_ID"),
                        resultSet.getInt("MINING_POINTS"),
                        resultSet.getDouble("LUCK"),
                        loadTransactionLedger(resultSet.getInt("LEDGER_ID"))
                ));
            }

            latestBlock = currentBlockChain.getLast();
            Transaction transaction = new Transaction(new Wallet(),
                    WalletData.getInstance().getWallet().getPublicKey().getEncoded(),
                    100, latestBlock.getLedgerId() + 1, signing);
            newBlockTransactions.clear();
            newBlockTransactions.add(transaction);
            verifyBlockChain(currentBlockChain);
            resultSet.close();
            stmt.close();
            connection.close();
        } catch (SQLException | NoSuchAlgorithmException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Transaction> loadTransactionLedger(Integer ledgerID) throws SQLException {
        ArrayList<Transaction> transactions = new ArrayList<>();
        try {
            Connection connection = DriverManager.getConnection(PropertiesUtils.getProperty("db.url.blockchain"));
            PreparedStatement stmt = connection.prepareStatement
                    (" SELECT  * FROM TRANSACTIONS WHERE LEDGER_ID = ?");
            stmt.setInt(1, ledgerID);
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                transactions.add(new Transaction(
                        resultSet.getBytes("FROM"),
                        resultSet.getBytes("TO"),
                        resultSet.getInt("VALUE"),
                        resultSet.getBytes("SIGNATURE"),
                        resultSet.getInt("LEDGER_ID"),
                        resultSet.getString("CREATED_ON")
                ));
            }
            resultSet.close();
            stmt.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    public void mineBlock() {
        try {
            finalizeBlock(WalletData.getInstance().getWallet());
            addBlock(latestBlock);
        } catch (SQLException | GeneralSecurityException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void finalizeBlock(Wallet minersWallet) throws GeneralSecurityException, SQLException {
        latestBlock = new Block(BlockchainData.getInstance().currentBlockChain);
        latestBlock.setTransactionLedger(new ArrayList<>(newBlockTransactions));
        latestBlock.setTimeStamp(LocalDateTime.now().toString());
        latestBlock.setMinedBy(minersWallet.getPublicKey().getEncoded());
        latestBlock.setMiningPoints(miningPoints);
        signing.initSign(minersWallet.getPrivateKey());
        signing.update(latestBlock.toString().getBytes());
        latestBlock.setCurrHash(signing.sign());
        currentBlockChain.add(latestBlock);
        miningPoints = 0;
        //Reward transaction
        latestBlock.getTransactionLedger().sort(transactionComparator);
        addTransaction(latestBlock.getTransactionLedger().get(0), true);
        Transaction transaction = new Transaction(new Wallet(), minersWallet.getPublicKey().getEncoded(),
                100, latestBlock.getLedgerId() + 1, signing);
        newBlockTransactions.clear();
        newBlockTransactions.add(transaction);
    }

    private void addBlock(Block block) {
        try {
            Connection connection = DriverManager.getConnection(PropertiesUtils.getProperty("db.url.blockchain"));
            PreparedStatement pstmt;
            pstmt = connection.prepareStatement
                    ("INSERT INTO BLOCKCHAIN(PREVIOUS_HASH, CURRENT_HASH, LEDGER_ID, CREATED_ON," +
                            " CREATED_BY, MINING_POINTS, LUCK) VALUES (?,?,?,?,?,?,?) ");
            pstmt.setBytes(1, block.getPrevHash());
            pstmt.setBytes(2, block.getCurrHash());
            pstmt.setInt(3, block.getLedgerId());
            pstmt.setString(4, block.getTimeStamp());
            pstmt.setBytes(5, block.getMinedBy());
            pstmt.setInt(6, block.getMiningPoints());
            pstmt.setDouble(7, block.getLuck());
            pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void replaceBlockchainInDatabase(LinkedList<Block> receivedBC) {
        try {
            Connection connection = DriverManager.getConnection(PropertiesUtils.getProperty("db.url.blockchain"));
            Statement clearDBStatement = connection.createStatement();
            clearDBStatement.executeUpdate(" DELETE FROM BLOCKCHAIN ");
            clearDBStatement.executeUpdate(" DELETE FROM TRANSACTIONS ");
            clearDBStatement.close();
            connection.close();
            for (Block block : receivedBC) {
                addBlock(block);
                boolean rewardTransaction = true;
                block.getTransactionLedger().sort(transactionComparator);
                for (Transaction transaction : block.getTransactionLedger()) {
                    addTransaction(transaction, rewardTransaction);
                    rewardTransaction = false;
                }
            }
        } catch (SQLException | GeneralSecurityException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public LinkedList<Block> getBlockchainConsensus(LinkedList<Block> receivedBC) {
        try {
            //Verify the validity of the received blockchain.
            verifyBlockChain(receivedBC);
            //Check if we have received an identical blockchain.
            if (!Arrays.equals(receivedBC.getLast().getCurrHash(), getCurrentBlockChain().getLast().getCurrHash())) {
                if (checkIfOutdated(receivedBC) != null) {
                    return getCurrentBlockChain();
                } else {
                    if (checkWhichIsCreatedFirst(receivedBC) != null) {
                        return getCurrentBlockChain();
                    } else {
                        if (compareMiningPointsAndLuck(receivedBC) != null) {
                            return getCurrentBlockChain();
                        }
                    }
                }
                // if only the transaction ledgers are different then combine them.
            } else if (!receivedBC.getLast().getTransactionLedger().equals(getCurrentBlockChain()
                    .getLast().getTransactionLedger())) {
                updateTransactionLedgers(receivedBC);
                System.out.println("Transaction ledgers updated");
                return receivedBC;
            } else {
                System.out.println("blockchains are identical");
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return receivedBC;
    }

    private void updateTransactionLedgers(LinkedList<Block> receivedBC) throws GeneralSecurityException {
        for (Transaction transaction : receivedBC.getLast().getTransactionLedger()) {
            if (!getCurrentBlockChain().getLast().getTransactionLedger().contains(transaction)) {
                getCurrentBlockChain().getLast().getTransactionLedger().add(transaction);
                System.out.println("current ledger id = " + getCurrentBlockChain().getLast().getLedgerId() + " transaction id = " + transaction.getLedgerId());
                addTransaction(transaction, false);
            }
        }
        getCurrentBlockChain().getLast().getTransactionLedger().sort(transactionComparator);
        for (Transaction transaction : getCurrentBlockChain().getLast().getTransactionLedger()) {
            if (!receivedBC.getLast().getTransactionLedger().contains(transaction)) {
                receivedBC.getLast().getTransactionLedger().add(transaction);
            }
        }
        receivedBC.getLast().getTransactionLedger().sort(transactionComparator);
    }

    private LinkedList<Block> checkIfOutdated(LinkedList<Block> receivedBC) {
        //Check how old the blockchains are.
        long lastMinedLocalBlock = LocalDateTime.parse
                (getCurrentBlockChain().getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        long lastMinedRcvdBlock = LocalDateTime.parse
                (receivedBC.getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        //if both are old just do nothing
        if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedRcvdBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {
            System.out.println("both are old check other peers");
            //If your blockchain is old but the received one is new use the received one
        } else if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedRcvdBlock + TIMEOUT_INTERVAL) >= LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {
            //we reset the mining points since we weren't contributing until now.
            setMiningPoints(0);
            replaceBlockchainInDatabase(receivedBC);
            setCurrentBlockChain(new LinkedList<>());
            loadBlockChain();
            System.out.println("received blockchain won!, local BC was old");
            //If received one is old but local is new send ours to them
        } else if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) > LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedRcvdBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {

            return getCurrentBlockChain();
        }
        return null;
    }

    private LinkedList<Block> checkWhichIsCreatedFirst(LinkedList<Block> receivedBC) {
        //Compare timestamps to see which one is created first.
        long initRcvBlockTime = LocalDateTime.parse(receivedBC.getFirst().getTimeStamp())
                .toEpochSecond(ZoneOffset.UTC);
        long initLocalBlockTIme = LocalDateTime.parse(getCurrentBlockChain().getFirst()
                .getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        if (initRcvBlockTime < initLocalBlockTIme) {
            //we reset the mining points since we weren't contributing until now.
            setMiningPoints(0);
            replaceBlockchainInDatabase(receivedBC);
            setCurrentBlockChain(new LinkedList<>());
            loadBlockChain();
            System.out.println("PeerClient blockchain won!, PeerServer's BC was old");
        } else if (initLocalBlockTIme < initRcvBlockTime) {
            return getCurrentBlockChain();
        }
        return null;
    }

    private LinkedList<Block> compareMiningPointsAndLuck(LinkedList<Block> receivedBC)
            throws GeneralSecurityException {
        //check if both blockchains have the same prevHashes to confirm they are both
        //contending to mine the last block
        //if they are the same compare the mining points and luck in case of equal mining points
        //of last block to see who wins
        if (receivedBC.equals(getCurrentBlockChain())) {
            //If received block has more mining points points or luck in case of tie
            // transfer all transactions to the winning block and add them in DB.
            if (receivedBC.getLast().getMiningPoints() > getCurrentBlockChain()
                    .getLast().getMiningPoints() || receivedBC.getLast().getMiningPoints()
                    .equals(getCurrentBlockChain().getLast().getMiningPoints()) &&
                    receivedBC.getLast().getLuck() > getCurrentBlockChain().getLast().getLuck()) {
                //remove the reward transaction from our losing block and
                // transfer the transactions to the winning block
                getCurrentBlockChain().getLast().getTransactionLedger().remove(0);
                for (Transaction transaction : getCurrentBlockChain().getLast().getTransactionLedger()) {
                    if (!receivedBC.getLast().getTransactionLedger().contains(transaction)) {
                        receivedBC.getLast().getTransactionLedger().add(transaction);
                    }
                }
                receivedBC.getLast().getTransactionLedger().sort(transactionComparator);
                //we are returning the mining points since our local block lost.
                setMiningPoints(BlockchainData.getInstance().getMiningPoints() +
                        getCurrentBlockChain().getLast().getMiningPoints());
                replaceBlockchainInDatabase(receivedBC);
                setCurrentBlockChain(new LinkedList<>());
                loadBlockChain();
                System.out.println("Received blockchain won!");
            } else {
                // remove the reward transaction from their losing block and transfer
                // the transactions to our winning block
                receivedBC.getLast().getTransactionLedger().remove(0);
                for (Transaction transaction : receivedBC.getLast().getTransactionLedger()) {
                    if (!getCurrentBlockChain().getLast().getTransactionLedger().contains(transaction)) {
                        getCurrentBlockChain().getLast().getTransactionLedger().add(transaction);
                        addTransaction(transaction, false);
                    }
                }
                getCurrentBlockChain().getLast().getTransactionLedger().sort(transactionComparator);
                return getCurrentBlockChain();
            }
        }
        return null;
    }

    public static int getTimeoutInterval() {
        return TIMEOUT_INTERVAL;
    }

    public static int getMiningInterval() {
        return MINING_INTERVAL;
    }

}
