(ns itedge.service-hub.core.test.date-time-util
  (:require [itedge.service-hub.core.date-time-util :refer :all]
            [clojure.test :refer :all])
  (:import java.sql.Timestamp 
           org.joda.time.DateTime))

(deftest test-timestamp-to-datetime
  (let [date-time (DateTime. 2013 6 28 22 15)
        millis (.getMillis date-time)]
    (is (= (timestamp-to-datetime (Timestamp. millis)) (DateTime. 2013 6 28 22 15)))))

(deftest test-datetime-to-timestamp
  (let [date-time (DateTime. 2013 6 28 22 15)
        millis (.getMillis date-time)]
    (is (= (datetime-to-timestamp date-time) (Timestamp. millis)))))

(deftest test-datetime-to-iso-8061
  (is (= (datetime-to-iso-8061 (DateTime. 2013 6 28 22 15)) "2013-06-28T22:15:00.000+02:00")))

(deftest test-get-duration-in-minutes
  (let [date-time-first (DateTime. 2013 6 28 22 15)
        date-time-second (DateTime. 2013 6 28 23 45)]
    (is (= (get-duration-in-minutes date-time-first date-time-second) 90))))

(deftest test-iso-8061-to-datetime
  (is (= (iso-8061-to-datetime "2013-06-28T22:15:00.000+02:00") (DateTime. 2013 6 28 22 15))))

(deftest test-iso-8061?
  (is (= (iso-8061? "2013-06-28T22:15:00.000+02:00") true))
  (is (= (iso-8061? "2013-06-28T22:1dd") false))
  (is (= (iso-8061? "2013-14-28T22:15:00.000+02:00") false)))

