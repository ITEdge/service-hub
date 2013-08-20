(ns itedge.service-hub.core.date-time-util
  (:import java.sql.Timestamp
           org.joda.time.DateTime
           org.joda.time.format.ISODateTimeFormat
           org.joda.time.Duration))

(defn timestamp-to-datetime
  "Converts java.sql.Timestamp object to org.joda.time.DateTime object"
  [Timestamp]
  (DateTime. (.getTime Timestamp)))

(defn datetime-to-timestamp
  "Converts org.joda.time.DateTime object to java.sql.Timestamp object"
  [DateTime]
  (Timestamp. (.getMillis DateTime)))

(defn datetime-to-iso-8061
  "Converts org.joda.time.DateTime object to ISO-8061 formatted string"
  [DateTime]
  (.print (ISODateTimeFormat/dateTime) DateTime))

(defn get-duration-in-minutes
  "Gets duration in minutes between start and end times"
  [start end]
  (.getStandardMinutes (Duration. start end)))

(defn iso-8061-to-datetime
  "Converts ISO-8061 formatted string to org.joda.time.DateTime object"
  [String]
  (DateTime. String))

(defn iso-8061?
  "Checks if string is compatible with ISO-8061 format"
  [iso-string]
  (try
    (do (DateTime. iso-string)
      true)
    (catch IllegalArgumentException e false)))

(defn get-current-date
  "Gets current time"
  []
  (DateTime.))
