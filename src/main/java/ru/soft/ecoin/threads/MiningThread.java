package ru.soft.ecoin.threads;

import lombok.extern.slf4j.Slf4j;

import static ru.soft.ecoin.serviceData.BlockchainData.*;
import static java.lang.String.valueOf;
import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.parse;
import static java.time.ZoneOffset.UTC;

@Slf4j
public class MiningThread extends Thread {

    @Override
    public void run() {
        while (true) {
            long lastMinedBlock = getLastMinedBlock();
            if ((lastMinedBlock + getTimeoutInterval()) < now().toEpochSecond(UTC)) {
                log.info("BlockChain is too old for mining! Update it from peers");
            } else if (((lastMinedBlock + getMiningInterval()) - now().toEpochSecond(UTC)) > 0) {
                log.info("BlockChain is current, mining will commence in {} seconds",
                        ((lastMinedBlock + getMiningInterval()) - now().toEpochSecond(UTC)));
            } else {
                log.info("MINING NEW BLOCK");
                getInstance().mineBlock();
                log.info(getInstance().getWalletBallanceFX());
            }
            log.info(valueOf(getLastMinedBlock()));
            try {
                sleep(2000);
                if (getInstance().isExit()) {
                    break;
                }
                getInstance().setMiningPoints(getInstance().getMiningPoints() + 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private long getLastMinedBlock() {
        return parse(getInstance().getCurrentBlockChain().getLast().getTimeStamp()).toEpochSecond(UTC);
    }
}