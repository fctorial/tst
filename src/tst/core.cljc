(ns tst.core)

(defn grouper [b]
  (group-by #(if (and (seqable? %)
                      (symbol? (first %))
                      (= "testing" (name (first %))))
               :testing :main) b))

(def VecType #?(:clj clojure.lang.PersistentVector
                :cljs cljs.core/PersistentVector))

(defmacro m [a b]
  `(+ a b))
; name !== :main
; params == [symbol]
(defmacro testing [name & rst]
  (let [[params & body] (if (instance? VecType (first rst))
                          rst
                          (cons [] rst))
        {subs :testing fns :main} (grouper body)
        res (mapv
              (fn [s]
                (let [n (second s)]
                  `[~n (~s ~n)]))
              subs)
        args (->> params
                  (map (fn [p] `[~p '~p]))
                  (into {}))
        fn `(fn [~args]
              ~@fns)]
    {name (into {} (if fns
                     (conj res [:main `(with-meta ~fn
                                                  {:code   (quote ~fn)
                                                   :params '~params})])
                     res))}))

(defn run-test [ts]
  (into {} (for [[name body] ts]
             [name
              (if (= name :main)
                (let [{params :params code :code} (meta body)
                      param_bindings (->> params
                                          (map (fn [p] [p (atom :tst/UNINITIALIZED)]))
                                          (into {}))]
                  (assoc
                    (try
                      (body param_bindings)
                      (catch #?(:clj  Throwable
                                :cljs :default) e
                        {:result    :EXCEPTION
                         :exception e}))
                    :context code
                    :state (->> param_bindings
                                (map (fn [[k v]] [k @v]))
                                (into {}))))
                (run-test body))])))

(defn flatten-result [res]
  (apply concat (for [[name body] res]
                  (if (= :main name)
                    [(assoc body :path '(:main))]
                    (map #(update % :path conj name) (flatten-result body))))))

(defn treefy-result [res]
  (let [chs (group-by #(first (:path %)) res)]
    (reduce
      (fn [r [name body]]
        (assoc r name (if (= name :main)
                        (dissoc (first body) :path)
                        (treefy-result (map #(update % :path rest) body)))))
      {}
      chs)))

(defn get-failed [res]
  (->> res
       flatten-result
       (filter #(not= (:result %)
                      :OK))
       treefy-result))

(defn filter-tests-by-result [res st]
  (->> res
       flatten-result
       (filter #(= st (:result %)))
       treefy-result))

(defn summarize-result [res]
  (let [flat (flatten-result res)
        grouped (group-by :result flat)]
    {:total_tests (count flat)
     :passed      (count (grouped :OK))
     :failed      (count (grouped :ERR))
     :errors      (count (grouped :EXCEPTION))}))

(defn combine-tests [ts]
  (reduce #(merge-with merge %1 %2) ts))
