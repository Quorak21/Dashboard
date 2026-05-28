package com.dokkcorp.dashboard.features.crypto.hype;

import com.dokkcorp.dashboard.features.crypto.hype.models.HypeBlockchainDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeChartsDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeHlpDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSummaryDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSupplyDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeTimedDataDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeValuationDto;

// TODO: Refactorisé en mini-DTO pour allégé et separer les données
public record HypeDto(

        HypeSummaryDto summary,

        HypeChartsDto charts,

        HypeTimedDataDto timedData,

        HypeSupplyDto supply,

        HypeBlockchainDto blockchain,

        HypeHlpDto hlp,

        HypeValuationDto valuation

) {

    // Pour gérer les futures erreur et éviter de tout se retaper a la main
    public static HypeDto error(String symbol) {
        return new HypeDto(

                HypeSummaryDto.error(symbol),
                HypeChartsDto.error(symbol),
                HypeTimedDataDto.error(symbol),
                HypeSupplyDto.error(symbol),
                HypeBlockchainDto.error(symbol),
                HypeHlpDto.error(symbol),
                HypeValuationDto.error(symbol)
            );
    }
}
