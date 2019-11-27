package com.laz.app

import java.text.SimpleDateFormat
import java.util.Date

import com.alibaba.fastjson.JSON
import com.laz.constant.GmallConstant
import com.laz.util.{MyKafkaUtil, RedisUtil}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis

object DauApp {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setMaster("local[*]").setAppName("dauapp")
    val ssc = new StreamingContext(conf, Seconds(5))
    /*1.消费kafka*/
    val inputDstream = MyKafkaUtil.getKafkaStream(GmallConstant.KAFKA_TOPIC_STARTUP, ssc)
    /*测试是否能联通kafka
    inputDstream.foreachRDD(_.map(_.value()).collect().foreach(println))*/
    /*2.数据流 map转换结构编程case class样例类补充俩个时间字段*/
    val startUpLogDstream = inputDstream.map { record => {
      val jsonStr = record.value()
      val startUpLog = JSON.parseObject(jsonStr, classOf[StartUpLog])
      // 转换时间格式
      val dateTimeStr = new SimpleDateFormat("yyyy-MM-dd HH").format(new Date(startUpLog.ts))
      val dateArr = dateTimeStr.split(" ")
      startUpLog.logDate = dateArr(0)
      startUpLog.logHour = dateArr(1)
      startUpLog
    }
    }
    //缓存
    startUpLogDstream.cache()
    //3 利用redis set用户清单进行过滤去重,只保留清单中不存在的用户访问记录
    ///  .....  driver 周期性的查询redis的清单   通过广播变量发送到executor中
    val filteredDstream = startUpLogDstream.transform {
      rdd => {
        val jedis = new Jedis("hadoop102",6379)
        val date = new SimpleDateFormat("yyyy-MM-dd").format(new Date())
        val key = "dau:" + date
        val dauMidSet = jedis.smembers(key)
        jedis.close()
        val bc = ssc.sparkContext.broadcast(dauMidSet)
        println("过滤前：" + rdd.count())
        //executor 根据广播变量 比对 自己的数据  进行过滤
        val filteredRDD = rdd.filter { startuplog => {
          val set = bc.value
          !set.contains(startuplog.mid)
        }
        }
        println("过滤后：" + filteredRDD.count())
        filteredRDD
      }
    }
    /*4.此时只是批次之间没有重复还需要批次内去重*/
    //  利用redis无法取出 一个批次内的数据 所以 每个 批次要做自查 内部去重 ， 方法：  用mid 进行分组 ，取每组第一
    val startupGroupbyMidDstream: DStream[(String, Iterable[StartUpLog])] = filteredDstream.map(startuplog=>(startuplog.mid,startuplog)).groupByKey()

    val filtered2Dstream: DStream[StartUpLog] = startupGroupbyMidDstream.flatMap { case (mid, startuplogItr) =>
      val sortList: List[StartUpLog] = startuplogItr.toList.sortWith { (startuplog1, startuplog2) =>
        startuplog1.ts < startuplog2.ts
      }
      val top1LogList: List[StartUpLog] = sortList.take(1)
      top1LogList
    }

    /*保存
    * redis保存*/
    //保存
    // redis  type :set      key    dau:2019-11-26    value:  mid
    //set 的写入
    filtered2Dstream.foreachRDD{rdd=>
      //driver
      rdd.foreachPartition{ startupLogItr=>
        // executor
        val jedis = new Jedis("hadoop102",6379)
        for (startuplog <- startupLogItr ) {
          println(startuplog)

          val dateKey: String ="dau:"+startuplog.logDate  //executor
          jedis.sadd(dateKey,startuplog.mid)
        }
        jedis.close()

      }

    }

    /*启动*/
    ssc.start()
    /*等待进程停止或者手动停止*/
    ssc.awaitTermination()
  }
}

case class StartUpLog(mid: String,
                      uid: String,
                      appid: String,
                      area: String,
                      os: String,
                      ch: String,
                      logType: String,
                      vs: String,
                      var logDate: String,
                      var logHour: String,
                      var ts: Long
                     ) {

}

