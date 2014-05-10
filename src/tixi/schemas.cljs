(ns tixi.schemas
 (:require [schema.core :as s]))

(def ^:private item-types
  [:line :rect :rect-line])

(def Item
  {:type (apply s/enum item-types)
   :content [s/Int]})

(def Data
  {:current (s/maybe {:id s/Int :item Item})
   :completed {s/Int Item}
   :tool (apply s/enum (conj item-types :select))
   :action (s/maybe s/Keyword)
   :autoincrement s/Int
   :selected-id (s/maybe s/Int)
   :hover-id (s/maybe s/Int)
   :moving-from (s/maybe [s/Int])})
