(ns station-search.core
  (:import [goog History])
  (:require [reagent.core :as r]
            [ajax.core :as ajax]
            [domina.core :refer [by-id value set-value! add-class! remove-class!]]
            [bootstrap.ui :as ui]
            [gaode-map.core :as gaode]))

(defn search-stations
  [pcode instation outstation ss-atom]
  (ajax/POST "/query/station"
             {:params {:pcode pcode :instation instation :outstation outstation}
              :response-format :json
              :format :json
              :keywords? true
              :handler (fn [response]
                         (let [status (response :status) [{:keys [inname inattr outname outattr in_lng in_lat out_lng out_lat]}] (response :result)]
                           (when (= "OK" status)
                             (do
                               (gaode/search-driving-path (array in_lng in_lat) (array out_lng out_lat))
                               (gaode/start-marker (str inname inattr) (array in_lng in_lat))
                               (gaode/end-marker (str outname outattr) (array out_lng out_lat))
                               (swap! ss-atom assoc :route (first (response :result)))))))
              :error-handler (fn [{:keys [status status-text]}] (swap! ss-atom assoc :route nil))})
  (ajax/POST "/available-stations"
             {:params {:pcode pcode :instation instation :outstation outstation}
              :response-format :json
              :format :json
              :keywords? true
              :handler (fn [response]
                         (let [status (response :status)
                               road (get-in response [:result :road])
                               station (fn [m k] (str (get-in m [k :station]) (get-in m [k :attr])))
                               poi (fn [m k] (array (get-in m [k :lng]) (get-in m [k :lat])))]
                           (swap! ss-atom assoc
                                  :available-stations
                                  {:columns ["入口"  "入口坐标" "出口" "出口坐标"]
                                   :rows (mapv #(vector (station % :in) (poi % :in) (station % :out) (poi % :out)) road)
                                   :rows-cls {}})))
              :error-handler (fn [{:keys [status status-text]}] (swap! ss-atom assoc :available-stations nil))}))

(defn station-form-submit
  [ss-atom]
  (fn [e]
    (let [pcode (value (by-id "pcode"))
          instation (value (by-id "instation"))
          outstation (value (by-id "outstation"))
          validate (fn [v id] (if (and v (not-empty v)) true (do (add-class! (by-id id) "has-error") false)))]
      (if (and
           (validate pcode "input-pcode")
           (validate instation "input-instation")
           (validate outstation "input-outstation"))
        (do
         (search-stations pcode instation outstation ss-atom)
         (map #(remove-class! (by-id %) "has-error") ["input-pcode" "input-instation" "input-outstation"])
         (.reset (by-id "search-form"))
         (.preventDefault e))
        (.preventDefault e)))))

(defn station-form
  [ss-atom]
  [ui/form-horizontal
   {:id "search-form" :action "#" :elements
    [[ui/input-element {:id "pcode" :name "pcode" :type "text" :placeholder "省份编码" :label "省份"}]
     [ui/input-element {:id "instation" :name "instation" :type "text" :placeholder "入站口" :label "入站"}]
     [ui/input-element {:id "outstation" :name "outstation" :type "text" :placeholder "出站口" :label "出站"}]
     [ui/button-element {:id "btn-submit" :type "submit" :value "查询" :on-click (station-form-submit ss-atom)}]]}])

(defn in-out-station
  [e]
  (if-let [{:keys [pcode instation outstation inname outname inattr outattr in_lng in_lat out_lng out_lat distance]} e]
    [ui/title-panel {:title "查询信息" :class "panel-success"
                     :body [:div
                            [:p "省份编码:" pcode]
                            [:p "入口站：" instation]
                            [:p "出口站：" outstation]
                            [:p "入口收费站：" (str inname inattr) "[" in_lng "," in_lat "]"]
                            [:p "出口收费站：" (str outname outattr) "[" out_lng "," out_lat "]"]
                            [:p "距离：" distance "米"]]}]
    [ui/title-panel {:title "路径信息" :class "panel-danger" :body [:p ""]}]))

(def init-gaode-map (with-meta ui/gaode-map
                      {:component-did-mount #(gaode/create-map "map")}))
(defn station-search-page
  [ss-atom]
  (let [{:keys [route available-stations]} @ss-atom]
   [:div
     [:div.row
      [:div.col-lg-3
       [ui/title-panel {:title "站点查询" :class "panel-success"
                        :body [:div
                               [station-form ss-atom]
                               (when route [in-out-station route])]}]]
      [:div.col-lg-9
       [init-gaode-map]]]
     [:div.row
      [:div.col-lg-12
       (when available-stations
         [ui/title-panel
          {:title "可选站点坐标" :class "panel-success"
           :body [ui/table (merge
                            available-stations
                            {:on-dbclick
                             (fn [i]
                               (let [row (-> (available-stations :rows) (get i))
                                     p1 (row 1)
                                     p2 (row 3)
                                     t1 (row 0)
                                     t2 (row 2)]
                                 (do
                                   (gaode/search-driving-path p1 p2)
                                   (gaode/start-marker t1 p1)
                                   (gaode/end-marker t2 p2)
                                   (swap! ss-atom assoc-in [:available-stations :rows-cls i] "success")
                                   nil)))})]}])]]]))
