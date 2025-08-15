package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.service.impl.RankingListServiceImpl;

public interface RankingListService {
    Result<RankingListServiceImpl.SortUserInfo> getUserTowerInfo(String studentId);

    Result<RankingListServiceImpl.SortUserInfo> getUserTowerSortUserInfo(String studentId, String towerId);
}
