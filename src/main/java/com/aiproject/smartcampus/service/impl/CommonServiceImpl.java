package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.exception.UserExpection;
import com.aiproject.smartcampus.mapper.*;
import com.aiproject.smartcampus.pojo.bo.NotificationMessage;
import com.aiproject.smartcampus.service.CommonService;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

import static net.sf.jsqlparser.util.validation.metadata.NamedObject.role;
import static org.apache.poi.hslf.model.textproperties.TextPropCollection.TextPropType.character;

/**
 * @program: SmartCampus
 * @description: 公用实现类
 * @author: lk
 * @create: 2025-05-20 09:21
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class CommonServiceImpl implements CommonService {




}
