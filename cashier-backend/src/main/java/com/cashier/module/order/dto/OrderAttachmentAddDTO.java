package com.cashier.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "订单附件（发票图片等）")
public class OrderAttachmentAddDTO {

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID", required = true)
    private Long orderId;

    @NotBlank(message = "文件地址不能为空")
    @Schema(description = "上传接口返回的 url，如 /uploads/...", required = true)
    private String url;

    @Schema(description = "类型：1-发票 2-欠条 3-送货图片（默认 1）")
    private Integer attachmentType = 1;
}
