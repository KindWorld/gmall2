package com.laz.publisher.service.impl;

import com.laz.publisher.mapper.DauMapper;
import com.laz.publisher.service.PublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class PublisherServiceImpl implements PublisherService {

    @Autowired
    DauMapper dauMapper;

    @Override
    public Long getDauTotal(String date) {
        Long dauTotal=dauMapper.getDauTotal(date);
        return dauTotal;
    }

    @Override
    public Map getDauHour(String date) {
        List<Map> dauHourList = dauMapper.getDauHour(date);
        Map dauHourMap = new HashMap<>();
        for (Map map:dauHourList){
            dauHourMap.put(map.get("loghour"),map.get("ct"));
        }
        return dauHourMap;
    }
}
