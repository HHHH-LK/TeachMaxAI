import apiClient from "@/utils/https";

export const gameService = {
    towerRanking: {
        /**
         * 获取指定课程的塔防排行榜（分榜）
         * @param {string} courseId - 课程ID
         * @returns {Promise<object>} 排行榜数据
         */
        getTowerRankingByCourse: async (courseId) => {
            return await apiClient.get('/rankinglist/getTowerSortByCourseId', {
                params: { courseId }
            });
        },

        /**
         * 获取总排行榜（总榜）
         * @returns {Promise<object>} 总排行榜数据
         */
        getTotalRanking: async () => {
            return await apiClient.get('/rankinglist/getTotleSort');
        },

        /**
         * 获取玩家在总榜中的详细信息
         * @param {string} studentId - 学生ID
         * @returns {Promise<object>} 玩家总榜详细信息
         */
        getUserTotalTowerInfo: async (studentId) => {
            return await apiClient.get('/rankinglist/getUserTotalTowerInfo', {
                params: { studentId }
            });
        },

        /**
         * 获取玩家在指定塔中的排行榜信息
         * @param {string} studentId - 学生ID
         * @param {string} towerId - 塔ID
         * @returns {Promise<object>} 玩家在指定塔中的排行信息
         */
        getUserTowerSortUserInfo: async (studentId, towerId) => {
            return await apiClient.get('/rankinglist/getUserTowerSortUserInfo', {
                params: { studentId, towerId }
            });
        },
    },

    gameUser: {
        /**
         * 查询玩家的基础信息
         * @param {string} studentId - 学生ID
         * @returns {Promise<object>} 玩家基础信息
         */
        getPlayerInfo: async (studentId) => {
            return await apiClient.get('/player/get/gameplayerInfo', {
                params: { studentId }
            });
        },

        /**
         * 新建游戏角色
         * @param {object} gameUser - 游戏角色信息
         * @returns {Promise<object>} 创建结果
         */
        createPlayer: async (gameUser) => {
            return await apiClient.post('/player/insert/gameplayerInfo', gameUser);
        },

        /**
         * 获取游戏角色等级（通过经验计算）
         * @param {string} studentId - 学生ID
         * @returns {Promise<object>} 角色等级
         */
        getPlayerLevel: async (studentId) => {
            return await apiClient.post('/player/insert/gameuserExp', null, {
                params: { studentId }
            });
        },

        /**
         * 修改游戏昵称
         * @param {string} name - 新的游戏昵称
         * @param {string} gameUserId - 游戏用户ID
         * @returns {Promise<object>} 修改结果
         */
        updateGamePlayerName: async (name, gameUserId) => {
            return await apiClient.post('/player/update/gameUserName', null, {
                params: { name, gameUserId }
            });
        }
    },

    items: {
        /**
         * 查看道具图鉴
         * @returns {Promise<object>} 道具图鉴列表
         */
        getIllustratedBook: async () => {
            return await apiClient.get('/item/get/IllustratedBook');
        },

        /**
         * 查看玩家的道具
         * @param {number} studentId - 学生ID
         * @returns {Promise<object>} 玩家拥有的道具列表
         */
        getStudentItems: async (studentId) => {
            return await apiClient.get('/item/get/studentItemInfo', {
                params: { studentId }
            });
        },

        /**
         * 查看道具数量
         * @param {number} itemId - 道具ID
         * @param {number} studentId - 学生ID
         * @returns {Promise<object>} 道具数量
         */
        getItemQuantity: async (itemId, studentId) => {
            return await apiClient.get('/item/get/itemnum', {
                params: { itemId, studentId }
            });
        },

        /**
         * 使用道具
         * @param {object} useItemData - 使用道具的参数
         * @returns {Promise<object>} 使用结果
         */
        useItem: async (useItemData) => {
            const { itemId, studentId, floorId, changeCount, max_HP } = useItemData;
            return await apiClient.post('/item/User/useitem', null, {
                params: { itemId, studentId, floorId, changeCount, max_HP }
            });
        }
    },

    fighting: {
        /**
         * 进入回合战斗(返回当前用户挑战塔层Id,并将boss，用户血 量缓存进redis中供后续查询)
         * @param {string} floorId - 塔层ID
         * @param {string} studentId - 学生ID
         * @returns {Promise<object>} 挑战塔层Id
         */
        startFighting: async (floorId, studentId) => {
            return await apiClient.post('/fighting/start/fighting', null, {
                params: { floorId, studentId }
            });
        },

        /**
         * 获取当前回合战斗日志
         * @param {string} floorId - 塔层ID
         * @param {string} towerChallengeLogId - 挑战日志ID
         * @param {string} studentId - 学生ID
         * @returns {Promise<object>} 战斗日志信息
         */
        getCurrentBattleLog: async (floorId, towerChallengeLogId, studentId) => {
            return await apiClient.get('/fighting/get/currentBattleLog', {
                params: { floorId, towerChallengeLogId, studentId }
            });
        },

        /**
         * 查看战斗进度(获取当前用户血量以及怪物血量)
         * @param {string} studentId - 学生ID
         * @param {string} towerChallengeLogId - 挑战日志ID
         * @returns {Promise<object>} 用户和Boss血量信息
         */
        getGameUserHPandbossHP: async (studentId, towerChallengeLogId) => {
            return await apiClient.get('/fighting/get/gameUserHPandbossHP', {
                params: { studentId, towerChallengeLogId }
            });
        },

        /**
         * 玩家攻击（获取题目进行答题）
         * @param {string} studentId - 学生ID
         * @param {string} towerChallengeLogId - 挑战日志ID
         * @returns {Promise<object>} 题目信息
         */
        userAttack: async (studentId, towerChallengeLogId) => {
            return await apiClient.post('/fighting/user/attack', null, {
                params: { studentId, towerChallengeLogId }
            });
        },

        /**
         * 检查答案是否正确并处理结果（扣血）
         * @param {string} studentId - 学生ID
         * @param {string} questionId - 题目ID
         * @param {string} context - 答案内容
         * @param {string} floorId - 塔层ID
         * @param {string} changeCount - 挑战次数
         * @returns {Promise<object>} 回合结果信息
         */
        checkAnswerIsTrue: async (studentId, questionId, context, floorId, changeCount) => {
            return await apiClient.post('/fighting/user/answerquestion', null, {
                params: { studentId, questionId, context, floorId, changeCount }
            });
        },

        /**
         * 获取用户当前挑战次数
         * @param {string} studentId - 学生ID
         * @param {string} floorId - 塔层ID
         * @returns {Promise<object>} 挑战次数
         */
        getUserChangeCount: async (studentId, floorId) => {
            return await apiClient.get('/fighting/user/changeCount', {
                params: { studentId, floorId }
            });
        },

        /**
         * 获取攻击伤害值
         * @param {string} questionId - 题目ID
         * @param {string} floorId - 塔层ID
         * @returns {Promise<object>} 伤害值
         */
        getDamage: async (questionId, floorId) => {
            return await apiClient.get('/fighting/get/damage', {
                params: { questionId, floorId }
            });
        },

        /**
         * 获取战斗结果（并修改下层解锁状态）
         * @param {string} floorId - 塔层ID
         * @param {string} studentId - 学生ID
         * @param {string} towerChallengeLogId - 挑战日志ID
         * @returns {Promise<object>} 战斗结果
         */
        getResult: async (floorId, studentId, towerChallengeLogId) => {
            return await apiClient.get('/fighting/get/result', {
                params: { floorId, studentId, towerChallengeLogId }
            });
        },

        /**
         * 获取奖励（概率事件）
         * @param {string} studentId - 学生ID
         * @param {string} floorId - 塔层ID
         * @returns {Promise<object>} 奖励信息
         */
        getAward: async (studentId, floorId) => {
            return await apiClient.get('/fighting/get/award', {
                params: { studentId, floorId }
            });
        },

        /**
         * 获取升级所需经验
         */
        getExp: async(studentLevel) => {
            return await apiClient.get('/fighting/get/requireExp', {
                params: {studentLevel}
            });
        }
    },

    tower: {
        /**
         * 根据学生和课程定制化生成战斗塔（提前）
         * @param {string} studentId - 学生ID
         * @param {string} courseId - 课程ID
         * @returns {Promise<object>} 创建结果
         */
        createTowerByAgent: async (studentId, courseId) => {
            return await apiClient.post('/tower/createByAgent', null, {
                params: { studentId, courseId }
            });
        },

        /**
         * 获取学生的所有塔列表
         * @param {string} studentId - 学生ID
         * @returns {Promise<object>} 塔列表
         */
        getTowerByStudentId: async (studentId) => {
            return await apiClient.get('/tower/getTowerBystudentId', {
                params: { studentId }
            });
        },

        /**
         * 获取对应塔下的所有塔层信息
         * @param {string} towerId - 塔ID
         * @returns {Promise<object>} 塔层信息列表
         */
        getTowerFloorByTowerId: async (towerId) => {
            return await apiClient.get('/tower/getTowerInfoByTowerId', {
                params: { towerId }
            });
        },

        /**
         * 查询主塔的背景故事
         * @param {string} towerId - 塔ID
         * @returns {Promise<object>} 塔背景故事
         */
        getTowerStoryByTowerId: async (towerId) => {
            return await apiClient.get('/tower/getTowerStoryByTowerId', {
                params: { towerId }
            });
        },

        /**
         * 查询塔层的故事情节
         * @param {string} towerFloorId - 塔层ID
         * @returns {Promise<object>} 塔层故事情节
         */
        getTowerFloorStoryByTowerFloorId: async (towerFloorId) => {
            return await apiClient.get('/tower/getTowerFloorStoryByTowerFloorId', {
                params: { towerFloorId }
            });
        },

        /**
         * 进入学生对应的塔层(根据towerFloorId查询塔层信息)
         * @param {string} towerFloorId - 塔层ID
         * @returns {Promise<object>} 塔层信息
         */
        getTowerFloorInfoByFloorId: async (towerFloorId) => {
            return await apiClient.get('/tower/getTowerFloorInfoByFloorId', {
                params: { towerFloorId }
            });
        },

        /**
         * 修改塔层状态(是否通过)
         * @param {string} towerFloorId - 塔层ID
         * @param {number} isPass - 是否通过(0:未通过,1:通过)
         * @returns {Promise<object>} 修改结果
         */
        setIsPass: async (towerFloorId, isPass) => {
            return await apiClient.post('/tower/setIsPass', null, {
                params: { towerFloorId, isPass }
            });
        },

        /**
         * 进入塔层时异步加载对应的题目（存入redis中）并定时同步
         * @param {string} towerId - 塔ID
         * @param {string} floorId - 塔层ID
         * @param {string} courseId - 课程ID
         * @param {string} studentId - 学生ID
         * @returns {Promise<object>} 加载结果
         */
        loadTest: async (towerId, floorId, courseId, studentId) => {
            return await apiClient.post('/tower/load', null, {
                params: { towerId, floorId, courseId, studentId }
            });
        }
    },

    monster: {
        /**
         * 获取boss信息
         * @param {string} floorId - 塔层ID
         * @returns {Promise<object>} Boss信息
         */
        getBossInfo: async (floorId) => {
            return await apiClient.get('/boss/get/bossInfo', {
                params: { floorId }
            });
        }
    }
}