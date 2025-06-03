package com.aiproject.smartcampus.model.intent.router;


import com.aiproject.smartcampus.model.intent.handler.Handler;

import java.util.List;
import java.util.Map;

public interface StepRouter {

    //意图路由
    String route(List<String> intent);


}
