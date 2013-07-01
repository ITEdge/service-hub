(ns itedge.service-hub.core.date-time-util
  (:import java.sql.Timestamp
           org.joda.time.DateTime
           org.joda.time.format.ISODateTimeFormat
           org.joda.time.Duration))

(defn timestamp-to-datetime
  "Converts java.sql.Timestamp object to org.joda.time.DateTime object"
  {:added "EBS 1.0"}
  [Timestamp]
  (DateTime. (.getTime Timestamp)))

(defn datetime-to-timestamp
  "Converts org.joda.time.DateTime object to java.sql.Timestamp object"
  {:added "EBS 1.0"}
  [DateTime]
  (Timestamp. (.getMillis DateTime)))

(defn datetime-to-iso-8061
  "Converts org.joda.time.DateTime object to ISO-8061 formatted string"
  {:added "EBS 1.0"}
  [DateTime]
  (.print (ISODateTimeFormat/dateTime) DateTime))

(defn get-duration-in-minutes
  "Gets duration in minutes between start and end times"
  {:added "EBS 1.0"}
  [start end]
  (.getStandardMinutes (Duration. start end)))

(defn iso-8061-to-datetime
  "Converts ISO-8061 formatted string to org.joda.time.DateTime object"
  {:added "EBS 1.0"}
  [String]
  (DateTime. String))

(defn iso-8061?
  "Checks if string is compatible with ISO-8061 format"
  {:added "EBS 1.0"}
  [iso-string]
  (try
    (do (DateTime. iso-string)
      true)
    (catch IllegalArgumentException e false)))

(defn get-current-date
  "Gets current time"
  {:added "EBS 1.0"}
  []
  (DateTime.))
