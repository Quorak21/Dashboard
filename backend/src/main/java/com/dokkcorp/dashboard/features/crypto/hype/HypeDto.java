package com.dokkcorp.dashboard.features.crypto.hype;

import com.dokkcorp.dashboard.features.crypto.hype.models.HypeBlockchainDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeChartsDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeHlpDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSummaryDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSupplyDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeTimedDataDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeValuationDto;

public record HypeDto(

        HypeSummaryDto summary,

        HypeChartsDto charts,

        HypeTimedDataDto timedData,

        HypeSupplyDto supply,

        HypeBlockchainDto blockchain,

        HypeHlpDto hlp,

        HypeValuationDto valuation

) {
}
