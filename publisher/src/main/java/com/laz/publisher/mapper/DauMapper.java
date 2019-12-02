package com.laz.publisher.mapper;

import java.util.List;
import java.util.Map;


public interface DauMapper {
    //求日活总数
    public long getDauTotal(String date);

    //求日活的分时数
    public List<Map> getDauHour(String date);

}
