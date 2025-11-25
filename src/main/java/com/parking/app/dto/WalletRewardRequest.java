package com.parking.app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletRewardRequest {
    private Double amount;
    private String source;
    private String rewardId;
}
