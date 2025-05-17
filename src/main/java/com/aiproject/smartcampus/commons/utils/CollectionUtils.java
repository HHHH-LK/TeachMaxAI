package com.aiproject.smartcampus.commons.utils;

import java.util.List;

/**
 * @program: lecture-langchain-20250525
 * @description: 集合工具类
 * @author: lk
 * @create: 2025-05-11 11:03
 **/

public class CollectionUtils {
    public static Boolean isEmpty(List collection) {
        if (collection == null || collection.isEmpty()) {
            return true;
        }
        return false;
    }


}
