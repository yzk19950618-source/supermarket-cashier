package com.cashier.module.member.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cashier.common.exception.BusinessException;
import com.cashier.common.result.ResultCode;
import com.cashier.module.member.dto.MemberDTO;
import com.cashier.module.member.dto.MemberQueryDTO;
import com.cashier.module.member.dto.RechargeDTO;
import com.cashier.module.member.entity.Member;
import com.cashier.module.member.mapper.MemberMapper;
import com.cashier.module.member.service.MemberService;
import com.cashier.module.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    @Override
    public IPage<MemberVO> pageList(MemberQueryDTO queryDTO) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getName()), Member::getName, queryDTO.getName())
                .like(StrUtil.isNotBlank(queryDTO.getPhone()), Member::getPhone, queryDTO.getPhone())
                .eq(StrUtil.isNotBlank(queryDTO.getCardNo()), Member::getCardNo, queryDTO.getCardNo())
                .orderByDesc(Member::getCreateTime);

        Page<Member> page = page(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        return page.convert(member -> BeanUtil.copyProperties(member, MemberVO.class));
    }

    @Override
    public MemberVO getByCardNo(String cardNo) {
        Member member = lambdaQuery().eq(Member::getCardNo, cardNo).one();
        if (member == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND.getCode(), "未找到该会员");
        }
        return BeanUtil.copyProperties(member, MemberVO.class);
    }

    @Override
    public void addMember(MemberDTO dto) {
        // 检查卡号是否已存在
        Member existing = lambdaQuery().eq(Member::getCardNo, dto.getCardNo()).one();
        if (existing != null) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS.getCode(), "会员卡号已存在");
        }

        Member member = new Member();
        member.setCardNo(dto.getCardNo());
        member.setName(dto.getName());
        member.setPhone(dto.getPhone());
        member.setGender(dto.getGender());
        member.setDiscount(dto.getDiscount());
        member.setStatus(1);
        save(member);
    }

    @Override
    public void updateMember(MemberDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("会员ID不能为空");
        }

        Member member = new Member();
        member.setId(dto.getId());
        member.setName(dto.getName());
        member.setPhone(dto.getPhone());
        member.setGender(dto.getGender());
        member.setDiscount(dto.getDiscount());
        updateById(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(RechargeDTO dto) {
        Member member = getById(dto.getId());
        if (member == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 增加余额
        int rows = baseMapper.addBalance(dto.getId(), dto.getAmount());
        if (rows == 0) {
            throw new BusinessException("充值失败");
        }
    }
}
