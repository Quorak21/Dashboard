package com.dokkcorp.dashboard.providers;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.utils.Convert;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.contracts.eip20.generated.ERC20;

import com.dokkcorp.dashboard.providers.dto.BlockChainDto;
import com.dokkcorp.dashboard.jobs.BlockChainRequest;

@Configuration
public class BlockChainClient {

        private final HyperEvmRpc hyperEvmRpc;

        @Autowired
        private Web3j web3j;

        @Autowired
        private BlockChainRequest blockChainRequest;

        BlockChainClient(HyperEvmRpc hyperEvmRpc) {
                this.hyperEvmRpc = hyperEvmRpc;
        }

        public BlockChainDto getBlockchainData() {

                BridgedHype bridgedHype = fetchBridgedHype();
                LiquidStaked liquidStaked = fetchLiquidStaked();

                return new BlockChainDto(
                                bridgedHype.bridgedHype(),
                                liquidStaked.liquidStaked());
        }

        private BridgedHype fetchBridgedHype() {

                BigInteger hypeWei = BigInteger.ZERO;
                String bridgedHype = "0";

                try {

                        hypeWei = web3j
                                        .ethGetBalance("0x2222222222222222222222222222222222222222",
                                                        DefaultBlockParameterName.LATEST)
                                        .send().getBalance();

                        BigDecimal bridgedHypeTemp = new BigDecimal(1000000000)
                                        .subtract(Convert.fromWei(hypeWei.toString(), Convert.Unit.ETHER));
                        bridgedHype = bridgedHypeTemp.toPlainString();

                } catch (Exception e) {

                        return new BridgedHype("0");

                }

                return new BridgedHype(bridgedHype);

        }

        private LiquidStaked fetchLiquidStaked() {

                // khype sthype kmhype

                String liquidStaked = "0";
                BigDecimal stHypeStaked = BigDecimal.ZERO;
                BigDecimal khypeStaked = BigDecimal.ZERO;
                BigDecimal mkhypeStaked = BigDecimal.ZERO;

                // Création du manager en ReadOnly
                ReadonlyTransactionManager txManager = new ReadonlyTransactionManager(web3j,
                                "0x0000000000000000000000000000000000000000");

                try {

                        // sthype
                        ERC20 stHypeContract = ERC20.load("0xfFaa4a3D97fE9107Cef8a3F48c069F577Ff76cC1", web3j,
                                        txManager,
                                        new DefaultGasProvider());
                        BigInteger stHypeWei = stHypeContract.totalSupply().send();
                        stHypeStaked = Convert.fromWei(stHypeWei.toString(), Convert.Unit.ETHER);

                        // khype
                        // buffer
                        BigInteger bufferWei = web3j.ethGetBalance(
                                        "0x393d0b87ed38fc779fd9611144ae649ba6082109",
                                        DefaultBlockParameterName.LATEST)
                                        .send()
                                        .getBalance();
                        BigDecimal khypeBuffer = Convert.fromWei(bufferWei.toString(), Convert.Unit.ETHER);

                        // total staked
                        BigInteger totalStakedWei = blockChainRequest.readContract(web3j,
                                        "0x393D0B87Ed38fc779FD9611144aE649BA6082109", "totalStaked");
                        BigDecimal khypeTotalStaked = Convert.fromWei(totalStakedWei.toString(), Convert.Unit.ETHER);

                        // total claimed
                        BigInteger totalClaimedWei = blockChainRequest.readContract(web3j,
                                        "0x393D0B87Ed38fc779FD9611144aE649BA6082109", "totalClaimed");
                        BigDecimal khypeTotalClaimed = Convert.fromWei(totalClaimedWei.toString(), Convert.Unit.ETHER);

                        // In withdraw
                        BigInteger inWithdrawWei = blockChainRequest.readContract(web3j,
                                        "0x393D0B87Ed38fc779FD9611144aE649BA6082109", "totalQueuedWithdrawals");
                        BigDecimal khypeInWithdraw = Convert.fromWei(inWithdrawWei.toString(), Convert.Unit.ETHER);

                        // total
                        khypeStaked = khypeTotalStaked.subtract(khypeTotalClaimed).add(khypeBuffer)
                                        .add(khypeInWithdraw);

                        // mkhype
                        // total staked
                        BigInteger mkhypeTotalStakedWei = blockChainRequest.readContract(web3j,
                                        "0x5901e744759561C63309865Ef8822aBb041655E2", "totalStaked");
                        BigDecimal mkhypeTotalStaked = Convert.fromWei(mkhypeTotalStakedWei.toString(),
                                        Convert.Unit.ETHER);
                        // total claimed
                        BigInteger mkhypeTotalClaimedWei = blockChainRequest.readContract(web3j,
                                        "0x5901e744759561C63309865Ef8822aBb041655E2", "totalClaimed");
                        BigDecimal mkhypeTotalClaimed = Convert.fromWei(mkhypeTotalClaimedWei.toString(),
                                        Convert.Unit.ETHER);

                        mkhypeStaked = mkhypeTotalStaked.subtract(mkhypeTotalClaimed);

                } catch (Exception e) {
                        return new LiquidStaked("0");
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
