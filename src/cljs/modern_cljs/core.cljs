(ns modern-cljs.core
  (:import [goog History])
  (:require [station-search.core :as station]
            [reagent.core :as r]
            [station-search.core :as station]
            [gaode-search.core :as gs]
            [bidi.bidi :as b]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [car-path.core :as car-path]))

;; enable cljs to print to the JS console of the browser
(enable-console-print!)

;; print to the console
(println "Hello, World!!!!")

(defonce state
  (r/atom {:navigation {:page :stations :params {}}}))

(def navigation-state (r/cursor state [:navigation]))
(def available-stations-atom (r/cursor state [:available-stations]))
(def gaode-stations-atom (r/cursor state [:gaode-stations]))
(def car-path-atom (r/cursor state [:car-path]))

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
         [:li [:a {:href "#/analysis/gaode"} "高德"]]
         [:li [:a {:href "#/analysis/carpath"} "轨迹"]]]]]]
     (case page
           :stations [station/station-search-page available-stations-atom]
           :gaode [gs/gaode-station-page gaode-stations-atom]
           :car-path [car-path/car-path-page car-path-atom]
           [:div "Not Found"])]))

(def routes
  ["/analysis" {"/stations" :stations
                "/gaode" :gaode
                "/carpath" :car-path}])
(defn render
  []
  (hook-browser-navigation! routes)
  (r/render-component [main-frame] (.getElementById js/document "app")))

(render)
