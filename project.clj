(defproject org.soulspace/overarch-java "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.soulspace/overarch-java-annotations "0.1.0-SNAPSHOT"]]
  ;:java-source-paths ["src/main/java"]
  :source-paths ["src/main/clojure"]
  :resource-paths ["resources"]
  :aot [org.soulspace.overarch.java.annotation-processor]
  ;:main ^:skip-aot org.soulspace.overarch.java.annotation-processor
  )