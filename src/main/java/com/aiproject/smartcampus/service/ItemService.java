package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.Item;
import org.apache.poi.ss.formula.functions.T;

import java.util.List;

public interface ItemService {
    Result<List<Item>> getIllustratedBook();

    Result<List<Item>> getUserItemInfo(Integer studentId);

    Result<Integer> getUserItemNum(Integer itemId, Integer studentId);

    Result<T> userUseItem(Integer itemId, Integer studentId, Integer floorId, Integer changeCount, Integer max_HP);
}
