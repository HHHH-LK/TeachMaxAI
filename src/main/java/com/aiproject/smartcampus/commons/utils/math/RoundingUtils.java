package com.aiproject.smartcampus.commons.utils.math;

import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @program: TeacherMaxAI
 * @description: 四舍五入工具
 * @author: lk_hhh
 * @create: 2025-08-07 16:36
 **/
@NoArgsConstructor
public class RoundingUtils {

    /**
     * 四舍五入保留两位小数
     */
    public static Double round(Double d) {
        if (d == null) {
            return null;
        }
        return new BigDecimal(d)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }


}