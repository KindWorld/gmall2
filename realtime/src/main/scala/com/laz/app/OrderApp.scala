package com.laz.app

import com.alibaba.fastjson.JSON
import com.laz.constant.GmallConstant
import com.laz.util.MyKafkaUtil
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.phoenix.spark._
object OrderApp {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setMaster("local[*]").setAppName("OrderConsumer")

    val ssc = new StreamingContext(conf, Seconds(5))
    val inputDstream = MyKafkaUtil.getKafkaStream(GmallConstant.KAFKA_TOPIC_ORDER, ssc)
    val orderInfoDstream = inputDstream.map(record => {
      val jsonString = record.value()
      val orderInfo = JSON.parseObject(jsonString, classOf[OrderInfo])
      /*电话脱敏 131****3123*/
      val tel1 = orderInfo.consignee_tel.splitAt(3)
      val tel2 = tel1._2.splitAt(4)
      orderInfo.consignee_tel = tel1._1 + "****" + tel2._2
      /*日期结构 改变日期 ,*/
      val dateAndTime: Array[String] = orderInfo.create_time.split(" ")
      orderInfo.create_date = dateAndTime(0)
      orderInfo.create_hour = dateAndTime(1).split(":")(0)
      //拓展:订单上增加一个字段,该订单是否是该用户首次下单 redis,mysql
      orderInfo
    })
    orderInfoDstream.foreachRDD(rdd=>
    rdd.saveToPhoenix("ORDER_INFO",Seq("ID","PROVINCE_ID", "CONSIGNEE", "ORDER_COMMENT", "CONSIGNEE_TEL", "ORDER_STATUS", "PAYMENT_WAY", "USER_ID","IMG_URL", "TOTAL_AMOUNT", "EXPIRE_TIME", "DELIVERY_ADDRESS", "CREATE_TIME","OPERATE_TIME","TRACKING_NO","PARENT_ORDER_ID","OUT_TRADE_NO", "TRADE_BODY", "CREATE_DATE", "CREATE_HOUR"),new Configuration,Some("hadoop102,hadoop103,hadoop104:2181")
      )
    )
    ssc.start()
    ssc.awaitTermination()
  }
}

case class OrderInfo(
                      id: String,
                      province_id: String,
                      consignee: String,
                      order_comment: String,
                      var consignee_tel: String,
                      order_status: String,
                      payment_way: String,
                      user_id: String,
                      img_url: String,
                      total_amount: Double,
                      expire_time: String,
                      delivery_address: String,
                      create_time: String,
                      operate_time: String,
                      tracking_no: String,
                      parent_order_id: String,
                      out_trade_no: String,
                      trade_body: String,
                      var create_date: String,
                      var create_hour: String

                    )