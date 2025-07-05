package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: ss
 * @description: 章节管理
 * @author: lk_hhh
 * @create: 2025-07-04 15:52
 **/

@RestController
@RequestMapping("/chapter")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    /**
     *  添加章节
     * */
    @GetMapping("/addchapter")
    public Result addChapter(){

        return Result.success();

    }

    /**
     *  删除章节
     **/
    @DeleteMapping("/deletechapter")
    public Result deleteChapter(){

        return Result.success();
    }

    /**
     * 根据课程id查询章节
     * */



}