package com.laz.canal;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.laz.constant.GmallConstant;

import java.util.List;

public class CanalHandler {
    CanalEntry.EventType eventType;

    String tableName;

    List<CanalEntry.RowData> rowDataList;

    //构造器
    public CanalHandler(CanalEntry.EventType eventType, String tableName, List<CanalEntry.RowData> rowDataList) {
        this.eventType = eventType;
        this.tableName = tableName;
        this.rowDataList = rowDataList;
    }

    public void handle() {
        /*订单表的下单数据*/
        if (tableName.equals("order_info") && eventType.equals(CanalEntry.EventType.INSERT)) {
             sendToKafka(GmallConstant.KAFKA_TOPIC_ORDER);
        }
    }

    private void sendToKafka(String topic) {
        for (CanalEntry.RowData rowData : rowDataList) {
            //获取insert之后的列数据数据之前的数据只有delete
            List<CanalEntry.Column> ColumnsList = rowData.getAfterColumnsList();
            JSONObject jsonObject = new JSONObject();
            for (CanalEntry.Column column : ColumnsList) {
                System.out.println(column.getName() + "-------" + column.getValue());
                jsonObject.put(column.getName(),column.getValue());
            }
            KafkaSender.send(topic,jsonObject.toJSONString());
        }
    }
}
