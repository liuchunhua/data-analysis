(ns car-path.core
  ; (:import [goog.ui InputDatePicker]
  ;          [goog.i18n DateTimeFormat DateTimeParse]
  ;          [goog.date Date])
  (:require [bootstrap.ui :as ui]
            [gaode-map.core :as gaode]
            [ajax.core :as ajax]
            [domina.core :refer [by-id value set-value! add-class! remove-class!]]))

(declare post-ajax form-submit)

(defn car-path-search-form
  [car-path-atom]
  (fn []
    [ui/title-panel
     {:title "轨迹查询"
      :body [ui/form-inline
             {:id "car-path-search-form"
              :elements
              [[ui/input-element {:id "car-path-cardno" :type "input" :placeholder "卡号"}]
               [ui/input-element {:id "car-path-start" :type "date" :placeholder "开始日期"}]
               [ui/input-element {:id "car-path-end" :type "date" :placeholder "结束日期"}]
               [ui/button-element {:type "button" :value "查询" :on-click (form-submit car-path-atom)}]]}]}]))

(defn form-submit
  [car-path-atom]
  (fn [e]
    (.preventDefault e)
    (let [[cardno start end] (map #(value (by-id %)) ["car-path-cardno" "car-path-start" "car-path-end"])
          validate (fn [v id] (if (and v (not-empty v)) true (do (add-class! (by-id (str "input-" id)) "has-error") false)))]
      (if (and (validate cardno "car-path-cardno") (validate start "car-path-start") (validate end "car-path-end"))
        (post-ajax cardno start end car-path-atom)
        (js/alert "请完成输入")))))

(defn post-ajax
  [cardno start end car-path-atom]
  (ajax/POST "/car-path"
             {:params {:cardno cardno :start start :end end}
              :response-format :json
              :format :json
              :keywords? true
              :handler (fn [response]
                         (let [status (response :status)
                               coll (response :result)]
                           (reset! car-path-atom
                                   {:columns ["车牌号" "入站" "入站时间" "入站名" "入口坐标" "出站" "出站时间" "出站名" "出口坐标"]
                                    :rows (vec (for [{:keys [dest_name_etc dest_longitude car_number origin_latitude
                                                             dest_latitude dest_name origin_name_etc origin_name
                                                             origin_longitude out_time in_time]} coll]
                                                 [car_number origin_name_etc in_time origin_name (array origin_longitude origin_latitude)
                                                  dest_name_etc out_time dest_name (array dest_longitude dest_latitude)]))
                                    :rows-cls {}})))
              :error-handler (fn [{:keys [status status-text]}] (reset! car-path-atom nil))}))


(def init-gaode-map (with-meta ui/gaode-map
                               {:component-did-mount #(gaode/create-map "map")}))
; (def init-search-form (with-meta car-path-search-form
;                                  {:component-did-mount #(let [PATTERN "yyyy-MM-dd"
;                                                               formatter (DateTimeFormat. PATTERN)
;                                                               parser (DateTimeParse. PATTERN)
;                                                               start-picker (InputDatePicker. formatter parser)
;                                                               end-picker (InputDatePicker. formatter parser)]
;                                                           (do
;                                                            (doto start-picker  (.decorate (by-id "car-path-start")))
;                                                            (doto end-picker  (.decorate (by-id "car-path-end")))))}))


(defn car-path-page
  [car-path-atom]
  [:div
   [car-path-search-form car-path-atom]
   [init-gaode-map]
   (when @car-path-atom
     (ui/title-panel
      {:title "车辆轨迹"
       :body [ui/table (merge @car-path-atom
                              {:on-dbclick
                               (fn [i]
                                 (let [row (-> (@car-path-atom :rows) (get i))
                                       p1 (row 4)
                                       p2 (row 8)
                                       t1 (row 3)
                                       t2 (row 7)]
                                   (do
                                    (gaode/search-driving-path p1 p2)
                                    (gaode/start-marker t1 p1)
                                    (gaode/end-marker t2 p2)
                                    (swap! car-path-atom assoc-in [:rows-cls i] "success")
                                    nil)))})]}))])
