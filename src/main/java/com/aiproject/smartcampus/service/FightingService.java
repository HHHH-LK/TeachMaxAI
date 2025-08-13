package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.BattleLog;
import com.aiproject.smartcampus.pojo.po.QuestionBank;
import com.aiproject.smartcampus.pojo.vo.AwardVO;
import org.apache.poi.ss.formula.functions.T;

import java.util.Map;

public interface FightingService {
    Result<BattleLog> getCurrentBattleLog(String floorId, String towerChallengeLogId,String studentId);

    Result<Long> startFighting(String floorId, String studentId);

    Result<Map<String, String>> getgetGameUserHPandbossHP(String studentId, String towerChallengeLogId);

    Result<QuestionBank> userAttack(String studentId, String towerChallengeLogId);

    Result<Map<String,String>> checkAswerIsTure(String studentId, String questionId, String context,String floorId,String towerChallengeCount);

    Result<Integer> getUserChangeCount(String studentId, String floorId);

    Result<Integer> getDamage(String questionId, String floorId);

    Result<Integer> getResult(String floorId, String studentId, String towerChallengeLogId);

    Result<AwardVO> getAward(String studentId, String floorId);
}
