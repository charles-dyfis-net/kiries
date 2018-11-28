(def boot-version
  (get (boot.App/config) "BOOT_VERSION" "2.8.2"))


;; Taken from https://github.com/boot-clj/boot/wiki/Using-Boot-in-a-Leiningen-Project
(deftask from-lein
  "Use project.clj as source of truth as far as possible"
  []
  (let [lein-proj (let [l (-> "project.clj" slurp read-string)]
                    (merge (->> l (drop 3) (partition 2) (map vec) (into {}))
                           {:project (second l) :version (nth l 2)}))]
    (merge-env! :repositories (:repositories lein-proj))
    (set-env!
      :certificates   (:certificates lein-proj)
      :source-paths   (or (into #{} (:source-paths lein-proj)) #{"src"})
      :resource-paths (or (into #{} (:resource-paths lein-proj)) #{"resources"})
      :dependencies   (into (:dependencies lein-proj)
                            `[[boot/core ~boot-version :scope "provided"]
                              [thought2/boot2nix "0.1.0"]]))

    (require '[thought2.boot2nix :refer [boot2nix]])
    (task-options!
      repl (:repl-options lein-proj {})
      aot  (let [aot (:aot lein-proj)
                 all? (or (nil? aot) (= :all aot))
                 ns (when-not all? (set aot))]
             {:namespace ns :all all?})
      jar  {:main (:main lein-proj)}
      pom  {:project     (symbol (:project lein-proj))
            :version     (:version lein-proj)
            :description (:description lein-proj)
            :url         (:url lein-proj)
            :scm         (:scm lein-proj)
            :license     (get lein-proj :license {"EPL" "http://www.eclipse.org/legal/epl-v10.html"})}))
  identity)
