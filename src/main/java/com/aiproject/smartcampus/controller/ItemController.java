package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.Item;
import com.aiproject.smartcampus.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @program: TeacherMaxAI
 * @description: 道具管理
 * @author: lk_hhh
 * @create: 2025-08-06 11:17
 **/

@RestController
@RequestMapping("/item")
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final ItemService itemService;


    /**
     * 查看道具图鉴
     */
    @GetMapping("/get/IllustratedBook")
    public Result<List<Item>> getIllustratedBook() {

        return itemService.getIllustratedBook();
    }

    /**
     * 查看玩家的道具
     */
    @GetMapping("/get/studentItemInfo")
    public Result<List<Item>> getStudentItemInfo(@RequestParam Integer studentId) {

        return itemService.getUserItemInfo(studentId);
    }


    /**
     * 查看道具数量
     */
    @GetMapping("/get/itemnum")
    public Result<Integer> getItemNum(@RequestParam Integer itemId, @RequestParam Integer studentId) {

        return itemService.getUserItemNum(itemId, studentId);

    }


    /**
     * 使用道具
     */
    @PostMapping("/User/useitem")
    public Result<T> useItem(@RequestParam Integer itemId, @RequestParam Integer studentId,@RequestParam Integer floorId, @RequestParam Integer changeCount,@RequestParam Integer max_HP)  {

        return itemService.userUseItem(itemId, studentId,floorId,changeCount,max_HP);
    }




}