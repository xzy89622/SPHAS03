package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.Challenge;
import com.sphas.project03.mapper.ChallengeMapper;
import com.sphas.project03.service.ChallengeService;
import org.springframework.stereotype.Service;

/**
 * 挑战Service实现
 */
@Service
public class ChallengeServiceImpl extends ServiceImpl<ChallengeMapper, Challenge> implements ChallengeService {
}
