(ns org.soulspace.overarch.java.annotation-processor
  (:require [clojure.string :as str]
            [clojure.edn :as edn])
  (:import [javax.annotation.processing AbstractProcessor ProcessingEnvironment]
           [javax.lang.model SourceVersion]
           [javax.lang.model.element Element]
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

;;;
;;; Annotation processor infrastructure
;;;
(def supported-annotations
  "Defines the annotations that this processor supports."
  (doto (java.util.HashSet.)
    (.add "org.soulspace.overarch.java.OverarchNode")
    (.add "org.soulspace.overarch.java.OverarchRelation")))

(defn -construct
  "Constructor"
  []
  [[] (atom {})]
  )

(defn -init
  "Initialization for OverarchProcessor."
  ([this processing-env]
   (reset! (.state this) {:processing-env processing-env})))

(defn -getSupportedAnnotationTypes
  "Returns the set of supported annotation types."
  [this]
  supported-annotations)

(defn -getSupportedSourceVersion
  "Returns the supported source version."
  [this]
  SourceVersion/RELEASE_21)

(defn log
  "Helper function to log messages to the processing environment."
  [processing-env msg]
  (-> processing-env
      (.getMessager)
      (.printMessage Diagnostic$Kind/NOTE msg)))

(defn processing-env
  "Returns the processing environment from the processor state."
  [this]
  (:processing-env @(.state this)))

;;;
;;; String conversion
;;;
(defn first-upper
  "Returns the string with the first letter converted to upper case."
  [^String s]
  (str (str/upper-case (subs s 0 1)) (subs s 1)))

(defn first-lower
  "Returns the string with the first letter converted to lower case."
  [^String s]
  (str (str/lower-case (subs s 0 1)) (subs s 1)))

(defn from-camel-case
  "Converts a string 's' from camel case to a lower case string with the spacer character
  'c' inserted in front of intra word uppercase chars. Spacer chars are not inserted into
  upper case abbreviations. The case of the chars is retained.
  
  Examples:
  * (from-camel-case \"fromCamelCase\" \\\\-) -> \"from-Camel-Case\"
  * (from-camel-case \"getHTTPRequest\" \\\\-) -> \"get-HTTP-Request\"
  "
  [^String s ^Character c]
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
  ([^String s]
   (to-camel-case s \-))
  ([^String s ^Character c]
   (loop [chars (seq s) cc-chars []]
     (if (seq chars)
       (if (= (first chars) c)
         (recur (rest (rest chars)) (conj cc-chars (str/upper-case (second chars))))
         (recur (rest chars) (conj cc-chars (str (first chars)))))
       (first-upper (apply str cc-chars))))))

(defn fqn-namespace
  "Returns the namespace part of the fully qualified name `fqn`."
  [^String fqn]
  (str/join "." (drop-last (str/split fqn #"\."))))

(defn fqn-name
  "Returns the name part of the fully qualified name `fqn`."
  [^String fqn]
  (last (str/split fqn #"\.")))

(defn fqn->id
  "Returns an overarch id keyword for the fully qualified name `fqn`."
  [^String fqn]
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

;;;
;;; Element handling
;;;

(defn overarch-annotations
  "Returns the annotations of the `element`."
  ([^Element element]
   (overarch-annotations element #{"org.soulspace.overarch.java.OverarchNode"
                                   "org.soulspace.overarch.java.OverarchRelation"})) 
  ([^Element element annotation-types] 
   (filter #(contains? annotation-types (.name %))
           (.getAnnotationMirrors element))))

(defn parent
  "Returns the children of the `element`."
  [^Element element]
  (.getEnclosingElement element))

(defn children
  "Returns the children of the `element`."
  [element]
  (.getEnclosedElements element))

(def element-kind-map
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

(defn element-kind
  "Returns the kind of the `element`."
  [element]
  (.name (.getKind element)))

(defn element-type
  "Returns the overarch element type for this `element` and annotation `anno`."
  ([element]
   (element-kind-map (.name (.getKind element))))
  ([element anno]
   (if (seq (.el anno))
     (keyword (.el anno))
     (element-type element))))

(defn element-id
  "Returns the overarch element id for this `element` and annotation `anno`."
  ([element]
   (fqn->id (str (.getQualifiedName element))))
  ([element anno]
   (if (seq (.id anno))
     (keyword (.id anno))
     (element-id element))))

(defn element-name
  "Returns the overarch element name for this `element` and annotation `anno`."
  ([element]
   (str (.getSimpleName element)))
  ([element anno]
   (if (seq (.name anno))
     (.name anno)
     (element-name element))))

(defn element-desc
  "Returns the overarch element desc for this `element` and annotation `anno`."
  ([element utils]
   (str/trim (str "" (.getDocComment utils element))))
  ([element anno utils]
   (if (seq (.desc anno))
     (.desc anno)
     (element-desc element utils))))

(defmulti process-element
  "Returns the overarch elements for the given java `element`."
  element-kind)

(defmethod process-element "ANNOTATION_TYPE"
  [element-kind element])

(defmethod process-element "BINDING_VARIABLE"
  [element-kind element])

(defmethod process-element "CLASS"
  [element-kind element])

(defmethod process-element "CONSTRUCTOR"
  [element-kind element])

(defmethod process-element "ENUM"
  [element-kind element])

(defmethod process-element "ENUM_CONSTANT"
  [element-kind element])

(defmethod process-element "EXCEPTION_PARAMETER"
  [element-kind element])

(defmethod process-element "FIELD"
  [element-kind element])

(defmethod process-element "INSTANCE_INIT"
  [element-kind element])

(defmethod process-element "INTERFACE"
  [element-kind element])

(defmethod process-element "LOCAL_VARIABLE"
  [element-kind element])

(defmethod process-element "METHOD"
  [element-kind element])

(defmethod process-element "MODULE"
  [element-kind element])

(defmethod process-element "OTHER"
  [element-kind element])

(defmethod process-element "PACKAGE"
  [element-kind element])

(defmethod process-element "PARAMETER"
  [element-kind element])

(defmethod process-element "RECORD"
  [element-kind element])

(defmethod process-element "RECORD_COMPONENT"
  [element-kind element])

(defmethod process-element "RESOURCE_VARIABLE"
  [element-kind element])

(defmethod process-element "STATIC_INIT"
  [element-kind element])

(defmethod process-element "TYPE_PARAMETER"
  [element-kind element])


;;;
;;; Model I/O
;;;
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


(defn -process
  "Processes elements annotated with @OverarchNode."
  [this annotations round-env]
  (let [^ProcessingEnvironment processing-env (:processing-env @(.state this))
        ^Elements utils (.getElementUtils processing-env)]
    (if (.processingOver round-env)
      (log processing-env "processing over for Overarch annotations")
      (let [elements (.getElementsAnnotatedWith round-env OverarchNode)]
        ; TODO convert to loop? convert to traverse with step-fn?
        (letfn [(process-elements
                 [acc elements]
                 (if (seq elements)
                   (let [element (first elements)
                         anno (.getAnnotation element OverarchNode)
                         node {:el (element-type element anno)
                               :id (element-id element anno)
                               :name (element-name element anno)
                               :desc (element-desc element anno utils)
                               :tech (if (seq (.tech anno)) (.tech anno) "Java")
                               :tags (if (seq (.tags anno)) (into #{} (.tags anno)) #{})}]
                      ;(println (.getEnclosingElement element))
                      ;(println (.getEnclosedElements element))
                     (recur (conj acc node) (rest elements)))
                   acc))]
          (->> elements
               (process-elements #{})
               (write-model))))))
  ; return true to claim the annotations and end the round
  true)
