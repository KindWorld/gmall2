package com.laz.publisher.service;

import java.util.Map;

public interface PublisherService {

    public Long getDauTotal(String date);

    public Map getDauHour(String date);

    public Double getOrderAmount(String date);

    public Map getOrderAmountHourMap(String date);
}
