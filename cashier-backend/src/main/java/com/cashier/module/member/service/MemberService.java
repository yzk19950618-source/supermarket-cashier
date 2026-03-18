package com.cashier.module.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cashier.module.member.dto.MemberDTO;
import com.cashier.module.member.dto.MemberQueryDTO;
import com.cashier.module.member.dto.RechargeDTO;
import com.cashier.module.member.entity.Member;
import com.cashier.module.member.vo.MemberVO;

public interface MemberService extends IService<Member> {

    IPage<MemberVO> pageList(MemberQueryDTO queryDTO);

    MemberVO getByCardNo(String cardNo);

    void addMember(MemberDTO dto);

    void updateMember(MemberDTO dto);

    void recharge(RechargeDTO dto);
}
