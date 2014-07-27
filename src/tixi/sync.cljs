(ns tixi.sync
  (:require [tixi.data :as d]
            [clojure.string :as string]
            [tixi.utils :refer [p]]
            [tixi.compress :as c]
            [tixi.uuid :as uuid]))

(def scheduled? (atom false))
(def timer (atom nil))

(def ^:private id (memoize (fn []
  (let [uuid (string/replace (.-hash js/location) #"^#" "")]
    (if (empty? uuid)
      (let [uuid (uuid/generate)]
        (set! (.-hash js/location) (str "#" uuid))
        uuid)
      uuid)))))

(def firebase (memoize (fn []
  (when (aget js/window "Firebase")
    (js/Firebase. (str "https://dazzling-fire-2058.firebaseio.com/" (id)))))))

(defn- -sync! []
  (.set (firebase) (c/compress (pr-str (d/state))))
  (reset! timer (js/setTimeout (fn []
                                 (when @scheduled?
                                   (-sync!))
                                 (reset! timer nil)
                                 (reset! scheduled? false))
                               1000)))

(defn sync! []
  (when (firebase)
    (if @timer
      (reset! scheduled? true)
      (-sync!))))

(defn load [f]
  (when (firebase)
    (.once (firebase) "value" f)))
