(ns org.soulspace.overarch.java.annotation-processor
  (:require [clojure.string :as str])
  (:import [javax.annotation.processing AbstractProcessor]
           [javax.lang.model SourceVersion]
           [javax.lang.model.element ElementKind]
           [javax.lang.model.util Elements]
           [java.lang.reflect Proxy]
           [javax.tools Diagnostic$Kind]
           [org.soulspace.overarch.java OverarchNode]
           )
  (:gen-class
   :name org.soulspace.overarch.java.OverarchProcessor
   :extends javax.annotation.processing.AbstractProcessor
   :state state
   :expose-methods []
   :init construct
   ;:constructors {[] []}
   ))

;; Define the annotations that this processor supports
(def supported-annotations
  (doto (java.util.HashSet.)
    (.add "org.soulspace.overarch.java.OverarchNode")
    (.add "org.soulspace.overarch.java.OverarchRelation")))

(defn -construct
  "Constructor"
  []
  [[] (atom nil)]
  )

(defn -init
  "Initialization for OverarchProcessor."
  ([this processing-env]
   (reset! (.state this) processing-env)))

(defn -getSupportedAnnotationTypes
  "Returns the set of supported annotation types."
  [this]
  supported-annotations)

(defn -getSupportedSourceVersion
  "Returns the supported source version."
  [this]
  javax.lang.model.SourceVersion/RELEASE_21)

#_(defn log
  "Helper function to log messages to the processing environment."
  [processing-env msg]
  (-> processing-env
      (.getMessager)
      (.printMessage Diagnostic$Kind/NOTE msg)))

#_(defn process-element
  "Process an individual element annotated with @OverarchNode."
  [processing-env elem]
  (log processing-env (str "Processing element: " (.getSimpleName elem)))
  ;; Here you can add any additional processing logic, e.g., generating files
  )

(defn -process
  "Processes elements annotated with @OverarchNode."
  [this annotations round-env]
  ;(println "process:" round-env)
  (let [processing-env @(.state this)
        utils (.getElementUtils processing-env)
        elements (.getElementsAnnotatedWith round-env OverarchNode)]
    (doseq [elem elements]
      (when (= (.getKind elem) ElementKind/CLASS)
        (let [anno (.getAnnotation elem OverarchNode)
              el (keyword (.el anno))
              id (if (seq (.id anno)) (.id anno) (.getQualifiedName elem))
              name (if (seq (.name anno)) (.name anno) (.toString (.getSimpleName elem)))
              ;name (if (seq (.name anno)) (.name anno) "")
              desc (if (seq (.desc anno)) (.desc anno) (str/trim (.getDocComment utils elem)))
              tech (if (seq (.tech anno)) (.tech anno) "Java")
              node {:el el :id id :name name :desc desc :tech tech}]
          ;(println "Annotation?" (type anno) anno)
          ;(println "Element?" (type elem) elem)
          (println node)
        ;(process-element processing-env elem)
        ))))
  true)
