(ns usage
  (:require [tst.core :refer [run-test get-failed summarize-result combine-tests flatten-result testing] :refer-macros [testing]]
            [#?(:clj  clojure.pprint
                :cljs cljs.pprint) :refer [pprint]]))

(def suite1 (testing :test []
                     {:result  :OK
                      :message "A simple test suite"}))
(def suite2 (testing :a
                     (testing :b []
                              {:result :OK})
                     (testing :c []
                              {:result  :ERR
                               :message "failing test"})
                     (testing :d []
                              (throw (ex-info "exception demo" {}))
                              {:result  :OK
                               :message "ignored"})))
(def combined (combine-tests [suite1 suite2]))
(def suite3 (get-in combined [:test]))

(def suite4 (get-in combined [:a :c]))

(def results (run-test combined))

(defn get-a [] 1)
(defn get-b [a]
  (throw (new Error)))

(def suite5 (testing :suite [val-a]
                     (let [a (get-a)
                           _ (reset! val-a a)
                           b (get-b a)]
                       (if (= a b)
                         {:result :OK}
                         {:result :ERR
                          :cause  "a and b not equal"
                          :a      a
                          :b      b}))))
(def results5 (run-test suite5))

(def suite6 (testing :test_with_state [a b c]
                     (reset! a 2)
                     (reset! b 1)
                     (throw (new Error))
                     (reset! c (/ @b @a))
                     {:result :OK}))

(def results6 (run-test suite6))
(defn summarize [r]
  (println (summarize-result r)))

(summarize results)
(summarize results5)
(summarize results6)
