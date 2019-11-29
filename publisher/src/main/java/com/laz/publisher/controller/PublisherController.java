package com.laz.publisher.controller;

import com.alibaba.fastjson.JSON;
import com.laz.publisher.service.PublisherService;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class PublisherController {

    @Autowired
    PublisherService publisherService;

    @GetMapping("realtime-total")
    public String  getRealtimeTotal(@RequestParam("date") String date){
        Long dauTotal = publisherService.getDauTotal(date);
        List<Object> totalList = new ArrayList<>();
        Map dauMap=new HashMap();
        dauMap.put("id","dau");
        dauMap.put("name","新增日活");
        dauMap.put("value",dauTotal);
        totalList.add(dauMap);
        Map newMidMap=new HashMap();
        newMidMap.put("id","new_mid");
        newMidMap.put("name","新增设备");
        newMidMap.put("value",100);
        totalList.add(newMidMap);
        return JSON.toJSONString(totalList);
    }

    @GetMapping("realtime-hour")
    public String getRealtimeHour(@RequestParam("id") String id,@RequestParam("date") String date){
        if (id.equals("dau")){
            Map dauHourMapTD=publisherService.getDauHour(date);
            String yd=getYd(date);
            Map dauHourMapYD = publisherService.getDauHour(yd);
            Map hourMap=new HashMap();
            hourMap.put("today",dauHourMapTD);
            hourMap.put("yesterday",dauHourMapYD);
            return JSON.toJSONString(hourMap);
        }else {
            return null;
        }
    }

    private String getYd(String date) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date today = dateFormat.parse(date);
            Date yd = DateUtils.addDays(today, -1);
            return dateFormat.format(yd);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
