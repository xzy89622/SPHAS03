package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.ChallengeJoin;
import com.sphas.project03.mapper.ChallengeJoinMapper;
import com.sphas.project03.service.ChallengeJoinService;
import org.springframework.stereotype.Service;

/**
 * 挑战报名Service实现
 */
@Service
public class ChallengeJoinServiceImpl extends ServiceImpl<ChallengeJoinMapper, ChallengeJoin> implements ChallengeJoinService {
}
