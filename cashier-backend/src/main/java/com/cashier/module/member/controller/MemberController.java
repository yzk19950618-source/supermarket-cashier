package com.cashier.module.member.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cashier.common.dto.IdDTO;
import com.cashier.common.result.R;
import com.cashier.module.member.dto.*;
import com.cashier.module.member.service.MemberService;
import com.cashier.module.member.vo.MemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "会员管理")
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "分页查询会员列表")
    @PostMapping("/page")
    public R<IPage<MemberVO>> page(@RequestBody MemberQueryDTO queryDTO) {
        return R.ok(memberService.pageList(queryDTO));
    }

    @Operation(summary = "根据卡号查询会员")
    @PostMapping("/getByCardNo")
    public R<MemberVO> getByCardNo(@RequestBody @Valid MemberCardDTO dto) {
        return R.ok(memberService.getByCardNo(dto.getCardNo()));
    }

    @Operation(summary = "新增会员")
    @PostMapping("/add")
    public R<Void> add(@RequestBody @Valid MemberDTO dto) {
        memberService.addMember(dto);
        return R.ok();
    }

    @Operation(summary = "修改会员")
    @PostMapping("/update")
    public R<Void> update(@RequestBody @Valid MemberDTO dto) {
        memberService.updateMember(dto);
        return R.ok();
    }

    @Operation(summary = "删除会员")
    @PostMapping("/delete")
    public R<Void> delete(@RequestBody @Valid IdDTO dto) {
        memberService.removeById(dto.getId());
        return R.ok();
    }

    @Operation(summary = "会员充值")
    @PostMapping("/recharge")
    public R<Void> recharge(@RequestBody @Valid RechargeDTO dto) {
        memberService.recharge(dto);
        return R.ok();
    }
}
