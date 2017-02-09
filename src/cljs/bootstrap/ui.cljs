(ns bootstrap.ui
  (:require [clojure.string :refer [join]]))

(defn input-element
  [id name type placeholder label]
  [:div.form-group {:id (str "input-" id)}
   [:label {:for id :class "col-sm-2 control-label"} label]
   [:div.col-sm-10 [:input.form-control {:id id :name name :type type :placeholder placeholder :required ""}]]])

(defn button-element
  [type value click-fn]
  [:button.btn.btn-default {:type type :on-click click-fn} value])

(defn form-horizontal
  [id action element-coll]
  [:form.form-horizontal {:id id :action action :style {:padding "4px"}}
   (for [[element i] (zipmap element-coll (range))]
     ^{:key i} [:div element])])

(defn title-panel
  [^string title cls body]
  [:div.panel {:class cls}
   [:div.panel-heading title]
   [:div.panel-body
    body]])

(defn tab-list
  [coll]
  (let [first-tab (first coll)
        tabs (next coll)]
    [:div
     [:ul {:class "nav nav-tabs" :role "tablist"}
      [:li {:class "active" :role "presentation"} [:a {:href (str "#" (first-tab :id)) :role "tab" :data-toggle "tab" :aria-controls (first-tab :id)} (first-tab :tab)]]
      (for [{:keys [id tab]} tabs]
        ^{:key id} [:li {:class "" :role "presentation"} [:a {:href (str "#" id) :role "tab" :data-toggle "tab" :aria-controls "station"} tab]])
      ]
     [:div.tab-content {:style {:padding "4px"}}
      [:div {:id (first-tab :id) :role "tabpanel" :class "tab-panel fade in active" :aria-labelledby "route-tab"}
       (first-tab :tab-content)]
      (for [{:keys [id tab-content]} tabs]
        ^{:key id}[:div#station {:role "tabpanel" :class "tab-panel fade" :aria-labelledby "station-tab"}
                   tab-content])]]))
(defn table
  [{:keys [columns rows on-click on-dbclick]}]
  [:table {:class "table table-bordered .table-hover"}
   [:thead
    [:tr
     (for [[i column] (zipmap (range) columns)]
       ^{:key i} [:th column])]]
   [:tbody
    (for [[i row] (zipmap (range) rows)]
      ^{:key i} [:tr {:on-double-click (fn [e] (if on-dbclick (on-dbclick i))) :on-click (fn [e] (when on-click (on-click e)))}
                 (for [[index cell] (zipmap (range) row)]
                   ^{:key index} [:td (cond (array? cell) (join "," cell)
                                            :else cell)])])]])
