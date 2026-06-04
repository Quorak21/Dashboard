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

                BridgedHype bridgedHype = fetchBridgedHype();
                LiquidStaked liquidStaked = fetchLiquidStaked();

                return new BlockChainDto(
                                bridgedHype.bridgedHype(),
                                liquidStaked.liquidStaked());
        }

        // Récupère les données pour les HYPE Bridgés sur EVM
        private BridgedHype fetchBridgedHype() {

                BigInteger hypeWei = BigInteger.ZERO;
                String bridgedHype = "0";

                try {
                        hypeWei = externalCallExecutor.execute(() -> {
                                try {
                                        return web3j
                                                        .ethGetBalance(BRIDGE_VAULT_ADDRESS,
                                                                        DefaultBlockParameterName.LATEST)
                                                        .send().getBalance();
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });

                        BigDecimal bridgedHypeTemp = HypeConstants.TOTAL_SUPPLY_BD
                                        .subtract(Convert.fromWei(hypeWei.toString(), Convert.Unit.ETHER));
                        bridgedHype = bridgedHypeTemp.toPlainString();

                } catch (Exception e) {
                        logger.error("Error fetching bridged hype: {}", e.getMessage());
                        return new BridgedHype("0");
                }

                return new BridgedHype(bridgedHype);

        }

        // Récupère la quantité de HYPE staké, en aditionnant le khype, le sthype et le mkhype
        // TODO : Utiliser CompletableFuture pour paralléliser les appels et éviter la latence entre chaque requete
        private LiquidStaked fetchLiquidStaked() {

                String liquidStaked = "0";
                BigDecimal stHypeStaked = BigDecimal.ZERO;
                BigDecimal khypeStaked = BigDecimal.ZERO;
                BigDecimal mkhypeStaked = BigDecimal.ZERO;

                // Création du manager en ReadOnly, on utilise la zero address pour avoir le from nécessaire
                ReadonlyTransactionManager txManager = new ReadonlyTransactionManager(web3j,
                                ZERO_ADDRESS);

                // sthype
                try {
                        ERC20 stHypeContract = ERC20.load(STHYPE_CONTRACT_ADDRESS, web3j,
                                        txManager,
                                        new DefaultGasProvider());
                        BigInteger stHypeWei = externalCallExecutor.execute(() -> {
                                try {
                                        return stHypeContract.totalSupply().send();
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });
                        stHypeStaked = Convert.fromWei(stHypeWei.toString(), Convert.Unit.ETHER);
                } catch (Exception e) {
                        logger.error("Error fetching stHype: {}", e.getMessage());
                        stHypeStaked = BigDecimal.ZERO;
                }

                // khype, On soustrait le total staked par le total claimed et on ajoute le
                // buffer et le inWithdraw pour avoir un chiffre juste
                try {
                        // buffer
                        BigInteger bufferWei = externalCallExecutor.execute(() -> {
                                try {
                                        return web3j.ethGetBalance(
                                                        KHYPE_CONTRACT_ADDRESS,
                                                        DefaultBlockParameterName.LATEST)
                                                        .send()
                                                        .getBalance();
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });
                        BigDecimal khypeBuffer = Convert.fromWei(bufferWei.toString(), Convert.Unit.ETHER);

                        // total staked
                        BigInteger totalStakedWei = externalCallExecutor.execute(() -> {
                                try {
                                        return contractReader.readContract(web3j, KHYPE_CONTRACT_ADDRESS, "totalStaked");
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });
                        BigDecimal khypeTotalStaked = Convert.fromWei(totalStakedWei.toString(), Convert.Unit.ETHER);

                        // total claimed
                        BigInteger totalClaimedWei = externalCallExecutor.execute(() -> {
                                try {
                                        return contractReader.readContract(web3j, KHYPE_CONTRACT_ADDRESS, "totalClaimed");
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });
                        BigDecimal khypeTotalClaimed = Convert.fromWei(totalClaimedWei.toString(), Convert.Unit.ETHER);

                        // In withdraw
                        BigInteger inWithdrawWei = externalCallExecutor.execute(() -> {
                                try {
                                        return contractReader.readContract(web3j, KHYPE_CONTRACT_ADDRESS, "totalQueuedWithdrawals");
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });
                        BigDecimal khypeInWithdraw = Convert.fromWei(inWithdrawWei.toString(), Convert.Unit.ETHER);

                        // total
                        khypeStaked = khypeTotalStaked.subtract(khypeTotalClaimed).add(khypeBuffer)
                                        .add(khypeInWithdraw);
                } catch (Exception e) {
                        logger.error("Error fetching khype: {}", e.getMessage());
                        khypeStaked = BigDecimal.ZERO;
                }

                // mkhype, total staked moins claimed, pas d'autres sources trouvées
                try {
                        // total staked
                        BigInteger mkhypeTotalStakedWei = externalCallExecutor.execute(() -> {
                                try {
                                        return contractReader.readContract(web3j, MKHYPE_CONTRACT_ADDRESS, "totalStaked");
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });
                        BigDecimal mkhypeTotalStaked = Convert.fromWei(mkhypeTotalStakedWei.toString(),
                                        Convert.Unit.ETHER);
                        // total claimed
                        BigInteger mkhypeTotalClaimedWei = externalCallExecutor.execute(() -> {
                                try {
                                        return contractReader.readContract(web3j, MKHYPE_CONTRACT_ADDRESS, "totalClaimed");
                                } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                }
                        });
                        BigDecimal mkhypeTotalClaimed = Convert.fromWei(mkhypeTotalClaimedWei.toString(),
                                        Convert.Unit.ETHER);

                        mkhypeStaked = mkhypeTotalStaked.subtract(mkhypeTotalClaimed);

                } catch (Exception e) {
                        logger.error("Error fetching mkhype: {}", e.getMessage());
                        mkhypeStaked = BigDecimal.ZERO;
                }

                // Compilation
                BigDecimal liquidStakedTemp = stHypeStaked.add(khypeStaked).add(mkhypeStaked);
                liquidStaked = liquidStakedTemp.toPlainString();

                return new LiquidStaked(liquidStaked);

        }

        record BridgedHype(
                        String bridgedHype) {
        }

        record LiquidStaked(
                        String liquidStaked) {
        }

}
