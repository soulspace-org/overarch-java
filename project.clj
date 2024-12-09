(defproject org.soulspace/overarch-java "0.2.0-SNAPSHOT"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.soulspace/overarch-java-annotations "0.1.1"]]
  ;:java-source-paths ["src/main/java"]
  :source-paths ["src/main/clojure"]
  :resource-paths ["resources"]
  :aot [org.soulspace.overarch.java.annotation-processor]
  ;:main ^:skip-aot org.soulspace.overarch.java.annotation-processor

  :scm {:name "git" :url "https://github.com/soulspace-org/overarch-java"}
  :deploy-repositories [["clojars" {:sign-releases false :url "https://clojars.org/repo"}]])