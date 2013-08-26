(ns itedge.service-hub.core.date-time-util
  (:import java.sql.Timestamp
           org.joda.time.DateTime
           org.joda.time.format.ISODateTimeFormat
           org.joda.time.Duration))

(defn ^DateTime timestamp-to-datetime
  "Converts java.sql.Timestamp object to org.joda.time.DateTime object"
  [^Timestamp timestamp]
  (DateTime. (.getTime timestamp)))

(defn ^Timestamp datetime-to-timestamp
  "Converts org.joda.time.DateTime object to java.sql.Timestamp object"
  [^DateTime datetime]
  (Timestamp. (.getMillis datetime)))

(defn datetime-to-iso-8061
  "Converts org.joda.time.DateTime object to ISO-8061 formatted string"
  [^DateTime datetime]
  (.print (ISODateTimeFormat/dateTime) datetime))

(defn get-duration-in-minutes
  "Gets duration in minutes between start and end times"
  [start end]
  (.getStandardMinutes (Duration. start end)))

(defn ^DateTime iso-8061-to-datetime
  "Converts ISO-8061 formatted string to org.joda.time.DateTime object"
  [^String string]
  (DateTime. string))

(defn iso-8061?
  "Checks if string is compatible with ISO-8061 format"
  [iso-string]
  (try
    (do ^DateTime (DateTime. iso-string)
      true)
    (catch IllegalArgumentException e false)))

(defn ^DateTime get-current-date
  "Gets current time"
  []
  (DateTime.))
