package com.laz.publisher.mapper;

import java.util.List;
import java.util.Map;

public interface OrderMapper {
    //求单日交易总金额
    public Double getOrderAmount(String date);

    //求单日分时总金额
    public List<Map> getOrderAmountHour(String date);
}
