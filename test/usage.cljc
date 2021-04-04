(ns usage
  (:require [tst.core :refer [run-test get-failed summarize-result combine-tests flatten-result testing] :refer-macros [testing]]
            [#?(:clj clojure.pprint
                :cljs cljs.pprint) :refer [pprint]]))

; 'testing' returns a test spec that can be passed to 'run-test'
(def t (testing :a
                (testing :b []
                         {:result :OK})
                (testing :c []
                         {:result  :ERR
                          :message "failing test"})
                (testing :d []
                         (throw (ex-info "" {}))
                         {:result  :OK
                          :message "ignored"})
                {:result  :ERR
                 :message "another failing test"}))

(def res (run-test t))
; returns:
#_{:a {:b    {:main {:result :OK, :context (clojure.core/fn [{}] {:result :OK}), :state {}}},
       :c    {:main {:result  :ERR,
                     :message "failing test",
                     :context (clojure.core/fn [{}] {:result :ERR, :message "failing test"}),
                     :state   {}}},
       :d    {:main {:result    :EXCEPTION,
                     :exception #:error{},
                     :context   (clojure.core/fn [{}] (/ 1 0) {:result :OK, :message "ignored"}),
                     :state     {}}},
       :main {:result  :ERR,
              :message "another failing test",
              :context (clojure.core/fn [{}] {:result :ERR, :message "another failing test"}),
              :state   {}}}}

(def failed (get-failed res))
#_{:a {:c    {:main {:result  :ERR,
                     :message "failing test",
                     :context (clojure.core/fn [{}] {:result :ERR, :message "failing test"}),
                     :state   {}}},
       :d    {:main {:result    :EXCEPTION,
                     :exception #:error{},
                     :context   (clojure.core/fn [{}] (/ 1 0) {:result :OK, :message "ignored"}),
                     :state     {}}},
       :main {:result  :ERR,
              :message "another failing test",
              :context (clojure.core/fn [{}] {:result :ERR, :message "another failing test"}),
              :state   {}}}}

(def lst (flatten-result failed))
; returns a sequence of three failed results

(def fst (first lst))
; first failed result
#_{:result  :ERR,
   :message "failing test",
   :context (clojure.core/fn [{}] {:result :ERR, :message "failing test"}),
   :state   {},
   :path    (:a :c :main)}

; The body of a test case must return a map with `:result` as a key, it's value must be `:OK` if the test was successful
; This map may contain other data as well

; test suits can be combined:

(defn int-tests []
  (testing :ints
           (testing :adding_1 []
                    (if (= (+ 5 1) 6)
                      {:result :OK}
                      {:result :ERR}))
           (testing :adding_2 []
                    (if (= (+ 5 2) 6)
                      {:result :OK}
                      {:result  :ERR
                       :message "values not equal"}))
           (testing :failing_test []
                    (throw (new Error)))))

(defn string-tests []
  (testing :strings
           (testing :concating_asdf
                    (if (= (str "hello" "asdf") "helloasdf")
                      {:result :OK}
                      {:result :ERR}))))

(defn all-tests []
  (combine-tests [(int-tests)
                  (string-tests)]))

(def res2 (run-test (all-tests)))
#_{:ints    {:adding_1     {:main {:result  :OK,
                                   :context (clojure.core/fn [{}] (if (= (+ 5 1) 6) {:result :OK} {:result :ERR})),
                                   :state   {}}},
             :adding_2     {:main {:result  :ERR,
                                   :message "values not equal",
                                   :context (clojure.core/fn
                                              [{}]
                                              (if (= (+ 5 2) 6) {:result :OK} {:result :ERR, :message "values not equal"})),
                                   :state   {}}},
             :failing_test {:main {:result    :EXCEPTION,
                                   :exception #:error{},
                                   :context   (clojure.core/fn [{}] (throw "Exception")),
                                   :state     {}}}},
   :strings {:concating_asdf {:main {:result  :OK,
                                     :context (clojure.core/fn
                                                [{}]
                                                (if (= (str "hello" "asdf") "helloasdf") {:result :OK} {:result :ERR})),
                                     :state   {}}}}}

; Leaf nodes in a test suite may have a vector of symbols which define the state of that test case.
; This symbols will resolve to atoms in the context of test suite. Their values will be returned in
; the `:state` key of result

(def t3 (testing :test_with_state [a b c]
                 (reset! a 2)
                 (reset! b 1)
                 (throw (new Error))
                 (reset! c (/ @b @a))
                 {:result :OK}))

(def res3 (run-test t3))
#_{:test_with_state {:main {:result    :EXCEPTION,
                            :exception #:error{},
                            :context   (clojure.core/fn
                                         [{a (quote a), b (quote b), c (quote c)}]
                                         (reset! a 0)
                                         (reset! b 1)
                                         (reset! c (/ (clojure.core/deref b) (clojure.core/deref a)))
                                         {:result :OK}),
                            :state     {a 0, b 1, c :tst/UNINITIALIZED}}}}

;(enable-console-print!)
; summary
(defn summarize [r]
  (println (summarize-result r)))

(summarize res)
#_{:total_tests 4, :passed 1, :failed 2, :errors 1}
(summarize res2)
(summarize res3)
