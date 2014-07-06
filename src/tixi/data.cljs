(ns tixi.data
  (:require-macros [tixi.utils :refer [defdata]])
  (:require [tixi.utils :refer [seq-contains? p]]
            [tixi.tree :as t :refer [Node]]
            [tixi.drawer :as dr]
            [clojure.zip :as z]))

(defn zip [root]
  (z/zipper
    (fn [_] true)
    #(:children %)
    #(assoc %1 :children %2)
    root))

(def initial-data
  {:current nil
   :state (zip (t/node {:completed {}
                        :locks {:connectors {}
                                :lockables {}}}))
   :tool :line
   :action nil
   :autoincrement 0
   :selection {:ids []
               :rect nil
               :current nil
               :rel-rects {}}
   :cache {}
   :edit-text-id nil
   :hover-id nil
   :show-result false})

(def data
  (atom initial-data))

(defdata current []
  (:current data))

(defdata state-loc []
  (:state data))

(defdata state []
  (:value (z/node (state-loc data))))

(defdata previous-state []
  (:value (z/node (z/up (state-loc data)))))

(defdata completed []
  (:completed (state data)))

(defdata locks []
  (:locks (state data)))

(defdata lockable [lockable-id]
  (get-in (state data) [:locks :lockables lockable-id] {}))

(defdata lockable-connector [lockable-id connector-id]
  (get-in (state data) [:locks :lockables lockable-id connector-id] {}))

(defdata connector-types [connector-id]
  (get-in (state data) [:locks :connectors connector-id] {}))

(defdata lockable-id-by-connector-id-and-type [connector-id type]
  (get-in (state data) [:locks :connectors connector-id type]))

(defdata completed-item [id]
  (get-in (state data) [:completed id]))

(defdata cache []
  (get data :cache))

(defdata item-cache [id]
  (get (cache data) id))

(defdata tool []
  (:tool data))

(defdata action []
  (:action data))

(defdata selection []
  (:selection data))

(defdata selected-ids []
  (get-in data [:selection :ids]))

(defdata selected-rel-rect [id]
  (get-in data [:selection :rel-rects id]))

(defdata selection-rect []
  (get-in data [:selection :rect]))

(defdata current-selection []
  (get-in data [:selection :current]))

(defdata hover-id []
  (:hover-id data))

(defdata draw-tool? []
  (seq-contains? [:line :rect :rect-line :text] (tool data)))

(defdata select-tool? []
  (seq-contains? [:select] (tool data)))

(defdata draw-action? []
  (seq-contains? [:draw] (action data)))

(defdata resize-action []
  (when-let [[_ result] (when (action data) (re-matches #"^resize-(.+)" (name (action data))))]
    (keyword result)))

(defdata edit-text-id []
  (:edit-text-id data))

(defdata result []
  (dr/buildResult (clj->js (completed data)) (clj->js (cache data))))

(defdata show-result? []
  (boolean (:show-result data)))
