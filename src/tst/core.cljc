(ns tst.core)

(def VecType #?(:clj  clojure.lang.PersistentVector
                :cljs cljs.core/PersistentVector))

(defmacro testing [name & rst]
  (if (= name :result)
    (throw (ex-info ":result cannot be used as a test suite name" {})))
  (if (instance? VecType (first rst))
    (let [[params & body] rst
          _ (if (not (every? symbol? params))
              (throw (ex-info "non-symbol in parameter list" {})))
          args (->> params
                    (map (fn [p] `[~p '~p]))
                    (into {}))
          fn `(fn [~args]
                ~@body)]
      {name `(with-meta ~fn
                        {:code   (quote ~fn)
                         :params '~params})})
    {name (into {}
                (map
                  (fn [s]
                    (let [sname (second s)]
                      `[~sname (~s ~sname)]))
                  rst))}))

(defn run-test [ts]
  (if (fn? ts)
    (let [{params :params code :code} (meta ts)
          param_bindings (->> params
                              (map (fn [p] [p (atom :tst/UNINITIALIZED)]))
                              (into {}))
          result (try
                   (ts param_bindings)
                   (catch #?(:clj  Throwable
                             :cljs :default) e
                     {:result    :EXCEPTION
                      :exception e}))]
      (if (empty? param_bindings)
        result
        (assoc result :state (->> param_bindings
                                  (map (fn [[k v]] [k @v]))
                                  (into {})))))
    (into {} (for [[name body] ts]
               [name (run-test body)]))))

(defn flatten-result [res]
  (apply concat (for [[name body] res]
                  (if (:result body)
                    [(assoc body :path `(~name))]
                    (map #(update % :path conj name) (flatten-result body))))))

(defn treefy-result [res]
  (let [chs (group-by #(first (:path %)) res)]
    (reduce
      (fn [r [name results]]
        (assoc r name (if (and (= (count results) 1)
                               (= (count (get-in results [0 :path])) 1))
                        (dissoc (first results) :path)
                        (treefy-result (map #(update % :path rest) results)))))
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
