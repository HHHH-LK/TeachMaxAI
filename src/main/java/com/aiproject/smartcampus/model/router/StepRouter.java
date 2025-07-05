package com.aiproject.smartcampus.model.router;


import java.util.List;

public interface StepRouter {

    //意图路由
    String route(List<String> intent);


}
