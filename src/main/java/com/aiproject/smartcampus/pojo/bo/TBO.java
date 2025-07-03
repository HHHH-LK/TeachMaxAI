package com.aiproject.smartcampus.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * @program: ss
 * @description: 权重范围（用来进行抽取）
 * @author: lk_hhh
 * @create: 2025-07-03 02:41
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TBO {
    private Double min;
    private Double max;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TBO)) {
            return false;
        }
        TBO tbo = (TBO) o;
        return Objects.equals(min, tbo.min) && Objects.equals(max, tbo.max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }

    @Override
    public String toString() {
        return "[" + min + ", " + max + ")";
    }
}
