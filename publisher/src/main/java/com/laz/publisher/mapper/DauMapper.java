package com.laz.publisher.mapper;

import java.util.List;
import java.util.Map;


public interface DauMapper {

    public  long getDauTotal(String date);

    public List<Map> getDauHour(String date);

}
