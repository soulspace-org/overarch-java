(ns org.soulspace.overarch.java.annotation-processor
  (:require [clojure.string :as str]
            [clojure.edn :as edn])
  (:import [javax.annotation.processing AbstractProcessor ProcessingEnvironment]
           [javax.lang.model SourceVersion]
           [javax.lang.model.util Elements]
           [javax.tools Diagnostic$Kind]
           [org.soulspace.overarch.java OverarchNode])
  ; Generate the Processor class via AOT compilation
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

(defn log
  "Helper function to log messages to the processing environment."
  [processing-env msg]
  (-> processing-env
      (.getMessager)
      (.printMessage Diagnostic$Kind/NOTE msg)))

(defn first-upper
  "Returns the string with the first letter converted to upper case."
  [s]
  (str (str/upper-case (subs s 0 1)) (subs s 1)))

(defn first-lower
  "Returns the string with the first letter converted to lower case."
  [s]
  (str (str/lower-case (subs s 0 1)) (subs s 1)))

(defn from-camel-case
  "Converts a string 's' from camel case to a lower case string with the spacer character
  'c' inserted in front of intra word uppercase chars. Spacer chars are not inserted into
  upper case abbreviations. The case of the chars is retained.
  
  Examples:
  * (from-camel-case \"fromCamelCase\" \\\\-) -> \"from-Camel-Case\"
  * (from-camel-case \"getHTTPRequest\" \\\\-) -> \"get-HTTP-Request\"
  "
  [s ^Character c]
  (loop [chars (seq s) r-chars [] start? true in-upper? false]
    (if (seq chars)
      (let [current-char (char (first chars))]
        (if (not (or (Character/isDigit current-char) (Character/isLetter current-char)))
          ;; special char or white space, replace with hyphen
          (recur (rest chars) (conj r-chars \-) false false)
          (if (or (Character/isLowerCase current-char) (Character/isDigit current-char))
            ;; lower case or digit, don't add spacer char
            (recur (rest chars) (conj r-chars current-char) false false)
            (if start?
              ;; start of word, don't add spacer
              (recur (rest chars) (conj r-chars current-char) false true)
              (if-not (seq (rest chars))
                ;; last char, dont add spacer
                (recur (rest chars) (conj r-chars current-char) false true)
                ;; not the last char of the string
                (if in-upper?
                  (if (Character/isUpperCase (fnext chars))
                    ;; in an upper case word and the next char is upper case too
                    ;; don't add spacer here
                    (recur (rest chars) (conj r-chars current-char) false true)
                    ;; in an upper case word but the next char is lower case
                    ;; add a spacer char in front of the last upper case char
                    (recur (rest chars) (conj r-chars c current-char) false true))
                  ;; first upper case char after a lower case char, add spacer char
                  (recur (rest chars) (conj r-chars c current-char) false true)))))))
      (str/lower-case (apply str r-chars)))))

(defn to-camel-case
  "Converts a string 's' into camel case. Removes occurences of 'c' and converts
  the next character to upper case."
  ([s]
   (to-camel-case s \-))
  ([s c]
   (loop [chars (seq s) cc-chars []]
     (if (seq chars)
       (if (= (first chars) c)
         (recur (rest (rest chars)) (conj cc-chars (str/upper-case (second chars))))
         (recur (rest chars) (conj cc-chars (str (first chars)))))
       (first-upper (apply str cc-chars))))))

(defn fqn-namespace
  "Returns the namespace part of the fully qualified name `fqn`."
  [fqn]
  (str/join "." (drop-last (str/split fqn #"\."))))

(defn fqn-name
  "Returns the name part of the fully qualified name `fqn`."
  [fqn]
  (last (str/split fqn #"\.")))

(defn fqn->id
  "Returns an overarch id keyword for the fully qualified name `fqn`."
  [fqn]
  (if-let [nspace (fqn-namespace fqn)]
    (keyword (str nspace "/" (str/lower-case (from-camel-case (fqn-name fqn) \-))))
    (keyword (str/lower-case (from-camel-case (fqn-name fqn) \-)))))

(comment
  (to-camel-case "i-user-repository")
  (from-camel-case "IUserRepository" \-)
  (fqn-namespace "example.user.domain.User")
  (fqn-name "example.user.domain.User")
  (fqn->id "example.user.application.IUserRepository")
  ;
  )

(def element-type-map
  {"ANNOTATION_TYPE"  :annotation
   "CLASS"            :class
   "ENUM"             :enum
   "ENUM_CONSTANT"    :enum-value
   "FIELD"            :field
   "INTERFACE"        :interface
   "METHOD"           :method
   "MODULE"           :component
   "PACKAGE"          :package
   "RECORD"           :class
   "RECORD_COMPONENT" :field})

(defn write-model
  "Writes model to file."
  ([elements]
   (spit "model.edn"
         (str "
;;;;
;;;; Overarch Model 
;;;;
"
              elements))))

;; TODO multi method dispatched on the type/kind of the element

#_(defn process-element
    "Process an individual element annotated with @OverarchNode."
    [processing-env elem]
    (log processing-env (str "Processing element: " (.getSimpleName elem)))
  ;; Here you can add any additional processing logic, e.g., generating files
    )

(defn children
  "Returns the children of the `element`."
  [element]
  (.getEnclosedElements element))

(defn -process
  "Processes elements annotated with @OverarchNode."
  [this annotations round-env]
  ;(println "process:" round-env)
  (let [^ProcessingEnvironment processing-env @(.state this)
        ^Elements utils (.getElementUtils processing-env)]
    (if (.processingOver round-env)
      (log processing-env "processing over for Overarch annotations")
      (let [elements (.getElementsAnnotatedWith round-env OverarchNode)]
        ; TODO convert to loop? convert to traverse with step-fn?
        (letfn [(process-elements
                 [acc elements]
                ;(println "acc:" acc)
                 (if (seq elements)
                   (let [element (first elements)
                         anno (.getAnnotation element OverarchNode)
                         el (if (seq (.el anno))
                              (keyword (.el anno))
                              (element-type-map (.name (.getKind element))))
                         id (if (seq (.id anno))
                              (keyword (.id anno))
                              (fqn->id (str (.getQualifiedName element))))
                         name (if (seq (.name anno))
                                (.name anno)
                                (.toString (.getSimpleName element)))
                         desc (if (seq (.desc anno))
                                (.desc anno)
                                (str/trim (.getDocComment utils element)))
                         tech (if (seq (.tech anno)) (.tech anno) "Java")
                         tags (if (seq (.tags anno)) (into #{} (.tags anno)) #{})
                         node {:el el :id id :name name :desc desc :tech tech :tags tags}]
                      ;(println node)
                      ;(println "Annotation?" (type anno) anno)
                      ;(println "Element?" (type elem) element)
                      ;(println (.getEnclosingElement element))
                      ;(println (.getEnclosedElements element))
                     (recur (conj acc node) (rest elements)))
                   acc))]
          (->> elements
               (process-elements #{})
               (write-model))))))
  ; return true to claim the annotations and end the round
  true)
