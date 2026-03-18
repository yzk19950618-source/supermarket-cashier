package com.cashier.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "分类信息")
public class CategoryDTO {

    @Schema(description = "分类ID（编辑时必传）")
    private Long id;

    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称", required = true)
    private String name;

    @Schema(description = "父分类ID")
    private Long parentId = 0L;

    @Schema(description = "排序号")
    private Integer sort = 0;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status = 1;
}
