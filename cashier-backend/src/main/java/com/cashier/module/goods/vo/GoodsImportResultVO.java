package com.cashier.module.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "商品批量导入结果")
public class GoodsImportResultVO {

    @Schema(description = "有效数据行数（不含表头）")
    private int total;

    @Schema(description = "成功条数")
    private int success;

    @Schema(description = "失败条数")
    private int fail;

    @Schema(description = "错误信息（最多 80 条）")
    private List<String> errors = new ArrayList<>();
}
