package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.QuestionBank;
import com.sphas.project03.mapper.QuestionBankMapper;
import com.sphas.project03.service.QuestionBankService;
import org.springframework.stereotype.Service;

/**
 * 题库 ServiceImpl
 */
@Service
public class QuestionBankServiceImpl extends ServiceImpl<QuestionBankMapper, QuestionBank>
        implements QuestionBankService {
}
