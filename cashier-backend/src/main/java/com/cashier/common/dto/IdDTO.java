package com.cashier.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ID 请求 DTO
 *
 * @author cashier
 * @since 2024-01-01
 */
@Data
@Schema(description = "ID请求")
public class IdDTO {

    @NotNull(message = "ID不能为空")
    @Schema(description = "ID", required = true)
    private Long id;
}
