package com.aiproject.smartcampus.contest;

/**
 * @program: SmartCampus
 * @description: 常量池
 * @author: lk
 * @create: 2025-05-17 17:44
 **/

public class CommonContest {

    public static final int REFRESH_TIEM=3;
    public static final String TOOL_SCAN_NAME="com.aiproject.smartcampus.functioncalling.";
    public static final String MAPPER_SCAN_NAME="com.aiproject.smartcampus.mapper.";
    public static final long TOKEN_PRE_TTL=3l;
    public static final Integer REDO_SUM=5;
    public static final long   ttl = TOKEN_PRE_TTL * 60 * 60 * 24;
    public static final String LOGIN_KEY = "smartcampus:login:";
    //todo 后续可以进行添加违法关键词
    public static  String[] illegalContentList={
            "赌博",      // 赌博相关
            "暴力",      // 暴力相关
            "涉政内容",  // 涉及政治敏感
            "恐怖组织",  // 恐怖主义
            "毒品",      // 毒品相关
            "不良信息",  // 泛指不良内容
            "黄色网站",  // 色情内容
            "诈骗信息",  // 网络诈骗
            "非法集资",  // 金融类违法
            "枪支弹药"   // 管制物品
    };

}
