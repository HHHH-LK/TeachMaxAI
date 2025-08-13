package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.mapper.ItemMapper;
import com.aiproject.smartcampus.mapper.UserItemMapper;
import com.aiproject.smartcampus.pojo.po.Item;
import com.aiproject.smartcampus.pojo.po.UserItem;
import com.aiproject.smartcampus.service.ItemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: TeacherMaxAI
 * @description:
 * @author: lk_hhh
 * @create: 2025-08-12 11:07
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemMapper itemMapper;
    private final UserItemMapper userItemMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<List<Item>> getIllustratedBook() {

        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();
        List<Item> items = itemMapper.selectList(queryWrapper).stream()
                .distinct()
                .collect(Collectors.toList());

        return Result.success(items);
    }

    @Override
    public Result<List<Item>> getUserItemInfo(Integer studentId) {

        LambdaQueryWrapper<UserItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserItem::getUserId, studentId);
        List<UserItem> userItems = userItemMapper.selectList(queryWrapper);

        List<Item> itemsAfterDistinct = userItems.stream().distinct()
                .map(a -> {
                    LambdaQueryWrapper<Item> itemLambdaQueryWrapper = new LambdaQueryWrapper<>();
                    itemLambdaQueryWrapper.eq(Item::getItemId, a.getItemId());
                    return itemMapper.selectOne(itemLambdaQueryWrapper);
                }).toList();


        return Result.success(itemsAfterDistinct);
    }

    @Override
    public Result<Integer> getUserItemNum(Integer itemId, Integer studentId) {

        LambdaQueryWrapper<UserItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserItem::getItemId, itemId);
        queryWrapper.eq(UserItem::getUserId, studentId);
        UserItem userItem = userItemMapper.selectOne(queryWrapper);

        if (userItem == null) {
            log.warn("用户不存在当前道具");
            return Result.success(0);
        }

        return Result.success(userItem.getQuantity());
    }

    @Override
    public Result<T> userUseItem(Integer itemId, Integer studentId, Integer floorId, Integer changeCount) {

        LambdaQueryWrapper<UserItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserItem::getItemId, itemId);
        queryWrapper.eq(UserItem::getUserId, studentId);
        UserItem userItem = userItemMapper.selectOne(queryWrapper);
        if (userItem == null) {
            log.warn("用户不存在当前道具。");
            return Result.success();
        }

        LambdaUpdateWrapper<UserItem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserItem::getItemId, itemId);
        updateWrapper.eq(UserItem::getUserId, studentId);
        updateWrapper.setSql("quantity = quantity - 1");
        int update = userItemMapper.update(null, updateWrapper);
        if (update == 0) {
            log.error("使用道具失败");
            throw new RuntimeException("使用道具失败，服务器内部异常");
        }

        //key
        String SET_USER_HP_KEY = String.format("game:user:hp:%d:%d:%d", studentId, floorId, changeCount);
        String SET_BOSS_HP_KEY = String.format("game:boss:hp:%d:%d:%d", floorId, studentId, changeCount);
        //获取用户血量和boss血量
        String USER_HP = stringRedisTemplate.opsForValue().get(SET_USER_HP_KEY);

        LambdaQueryWrapper<Item> itemLambdaQueryWrapper = new LambdaQueryWrapper<>();
        itemLambdaQueryWrapper.eq(Item::getItemId, itemId);
        Item item = itemMapper.selectOne(itemLambdaQueryWrapper);
        //todo 实现道具效果
        String type = item.getType();
        switch (type) {
            case "attack":
                break;
            case "defense":
                break;
            case "heal":
                stringRedisTemplate.opsForValue().increment(SET_USER_HP_KEY, item.getEffectValue());
                break;
        }


        return Result.success();

    }
}