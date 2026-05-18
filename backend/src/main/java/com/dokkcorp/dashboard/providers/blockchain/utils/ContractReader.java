package com.dokkcorp.dashboard.providers.blockchain.utils;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ContractReader {

    // Fonction qui permet de lire un contrat pour en ressortir ses datas, va etre utilisé pour des requetes plus poussée qu'une simple balance
    public BigInteger readContract(Web3j web3j, String contractAddress, String functionName) throws Exception {

        Function function = new Function(
                functionName,
                Collections.emptyList(),
                Arrays.asList(new TypeReference<Uint256>() {
                }));
        String encodedData = FunctionEncoder.encode(function);

        Transaction transaction = Transaction.createEthCallTransaction(null, contractAddress, encodedData);
        String responseRaw = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send().getValue();

        @SuppressWarnings("rawtypes")
        List<Type> decodedResponses = FunctionReturnDecoder.decode(responseRaw, function.getOutputParameters());

        if (decodedResponses.isEmpty()) {
            return BigInteger.ZERO;
        } else {
            return (BigInteger) decodedResponses.get(0).getValue();
        }
    }
}
