package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.AdminInfoDTO;
import com.aiproject.smartcampus.pojo.dto.AdminUserDTO;

public interface InformationAdminService {
    Result deleteInformation(String userId);

    Result informationTimes();

    Result getAllResources();

    Result deleteResource(String resourceId);

    Result getAdminInfo();

    Result updateAdminInfo(AdminInfoDTO adminInfo);

    Result updateUserInfo(AdminUserDTO adminUserDTO);

    Result getHighFrequencyErrorPoints();
}
