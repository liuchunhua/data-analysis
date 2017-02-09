(ns gaode-map.core)


(defmacro driving-plan
  [& body]
  `(do
    (js/AMap.service "AMap.Driving" (fn [] ~@body))
    nil))
