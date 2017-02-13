(set-env!
 :source-paths #{"src/cljs"}
 :resource-paths #{"html" "src/clj"}

 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [boot/core "2.7.1" :scope "test"]
                 [adzerk/boot-cljs "1.7.170-3" :scope "test"]
                 [pandeiro/boot-http "0.7.0" :scope "test"]
                 [adzerk/boot-reload "0.5.1" :scope "test" ]
                 [adzerk/boot-cljs-repl "0.3.0" :scope "test"]
                 [com.cemerick/piggieback "0.2.1" :scope "test"]     ;; needed by bREPL
                 [weasel "0.7.0" :scope "test"]                      ;; needed by bREPL
                 [org.clojure/tools.nrepl "0.2.12" :scope "test"]
                 [reagent "0.6.0"]
                 [cljs-ajax "0.5.8"]
                 [org.clojars.magomimmo/domina "2.0.0-SNAPSHOT"]
                 [com.sdhs.etc.analysis/analysis-restful-service "0.0.1"]
                 [bidi "2.0.16"]
                 [proto-repl "0.3.1"]
                 ]
 :mirrors ["central" {:name "uk" :url "http://uk.maven.org/maven2/"}])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[pandeiro.boot-http :refer [serve]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         )
(task-options!
 cljs {:source-map true}
)

;; define dev task as composition of subtasks
(deftask dev
  "Launch Immediate Feedback Development Environment"
  []
  (comp
   (serve :dir "target"
          :resource-root "target" 
          :handler 'com.sdhs.etc.analysis.core/app
          :reload true)
   (watch)
   (speak)
   (reload)
   (cljs-repl) ;; before cljs task
   (cljs)
   (target :dir #{"target"})))

(deftask build []
  (cljs :optimizations :advanced))
