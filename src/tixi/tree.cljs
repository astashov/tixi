(ns tixi.tree
  (:require [tixi.utils :refer [p]]))

(defprotocol INode
  (update [this value]))

(defrecord Node [value children]
  INode
  (update [this new-value]
    (Node. new-value children)))

(defn node
  ([value] (node value []))
  ([value children] (Node. value children)))
