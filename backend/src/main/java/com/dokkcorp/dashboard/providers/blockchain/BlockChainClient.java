package com.dokkcorp.dashboard.providers.blockchain;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.web3j.contracts.eip20.generated.ERC20;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;

import org.springframework.stereotype.Service;

import com.dokkcorp.dashboard.config.ExternalCallExecutor;
import com.dokkcorp.dashboard.features.crypto.hype.maths.HypeConstants;
import com.dokkcorp.dashboard.providers.blockchain.utils.ContractReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class BlockChainClient {

        private static final Logger logger = LoggerFactory.getLogger(BlockChainClient.class);

        private final Web3j web3j;
        private final ContractReader contractReader;
        private final ExternalCallExecutor externalCallExecutor;

        // Liste des adresses de contrats utilisées
        private static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";
        private static final String BRIDGE_VAULT_ADDRESS = "0x2222222222222222222222222222222222222222";
        private static final String STHYPE_CONTRACT_ADDRESS = "0xfFaa4a3D97fE9107Cef8a3F48c069F577Ff76cC1";
        private static final String KHYPE_CONTRACT_ADDRESS = "0x393d0b87ed38fc779fd9611144ae649ba6082109";
        private static final String MKHYPE_CONTRACT_ADDRESS = "0x5901e744759561C63309865Ef8822aBb041655E2";

        public BlockChainClient(Web3j web3j, ContractReader contractReader, ExternalCallExecutor externalCallExecutor) {
                this.web3j = web3j;
                this.contractReader = contractReader;
                this.externalCallExecutor = externalCallExecutor;
        }

        // La fonction principal
        // Récupère les données des deux autres fonctions pour renvoyer le DTO des données onchain final
        public BlockChainDto getBlockchainData() {

                try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                        Future<BridgedHype> bridgedFuture = executor.submit(this::fetchBridgedHype);
                        Future<LiquidStaked> liquidFuture = executor.submit(this::fetchLiquidStaked);

                        return new BlockChainDto(bridgedFuture.get().bridgedHype(), liquidFuture.get().liquidStaked());
                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("Interrupted while fetching blockchain data: {}", e.getMessage());
                        return new BlockChainDto("0", "0");
                } catch (ExecutionException e) {
                        logger.error("Error fetching blockchain data: {}",
                                        e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                        return new BlockChainDto("0", "0");
                }
        }

        // Récupère les données pour les HYPE Bridgés sur EVM
        private BridgedHype fetchBridgedHype() {

                BigInteger hypeWei = BigInteger.ZERO;
                String bridgedHype = "0";

                try {
                        hypeWei = externalCallExecutor.execute(() -> {
                                try {
                                        return web3j.ethGetBalance(BRIDGE_VAULT_ADDRESS, DefaultBlockParameterName.LATEST).send().getBalance();
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });

                        BigDecimal bridgedHypeTemp = HypeConstants.TOTAL_SUPPLY_BD.subtract(Convert.fromWei(hypeWei.toString(), Convert.Unit.ETHER));
                        bridgedHype = bridgedHypeTemp.toPlainString();

                } catch (Exception e) {
                        logger.error("Error fetching bridged hype: {}", e.getMessage());
                        return new BridgedHype("0");
                }

                return new BridgedHype(bridgedHype);

        }

        // Récupère la quantité de HYPE staké, en aditionnant le khype, le sthype et le mkhype
        private LiquidStaked fetchLiquidStaked() {
                try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                        Future<BigDecimal> stHypeFuture = executor.submit(this::fetchStHypeStaked);
                        Future<BigDecimal> khypeFuture = executor.submit(this::fetchKhypeStaked);
                        Future<BigDecimal> mkhypeFuture = executor.submit(this::fetchMkhypeStaked);

                        BigDecimal liquidStakedTotal = stHypeFuture.get().add(khypeFuture.get()).add(mkhypeFuture.get());
                        return new LiquidStaked(liquidStakedTotal.toPlainString());
                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("Interrupted while fetching liquid staked: {}", e.getMessage());
                        return new LiquidStaked("0");
                } catch (ExecutionException e) {
                        logger.error("Error fetching liquid staked: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                        return new LiquidStaked("0");
                }
        }

        private BigDecimal fetchStHypeStaked() {
                ReadonlyTransactionManager txManager = new ReadonlyTransactionManager(web3j, ZERO_ADDRESS);
                try {
                        ERC20 stHypeContract = ERC20.load(STHYPE_CONTRACT_ADDRESS, web3j, txManager, new DefaultGasProvider());
                        BigInteger stHypeWei = externalCallExecutor.execute(() -> {
                                try {
                                        return stHypeContract.totalSupply().send();
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });
                        return Convert.fromWei(stHypeWei.toString(), Convert.Unit.ETHER);
                } catch (Exception e) {
                        logger.error("Error fetching stHype: {}", e.getMessage());
                        return BigDecimal.ZERO;
                }
        }

        private BigDecimal fetchKhypeStaked() {
                try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                        Future<BigInteger> bufferFuture = executor.submit(this::fetchKhypeBufferWei);
                        Future<BigInteger> totalStakedFuture = executor.submit(this::fetchKhypeTotalStakedWei);
                        Future<BigInteger> totalClaimedFuture = executor.submit(this::fetchKhypeTotalClaimedWei);
                        Future<BigInteger> inWithdrawFuture = executor.submit(this::fetchKhypeInWithdrawWei);

                        BigDecimal khypeTotalStaked = toEther(totalStakedFuture.get());
                        BigDecimal khypeTotalClaimed = toEther(totalClaimedFuture.get());
                        BigDecimal khypeBuffer = toEther(bufferFuture.get());
                        BigDecimal khypeInWithdraw = toEther(inWithdrawFuture.get());

                        return khypeTotalStaked.subtract(khypeTotalClaimed).add(khypeBuffer).add(khypeInWithdraw);
                } catch (Exception e) {
                        logger.error("Error fetching khype: {}", e.getMessage());
                        return BigDecimal.ZERO;
                }
        }

        private BigDecimal fetchMkhypeStaked() {
                try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                        Future<BigInteger> totalStakedFuture = executor.submit(this::fetchMkhypeTotalStakedWei);
                        Future<BigInteger> totalClaimedFuture = executor.submit(this::fetchMkhypeTotalClaimedWei);

                        BigDecimal mkhypeTotalStaked = toEther(totalStakedFuture.get());
                        BigDecimal mkhypeTotalClaimed = toEther(totalClaimedFuture.get());
                        return mkhypeTotalStaked.subtract(mkhypeTotalClaimed);
                } catch (Exception e) {
                        logger.error("Error fetching mkhype: {}", e.getMessage());
                        return BigDecimal.ZERO;
                }
        }

        private BigInteger fetchKhypeBufferWei() {
                try {
                        return externalCallExecutor.execute(() -> {
                                try {
                                        return web3j.ethGetBalance(KHYPE_CONTRACT_ADDRESS, DefaultBlockParameterName.LATEST).send().getBalance();
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });
                } catch (Exception e) {
                        logger.error("Error fetching khype buffer: {}", e.getMessage());
                        return BigInteger.ZERO;
                }
        }

        private BigInteger fetchKhypeTotalStakedWei() {
                try {
                        return externalCallExecutor.execute(() -> {
                                try {
                                        return contractReader.readContract(web3j, KHYPE_CONTRACT_ADDRESS, "totalStaked");
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });
                } catch (Exception e) {
                        logger.error("Error fetching khype totalStaked: {}", e.getMessage());
                        return BigInteger.ZERO;
                }
        }

        private BigInteger fetchKhypeTotalClaimedWei() {
                try {
                        return externalCallExecutor.execute(() -> {
                                try {
                                        return contractReader.readContract(web3j, KHYPE_CONTRACT_ADDRESS, "totalClaimed");
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });
                } catch (Exception e) {
                        logger.error("Error fetching khype totalClaimed: {}", e.getMessage());
                        return BigInteger.ZERO;
                }
        }

        private BigInteger fetchKhypeInWithdrawWei() {
                try {
                        return externalCallExecutor.execute(() -> {
                                try {
                                        return contractReader.readContract(web3j, KHYPE_CONTRACT_ADDRESS, "totalQueuedWithdrawals");
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });
                } catch (Exception e) {
                        logger.error("Error fetching khype inWithdraw: {}", e.getMessage());
                        return BigInteger.ZERO;
                }
        }

        private BigInteger fetchMkhypeTotalStakedWei() {
                try {
                        return externalCallExecutor.execute(() -> {
                                try {
                                        return contractReader.readContract(web3j, MKHYPE_CONTRACT_ADDRESS, "totalStaked");
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });
                } catch (Exception e) {
                        logger.error("Error fetching mkhype totalStaked: {}", e.getMessage());
                        return BigInteger.ZERO;
                }
        }

        private BigInteger fetchMkhypeTotalClaimedWei() {
                try {
                        return externalCallExecutor.execute(() -> {
                                try {
                                        return contractReader.readContract(web3j, MKHYPE_CONTRACT_ADDRESS, "totalClaimed");
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });
                } catch (Exception e) {
                        logger.error("Error fetching mkhype totalClaimed: {}", e.getMessage());
                        return BigInteger.ZERO;
                }
        }

        private static BigDecimal toEther(BigInteger wei) {
                return Convert.fromWei(wei.toString(), Convert.Unit.ETHER);
        }

        record BridgedHype(String bridgedHype) {
        }

        record LiquidStaked(String liquidStaked) {
        }

}
