package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.LoginDTO;
import com.aiproject.smartcampus.pojo.dto.PasswordResetDTO;
import com.aiproject.smartcampus.pojo.dto.PasswordResetVerificationDTO;
import com.aiproject.smartcampus.pojo.dto.RegisterDTO;
import org.springframework.web.multipart.MultipartFile;

public interface CommonService {

    Result login(LoginDTO loginDTO);

    Result register(RegisterDTO registerDTO);

//    Result sendPasswordResetCode(PasswordResetVerificationDTO dto);

    Result resetPassword(PasswordResetDTO dto);
}
