package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.AdminInfoDTO;
import com.aiproject.smartcampus.pojo.dto.AdminUserDTO;
import com.aiproject.smartcampus.service.InformationAdminService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/admin")
@RequiredArgsConstructor
public class InfomationAdminController {
    private final InformationAdminService informationAdminService;
    /**
     * 删除信息
     */
    @DeleteMapping("/deleteInformation/{userId}")
    @Operation(summary = "删除用户信息", description = "根据id删除信息")
    public Result deleteInformation(@PathVariable String userId) {
        return informationAdminService.deleteInformation(userId);
    }

    /**
     * 使用次数统计
     */
    @PostMapping("/Information")
    @Operation(summary = "使用次数统计", description = "统计使用次数")
    public Result informationTimes() {
        return informationAdminService.informationTimes();
    }

    /**
     * 获取全部资源
     */
    @PostMapping("/getAllResources")
    @Operation(summary = "获取全部资源", description = "获取所有资源信息")
    public Result getAllResources() {
        return informationAdminService.getAllResources();
    }

    /**
     * 删除资源
     */
    @DeleteMapping("/deleteResource/{resourceId}")
    @Operation(summary = "删除资源", description = "根据资源ID删除资源")
    public Result deleteResource(@PathVariable String resourceId) {
        return informationAdminService.deleteResource(resourceId);
    }

    /**
     * 查询管理员个人信息
     */
    @GetMapping("/getAdminInfo")
    @Operation(summary = "查询管理员个人信息", description = "获取当前管理员的个人信息")
    public Result getAdminInfo() {
        return informationAdminService.getAdminInfo();
    }

    /**
     * 更改管理员个人信息
     */
    @PutMapping("/updateAdminInfo")
    @Operation(summary = "更改管理员个人信息", description = "更新当前管理员的个人信息")
    public Result updateAdminInfo(@RequestBody AdminInfoDTO adminInfo) {
        return informationAdminService.updateAdminInfo(adminInfo);
    }

    /**
     * 更改用户信息
     */
    @PutMapping("/updateUserInfo")
    @Operation(summary = "更改用户信息", description = "更新所有用户的信息")
    public Result updateUserInfo(@RequestBody AdminUserDTO adminUserDTO) {
        return informationAdminService.updateUserInfo(adminUserDTO);
    }

    /**
     * 获取高频错误知识点
     */
    @GetMapping("/getHighFrequencyErrorPoints")
    @Operation(summary = "获取高频错误知识点", description = "获取所有高频错误知识点")
    public Result getHighFrequencyErrorPoints() {
        return informationAdminService.getHighFrequencyErrorPoints();
    }
}
