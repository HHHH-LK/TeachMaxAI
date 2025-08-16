package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.BattleLog;
import com.aiproject.smartcampus.pojo.po.QuestionBank;
import com.aiproject.smartcampus.pojo.vo.AwardVO;
import com.aiproject.smartcampus.service.FightingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @program: TeacherMaxAI
 * @description: 战斗管理
 * @author: lk_hhh
 * @create: 2025-08-06 11:32
 **/

@RestController
@RequestMapping("/fighting")
@RequiredArgsConstructor
@Slf4j
public class FightingController {

    private final FightingService fightingService;

    /**
     * 进入回合战斗(返回当前用户挑战塔层Id,并将boss，用户血量缓存进redis中供后续查询)
     */
    @PostMapping("/start/fighting")
    public Result<Long> startFighting(@RequestParam(value = "floorId") String floorId, @RequestParam(value = "studentId") String studentId) {

        return fightingService.startFighting(floorId, studentId);
    }

    /**
     * 获取当前回合（新建当前回合并返回）
     */
    @GetMapping("/get/currentBattleLog")
    public Result<BattleLog> getCurrentBattleLog(@RequestParam(value = "floorId") String floorId, @RequestParam(value = "towerChallengeLogId") String towerChallengeLogId
            , @RequestParam(value = "studentId") String studentId) {

        return fightingService.getCurrentBattleLog(floorId, towerChallengeLogId, studentId);
    }

    /**
     * 查看战斗进度(获取当前用户血量以及怪物血量)
     */
    @GetMapping("/get/gameUserHPandbossHP")
    public Result<Map<String, String>> getGameUserHPandbossHP(@RequestParam(value = "studentId") String studentId, @RequestParam(value = "towerChallengeLogId") String towerChallengeLogId) {

        return fightingService.getgetGameUserHPandbossHP(studentId, towerChallengeLogId);
    }

    /**
     * 玩家攻击（获取题目进行答题）
     */
    @PostMapping("/user/attack")
    public Result<QuestionBank> userAttack(@RequestParam(value = "studentId") String studentId, @RequestParam(value = "towerChallengeLogId") String towerChallengeLogId) {

        return fightingService.userAttack(studentId, towerChallengeLogId);
    }

    /**
     * 当前回合结果（扣血）并对回合结果信息进行处理
     */
    @PostMapping("/user/answerquestion")
    public Result<Map<String, String>> checkAswerIsTure(@RequestParam(value = "studentId") String studentId, @RequestParam(value = "questionId") String questionId
            , @RequestParam(value = "context") String context, @RequestParam(value = "floorId") String floorId, @RequestParam(value = "changeCount") String changeCount) {

        return fightingService.checkAswerIsTure(studentId, questionId, context, floorId, changeCount);
    }

    /**
     * 获取当前第几次挑战
     */
    @GetMapping("/user/changeCount")
    public Result<Integer> getUserChangeCount(@RequestParam(value = "studentId") String studentId, @RequestParam(value = "floorId") String floorId) {

        return fightingService.getUserChangeCount(studentId, floorId);
    }

    /**
     * 玩家攻击（查询对应的伤害）
     */
    @GetMapping("/get/damage")
    public Result<Integer> getDamage(@RequestParam(value = "questionId") String questionId, @RequestParam(value = "floorId") String floorId) {

        return fightingService.getDamage(questionId, floorId);
    }

    /**
     * 战斗结果（并修改下层解锁状态）
     */
    @GetMapping("/get/result")
    public Result<Integer> getResult(@RequestParam(value = "floorId") String floorId, @RequestParam(value = "studentId") String studentId, @RequestParam(value = "towerChallengeLogId") String towerChallengeLogId) {

        return fightingService.getResult(floorId, studentId, towerChallengeLogId);
    }

    /**
     * 获取奖励（概率事件）
     */
    @GetMapping("/get/award")
    public Result<AwardVO> getAward(@RequestParam(value = "studentId") String studentId, @RequestParam(value = "floorId") String floorId) {


        return fightingService.getAward(studentId, floorId);

    }

    /**
     * 计算所需要的经验值
     */
    @GetMapping("/get/requireExp")
    public Result<Long> getRequireExp(@RequestParam(value = "studentLevel") String studentLevel) {

        return fightingService.getRequireExp(studentLevel);
    }

    /**
     * 计算升级的等级数量
     */
    @GetMapping("/get/levelAdds")
    public Result<Integer> getLevelAdds(@RequestParam(value = "studentLevel") String studentLevel, @RequestParam(value = "awardExp") String awardExp) {

        return fightingService.getLevelAdds(studentLevel, awardExp);
    }


}