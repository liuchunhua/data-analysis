(ns station-search.core
  (:import [goog History])
  (:require [reagent.core :as r]
            [ajax.core :as ajax]
            [domina.core :refer [by-id value set-value! add-class! remove-class!]]
            [bootstrap.ui :as ui]
            [gaode-map.core :as gaode]
            [bidi.bidi :as b]
            [goog.events :as events]
            [goog.history.EventType :as EventType]))



(defonce state
  (r/atom {:navigation {:page :stations :params {}}}))

(def navigation-state (r/cursor state [:navigation]))

(defn url-to-nav [routes path]
  (let [{:keys [handler route-params]} (b/match-route routes path)]
    {:page handler :params route-params}))

(defn nav-to-url [routes {:keys [page params]}]
  (apply b/path-for routes page (->> params seq flatten)))

(defonce h (History.))

(defn navigate-to! [routes nav]
  (.setToken h (nav-to-url routes nav)))

(defn hook-browser-navigation! "Listen to navigation events and updates the application state accordingly."
  [routes]
  (doto h
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (let [path (.-token event)
             {:keys [page params] :as nav} (url-to-nav routes path)]
         (if page
           (reset! navigation-state nav)
           (do
             (.warn js/console (str "No route matches token " path ", redirecting to /stations"))
             (navigate-to! routes {:page :stations}))))))


    (.setEnabled true)))

(defn search-stations
  [pcode instation outstation]
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
                               (swap! state assoc :route (first (response :result)))))))
              :error-handler (fn [{:keys [status status-text]}] (swap! state assoc :route nil))})
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
                           (swap! state assoc
                                  :available-stations
                                  {:columns ["入口"  "入口坐标" "出口" "出口坐标"]
                                   :rows (mapv #(vector (station % :in) (poi % :in) (station % :out) (poi % :out)) road)})))}))

(defn search-gaode-stations
  [pcode station attr]
  (ajax/POST "/gaode-stations"
             {:params {:pcode pcode :station station :attr attr}
              :response-format :json
              :format :json
              :keywords? true
              :handler (fn [response]
                         (let [status (response :status)
                               coll (response :result)]
                           (swap! state assoc
                                  :gaode-stations
                                  {:columns ["ID" "省份编码" "属性" "坐标"]
                                   :rows (for [{:keys [id pcode station attr lng lat]} coll]
                                           [id pcode station attr (array lng lat)])})
                           (doseq [{:keys [station attr lng lat]} coll]
                             (gaode/create-marker (str station attr) nil (array lng lat)))))}))
(defn station-form-submit
  [e]
  (let [pcode (value (by-id "pcode"))
        instation (value (by-id "instation"))
        outstation (value (by-id "outstation"))
        validate (fn [v id] (if (and v (not-empty v)) true (do (add-class! (by-id id) "has-error") false)))]
    (if (and
         (validate pcode "input-pcode")
         (validate instation "input-instation")
         (validate outstation "input-outstation"))
      (do
        (search-stations pcode instation outstation)
        (map #(remove-class! (by-id %) "has-error") ["input-pcode" "input-instation" "input-outstation"])
        (.reset (by-id "search-form"))
        (.preventDefault e))
      (.preventDefault e))))

(defn gaode-station-submit
  [e]
  (let [pcode (value (by-id "gaode-pcode"))
        station (value (by-id "gaode-station"))
        attr (value (by-id "gaode-attr"))
        validate (fn [v id] (if (and v (not-empty v)) true (do (add-class! (by-id id) "has-error") false)))]
    (if (or
         ;;(validate pcode "input-gaode-pcode")
         (validate station "input-gaode-station")
         (validate attr "input-gaode-attr"))
      (do
        (search-gaode-stations pcode station attr)
        (map #(remove-class! (by-id %) "has-error") ["input-gaode-pcode" "input-gaode-station" "input-gaode-attr"])
        (.reset (by-id "gaode-search-form"))
        (.preventDefault e))
      (.preventDefault e))))

(defn station-form
  []
  [ui/form-horizontal
   {:id "search-form" :action "#" :elements
    [[ui/input-element {:id "pcode" :name "pcode" :type "text" :placeholder "省份编码" :label "省份"}]
     [ui/input-element {:id "instation" :name "instation" :type "text" :placeholder "入站口" :label "入站"}]
     [ui/input-element {:id "outstation" :name "outstation" :type "text" :placeholder "出站口" :label "出站"}]
     [ui/button-element {:id "btn-submit" :type "submit" :value "查询" :on-click station-form-submit}]]}])

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

(defn gaode-station-form
  []
  (ui/form-horizontal
    {:id "gaode-search-form" :action "#" :elements
     [[ui/input-element {:id "gaode-pcode" :name "pcode" :type "text" :placeholder "省份编码" :label "省份"}]
      [ui/input-element {:id "gaode-station" :name "station" :type "text" :placeholder "收费站" :label "收费站"}]
      [ui/input-element {:id "gaode-attr" :name "attr" :type "text" :placeholder "属性" :label "属性"}]
      [ui/button-element {:type "submit" :value "查询" :on-click gaode-station-submit}]]}))

(def init-gaode-map (with-meta ui/gaode-map
                      {:component-did-mount #(gaode/create-map "map")}))
(defn station-search-page
  [{:keys [route available-stations]}]
  [:div
    [:div.row
     [:div.col-lg-3
      [ui/title-panel {:title "站点查询" :class "panel-success"
                       :body [:div
                              [station-form]
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
                              (let [row (-> (available-stations :row) (get i))
                                    p1 (row 1)
                                    p2 (row 3)
                                    t1 (row 0)
                                    t2 (row 2)]
                                (do
                                  (gaode/search-driving-path p1 p2)
                                  (gaode/start-marker t1 p1)
                                  (gaode/end-marker t2 p2)
                                  nil)))})]}])]]])

(defn gaode-station-page
  [params]
  [:div
   [:div.row
    [:div.col-lg-3
     [ui/title-panel {:title "高德收费站查询" :class "panel-success" :body [gaode-station-form]}]]
    [:div.col-lg-9
     [init-gaode-map]]]
   [:div.row
    [:div.col-lg-12
     (when params
       [ui/title-panel
        {:title "收费站数据"
         :class "panel-success"
         :body [ui/table params]}])]]])

(defn main-frame
  []
  (let [{:keys [page params]} @navigation-state]
    [:div.container-fluid
     [:nav {:class "navbar navbar-default"}
      [:div.container-fluid
       [:div.navbar-header
        [:a.navbar-brand {:href "#"} "数据分析展示"]]
       [:div {:class "collapse navbar-collapse"}
        [:ul {:class "nav navbar-nav"}
         [:li [:a {:href "#/analysis/stations"} "路径"]]
         [:li [:a {:href "#/analysis/gaode"} "高德"]]]]]]

     (case page
       :stations [station-search-page @state]
       :gaode [gaode-station-page (get-in @state [:gaode-stations])]
       [:div "Not Found"])]))



(def routes
  ["/analysis" {"/stations" :stations
                "/gaode" :gaode}])
(defn render
  []
  (hook-browser-navigation! routes)
  (r/render-component [main-frame] (by-id "app")))
