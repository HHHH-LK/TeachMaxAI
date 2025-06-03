package com.aiproject.smartcampus.model.intent.handler;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Handler {

    //执行 并将结果和意图关联起来
    String run(String intent, List<CompletableFuture<String>> result);


}
