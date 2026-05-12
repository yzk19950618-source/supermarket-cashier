package com.cashier.module.member.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cashier.common.constant.CommonConstant;
import com.cashier.common.exception.BusinessException;
import com.cashier.common.result.ResultCode;
import com.cashier.common.utils.SyntheticMemberPhone;
import com.cashier.module.member.dto.MemberDTO;
import com.cashier.module.member.dto.MemberDebtRow;
import com.cashier.module.member.dto.MemberQueryDTO;
import com.cashier.module.member.dto.RechargeDTO;
import com.cashier.module.member.entity.Member;
import com.cashier.module.member.mapper.MemberMapper;
import com.cashier.module.member.service.MemberService;
import com.cashier.module.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    private static MemberVO toVo(Member member) {
        MemberVO vo = BeanUtil.copyProperties(member, MemberVO.class);
        vo.setCardNo(member.getPhone());
        return vo;
    }

    @Override
    public IPage<MemberVO> pageList(MemberQueryDTO queryDTO) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getName()), Member::getName, queryDTO.getName())
                .like(StrUtil.isNotBlank(queryDTO.getPhone()), Member::getPhone, queryDTO.getPhone())
                .eq(StrUtil.isNotBlank(queryDTO.getCardNo()), Member::getPhone, queryDTO.getCardNo())
                .like(StrUtil.isNotBlank(queryDTO.getAddress()), Member::getAddress, queryDTO.getAddress())
                .orderByDesc(Member::getCreateTime);

        Page<Member> page = page(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        List<Long> ids = page.getRecords().stream().map(Member::getId).collect(Collectors.toList());
        Map<Long, BigDecimal> debtMap = new HashMap<>();
        if (!ids.isEmpty()) {
            for (MemberDebtRow row : baseMapper.selectDebtTotalsByMemberIds(ids)) {
                debtMap.put(row.getMemberId(),
                        row.getTotalDebt() != null ? row.getTotalDebt() : BigDecimal.ZERO);
            }
        }
        final Map<Long, BigDecimal> totals = debtMap;
        return page.convert(m -> {
            MemberVO vo = toVo(m);
            vo.setTotalDebt(totals.getOrDefault(m.getId(), BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP));
            return vo;
        });
    }

    @Override
    public MemberVO getByCardNo(String cardNo) {
        Member member = lambdaQuery().eq(Member::getPhone, cardNo).one();
        if (member == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND.getCode(), "未找到该会员");
        }
        return toVo(member);
    }

    @Override
    public void addMember(MemberDTO dto) {
        String normalizedPhone = StrUtil.trim(dto.getPhone());
        String phoneToStore = StrUtil.isNotBlank(normalizedPhone) ? normalizedPhone : nextUniqueSyntheticPhone();
        Member existingPhone = lambdaQuery().eq(Member::getPhone, phoneToStore).one();
        if (existingPhone != null) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS.getCode(), "手机号已注册");
        }

        Member member = new Member();
        member.setName(dto.getName());
        member.setPhone(phoneToStore);
        member.setGender(dto.getGender() != null ? dto.getGender() : 0);
        member.setAddress(StrUtil.blankToDefault(dto.getAddress(), null));
        member.setRemark(truncateMemberRemark(StrUtil.blankToDefault(dto.getRemark(), null)));
        member.setDiscount(dto.getDiscount());
        member.setStatus(1);
        save(member);
    }

    @Override
    public void updateMember(MemberDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("会员ID不能为空");
        }

        Member current = getById(dto.getId());
        if (current == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        String normalizedPhone = StrUtil.trim(dto.getPhone());
        String phoneToStore = StrUtil.isBlank(normalizedPhone) ? current.getPhone() : normalizedPhone;

        Member otherPhone = lambdaQuery()
                .eq(Member::getPhone, phoneToStore)
                .ne(Member::getId, dto.getId())
                .one();
        if (otherPhone != null) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS.getCode(), "手机号已被其他会员使用");
        }

        Member member = new Member();
        member.setId(dto.getId());
        member.setName(dto.getName());
        member.setPhone(phoneToStore);
        member.setGender(dto.getGender());
        member.setAddress(StrUtil.blankToDefault(dto.getAddress(), null));
        member.setRemark(truncateMemberRemark(StrUtil.blankToDefault(dto.getRemark(), null)));
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

        int rows = baseMapper.addBalance(dto.getId(), dto.getAmount());
        if (rows == 0) {
            throw new BusinessException("充值失败");
        }
    }

    private static String truncateMemberRemark(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        int max = CommonConstant.MEMBER_REMARK_MAX_LENGTH;
        return t.length() <= max ? t : t.substring(0, max);
    }

    private String nextUniqueSyntheticPhone() {
        for (int i = 0; i < 12; i++) {
            String p = SyntheticMemberPhone.next();
            if (lambdaQuery().eq(Member::getPhone, p).eq(Member::getDeleted, 0).count() == 0) {
                return p;
            }
        }
        throw new BusinessException("生成占位手机号失败，请重试");
    }
}
