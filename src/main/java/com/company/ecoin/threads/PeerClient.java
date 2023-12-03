package com.company.ecoin.threads;

import com.company.ecoin.model.Block;
import com.company.ecoin.serviceData.BlockchainData;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.company.ecoin.util.PropertiesUtils.getProperty;
import static java.util.Arrays.stream;

@Slf4j
public class PeerClient extends Thread {

    private final Queue<Integer> queue = new ConcurrentLinkedQueue<>();

    public PeerClient() throws IllegalAccessException {
        String clientPorts = getProperty("client.ports");
        stream(clientPorts.split(","))
                .forEach(port -> this.queue.add(Integer.valueOf(port)));
        if (queue.isEmpty()) {
            throw new IllegalAccessException("set client ports");
        }
    }

    @Override
    @SneakyThrows
    public void run() {
        while (true) {
            sleep(2000);
            try (Socket socket = new Socket("127.0.0.1", queue.peek())) {
                log.info("Sending blockchain object on port: {}", queue.peek());
                queue.add(queue.poll());
                socket.setSoTimeout(5000);

                ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());

                LinkedList<Block> blockChain = BlockchainData.getInstance().getCurrentBlockChain();
                objectOutput.writeObject(blockChain);

                @SuppressWarnings("unchecked")
                LinkedList<Block> returnedBlockchain = (LinkedList<Block>) objectInput.readObject();
                log.info("RETURNED BC LedgerId = {} Size= {}", returnedBlockchain.getLast().getLedgerId(), returnedBlockchain.getLast().getTransactionLedger().size());
                BlockchainData.getInstance().getBlockchainConsensus(returnedBlockchain);
                sleep(2000);
            } catch (SocketTimeoutException e) {
                log.error("The socket timed out");
                queue.add(queue.poll());
            } catch (IOException e) {
                log.error("Client Error: {} -- Error on port: {}", e.getMessage(), queue.peek());
                queue.add(queue.poll());
            } catch (InterruptedException | ClassNotFoundException e) {
                log.error("Peer exception", e);
                queue.add(queue.poll());
            }
        }
    }
}
