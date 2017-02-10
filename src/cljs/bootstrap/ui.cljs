(ns bootstrap.ui
  (:require [clojure.string :refer [join]]
            [goog.dom :as dom]
            [goog.dom.classlist :as classes]))

(defn input-element
  [{:keys [id name type placeholder label]}]
  [:div.form-group {:id (str "input-" id)}
   [:label {:for id :class "col-sm-2 control-label"} label]
   [:div.col-sm-10 [:input.form-control {:id id :name name :type type :placeholder placeholder :required ""}]]])

(defn button-element
  [{:keys [id type value on-click class]}]
  [:button.btn.btn-default {:id id :type type :class class :on-click on-click} value])

(defn form-horizontal
  [{:keys [id action elements]}]
  [:form.form-horizontal {:id id :action action :style {:padding "4px"}}
   (for [[element i] (zipmap elements (range))]
     ^{:key i} [:div element])])

(defn title-panel
  [{:keys [title class body]}]
  [:div.panel {:class class}
   [:div.panel-heading title]
   [:div.panel-body
    body]])

(defn tab-list
  [coll]
  (let [{:keys [id tab tab-content]} (first coll)
        tabs (next coll)]
    [:div
     [:ul {:class "nav nav-tabs" :role "tablist"}
      [:li {:class "active" :role "presentation"} [:a {:href (str "#" id) :role "tab" :data-toggle "tab" :aria-controls id} tab]]
      (for [{:keys [id tab]} tabs]
        ^{:key id} [:li {:role "presentation"} [:a {:href (str "#" id) :role "tab" :data-toggle "tab" :aria-controls id} tab]])
      ]
     [:div.tab-content {:style {:padding "4px"}}
      [:div {:id id :role "tabpanel" :class "tab-pane fade in active" :aria-labelledby (str id "-tab")} tab-content]
      (for [{:keys [id tab-content]} tabs]
        ^{:key id}[:div#station {:role "tabpanel" :class "tab-pane fade" :aria-labelledby (str id "-tab")}
                   tab-content])]]))
(defn table
  [{:keys [columns rows on-click on-dbclick]}]
  [:table {:class "table table-hover"}
   [:thead
    [:tr
     (for [[i column] (zipmap (range) columns)]
       ^{:key i} [:th column])]]
   [:tbody
    (for [[i row] (zipmap (range) rows)]
      ^{:key i} [:tr {:on-double-click (fn [e] (when on-dbclick (do (on-dbclick i) (classes/add (dom/getParentElement (.-target e)) "success") nil)))
                      :on-click (fn [e] (when on-click (on-click e)))}
                 (for [[index cell] (zipmap (range) row)]
                   ^{:key index} [:td (cond (array? cell) (join "," cell)
                                            :else cell)])])]])
(defn gaode-map
  []
  [title-panel {:title [button-element {:id "btn-map-clear" :type "button" :value "地图清空"  :class "btn-info btn-xs"}]
                   :class "panel-success"
                   :body [:div
                          [:div#map]]}])
