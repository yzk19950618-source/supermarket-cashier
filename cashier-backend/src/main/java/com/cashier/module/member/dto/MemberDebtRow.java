package com.cashier.module.member.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MemberDebtRow {
    private Long memberId;
    private BigDecimal totalDebt;
}
