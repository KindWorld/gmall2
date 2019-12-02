package com.laz.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.net.InetSocketAddress;
import java.util.List;

public class CanalApp {
    public static void main(String[] args) throws InvalidProtocolBufferException {
        //    1.建立与canalserver的单机连接
        CanalConnector canalConnector = CanalConnectors.newSingleConnector(new InetSocketAddress("hadoop102", 11111), "example", "", "");

        while (true) {
            /*死循环不断连接*/
            canalConnector.connect();
            //    2.订阅数据
            canalConnector.subscribe("*.*");
            //    3.抓取message=n条sql=多个行数据=多个列数据
            Message message = canalConnector.get(100);/*100条sql数据*/
            //entry是单条sql的数据
            List<CanalEntry.Entry> entries = message.getEntries();
            int size = entries.size();
            if (size == 0) {
                try {
                    //如果抓取数据为0抓取频率降低
                    System.out.println("数据为空,待机5秒");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                for (CanalEntry.Entry entry : entries) {
                    /*由于关系型数据库每次操作都有事务性操作canal抓取并不需要所以关闭*/
                    /*只有是数据集合才会操作*/
                    if (entry.getEntryType().equals(CanalEntry.EntryType.ROWDATA)) {
                        //获取压缩数据乱码
                        ByteString storeValue = entry.getStoreValue();
                        //解析压缩的数据
                        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(storeValue);
                        //获取一条sql改变的多行数据,行集
                        List<CanalEntry.RowData> rowDatasList = rowChange.getRowDatasList();
                        //头信息
                        CanalEntry.Header header = entry.getHeader();
                        //表名
                        String tableName = header.getTableName();
                        //操作类型 insert,update,delete
                        CanalEntry.EventType eventType = rowChange.getEventType();
                        CanalHandler canalHandler = new CanalHandler(eventType, tableName, rowDatasList);
                        canalHandler.handle();//业务处理
                    }

                }
            }
        }

    }
}
