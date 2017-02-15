(ns gaode-search.core
   (:require [ajax.core :as ajax]
             [domina.core :refer [by-id value set-value! add-class! remove-class!]]
             [bootstrap.ui :as ui]
             [gaode-map.core :as gaode]
             [reagent.core :as r]))

(defn search-gaode-stations
  [pcode station attr gs-atom]
  (ajax/POST "/gaode-stations"
             {:params {:pcode pcode :station station :attr attr}
              :response-format :json
              :format :json
              :keywords? true
              :handler (fn [response]
                         (let [status (response :status)
                               coll (response :result)]
                           (reset! gs-atom
                                   {:columns ["ID" "省份编码" "属性" "坐标"]
                                    :rows (for [{:keys [id pcode station attr lng lat]} coll] [id pcode station attr (array lng lat)])
                                    :rows-cls {}})
                           (doseq [{:keys [station attr lng lat]} coll]
                             (gaode/create-marker (str station attr) nil (array lng lat)))))
              :error-handler (fn [{:keys [status status-text]}] (reset! gs-atom nil))}))

(defn gaode-station-submit
  [gaode-stations-atom]
  (fn [e]
    (let [pcode (value (by-id "gaode-pcode"))
          station (value (by-id "gaode-station"))
          attr (value (by-id "gaode-attr"))
          validate (fn [v id] (if (and v (not-empty v)) true (do (add-class! (by-id id) "has-error") false)))]
      (if (or
           ;;(validate pcode "input-gaode-pcode")
           (validate station "input-gaode-station")
           (validate attr "input-gaode-attr"))
        (do
         (search-gaode-stations pcode station attr gaode-stations-atom)
         (map #(remove-class! (by-id %) "has-error") ["input-gaode-pcode" "input-gaode-station" "input-gaode-attr"])
         (.reset (by-id "gaode-search-form"))
         (.preventDefault e))
        (.preventDefault e)))))

(defn gaode-station-form
  [gaode-stations-atom]
  (ui/form-horizontal
    {:id "gaode-search-form" :action "#" :elements
     [[ui/input-element {:id "gaode-pcode" :name "pcode" :type "text" :placeholder "省份编码" :label "省份"}]
      [ui/input-element {:id "gaode-station" :name "station" :type "text" :placeholder "收费站" :label "收费站"}]
      [ui/input-element {:id "gaode-attr" :name "attr" :type "text" :placeholder "属性" :label "属性"}]
      [ui/button-element {:type "submit" :value "查询" :on-click (gaode-station-submit gaode-stations-atom)}]]}))

(def init-gaode-map (with-meta ui/gaode-map
                               {:component-did-mount #(gaode/create-map "map")}))


(defn gaode-station-page
  [gs-atom]
  [:div
   [:div.row
    [:div.col-lg-3
     [ui/title-panel {:title "高德收费站查询" :class "panel-success" :body [gaode-station-form gs-atom]}]]
    [:div.col-lg-9
     [init-gaode-map]]]
   [:div.row
    [:div.col-lg-12
     (when @gs-atom
       [ui/title-panel
        {:title "收费站数据"
         :class "panel-success"
         :body [ui/table @gs-atom]}])]]])
