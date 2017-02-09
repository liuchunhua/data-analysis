(ns gaode-map.core
  (:require [goog.dom :as dom]
            [reagent.core :as r]
            [ajax.core :as ajax]
            [domina.core :refer [by-id value set-value! add-class! remove-class!]]
            [bootstrap.ui :as ui])
  (:require-macros [gaode-map.core :refer [driving-plan]]))

(defonce state
  (r/atom {}))

(extend-type js/NodeList
  ISeqable
  (-seq [node-list] (array-seq node-list)))

(defn create-map
  [id]
  (let [div (dom/getElement id)
        ops #js {:resizeEnable true, :center #js [116.397428, 39.90923], :zoom 10}
        map (js/AMap.Map. div ops)]
    (do
      (.addDomListener js/AMap.event (by-id "btn-map-clear") "click" (fn [] (.clearMap map)) false)
      map)))


(defn destroy-map
  []
  (->> (array-seq (dom/getElementsByTagNameAndClass "div" "amap-container"))
       (map dom/removeNode)))


(defn search-driving-path
  [p1,p2]
  (do
    (if-not (@state :map) (swap! state assoc :map (create-map "map")))
    (driving-plan (let [driving (js/AMap.Driving. #js {:map (@state :map) :hideMarkers true})]
                    (do (swap! state assoc :driving driving)(.search driving p1 p2)) nil))))
(defn create-marker
  [title icon position map]
  (let [url (or icon "http://webapi.amap.com/theme/v1.3/markers/n/mark_b.png")
        marker (js/AMap.Marker. #js {:icon url :position position})]
    (do
      (.setTitle marker title)
      (.setMap marker map)
      nil)))

(defn start-marker
  [title position]
  (let [start "http://webapi.amap.com/theme/v1.3/markers/n/start.png"]
    (create-marker title start position (@state :map))))

(defn end-marker
  [title position]
  (let [end "http://webapi.amap.com/theme/v1.3/markers/n/end.png"]
    (create-marker title end position (@state :map))))



