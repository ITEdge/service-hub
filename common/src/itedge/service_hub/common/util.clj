(ns itedge.service-hub.common.util)

(defn update-map-keys-values [m f]
  "Updates map keys and values with provided fn"
  (into {} (for [[k v] m] [k (f k v)])))

(defn update-if-in
  "Same as core update-in, but returns original map if key is not present, works only in one level"
  [m k fn]
  (if (k m)
    (update-in m [k] fn)
    m))
