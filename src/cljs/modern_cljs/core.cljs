(ns modern-cljs.core
  (:require [station-search.core :as station]))

;; enable cljs to print to the JS console of the browser
(enable-console-print!)

;; print to the console
(println "Hello, World!!!!")

(station/render)
;;(gaode/search-driving-path #js [116.379028, 39.865042] #js [116.427281, 39.903719])
