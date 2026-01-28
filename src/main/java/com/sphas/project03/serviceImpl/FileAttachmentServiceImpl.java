package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.FileAttachment;
import com.sphas.project03.mapper.FileAttachmentMapper;
import com.sphas.project03.service.FileAttachmentService;
import org.springframework.stereotype.Service;

@Service
public class FileAttachmentServiceImpl
        extends ServiceImpl<FileAttachmentMapper, FileAttachment>
        implements FileAttachmentService {
}

