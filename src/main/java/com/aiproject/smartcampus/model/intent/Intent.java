package com.aiproject.smartcampus.model.intent;


import java.util.List;

public interface Intent {

    String handlerIntent(String intent);

    List<String> intentSpilder(String intent);


}
