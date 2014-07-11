(ns tixi.mutators
  (:require-macros [tixi.utils :refer (b)])
  (:require [tixi.data :as d]))

(defn reset-data! []
  (reset! d/data d/initial-data))

(defn set-tool! [name]
  (swap! d/data assoc :tool name))

(defn set-action! [name]
  (swap! d/data assoc :action name))

(defn show-result! [value]
  (swap! d/data assoc :show-result value))
