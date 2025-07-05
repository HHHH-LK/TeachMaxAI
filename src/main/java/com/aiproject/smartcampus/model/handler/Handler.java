package com.aiproject.smartcampus.model.handler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface Handler {

    //执行 并将结果和意图关联起来
    String run(String intent, List<CompletableFuture<String>> result) throws ExecutionException, InterruptedException, TimeoutException;


}
