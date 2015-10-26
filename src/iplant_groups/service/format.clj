(ns iplant_groups.service.format
  (:use [medley.core :only [remove-vals]]
        [slingshot.slingshot :only [throw+]])
  (:require [clj-time.format :as tf]
            [clojure.string :as string]
            [clojure-commons.error-codes :as ce]))

(def ^:private timestamp-formatter (tf/formatter "yyyy/MM/dd HH:mm:ss.SSSSSS"))

(defn timestamp-to-millis
  [timestamp]
  (when-not (nil? timestamp)
    (.getMillis (tf/parse timestamp-formatter timestamp))))

(defn string-to-boolean
  [s]
  (when-not (nil? s)
    (condp = (string/lower-case s)
      "true"  true
      "t"     true
      "yes"   true
      "y"     true
      "false" false
      "f"     false
      "no"    false
      "n"     false
      (throw+ {:error_code    ce/ERR_ILLEGAL_ARGUMENT
               :boolean_value s}))))

(defn- not-found
  [response]
  (throw+ {:error_code          ce/ERR_NOT_FOUND
           :grouper_result_code (:resultCode response)
           :id                  (:id response)}))

(defn format-group
  [group]
  (when-not (nil? group)
    (->> {:description       (:description group)
          :display_extension (:displayExtension group)
          :display_name      (:displayName group)
          :extension         (:extension group)
          :id_index          (:idIndex group)
          :name              (:name group)
          :type              (:typeOfGroup group)
          :id                (:uuid group)}
         (remove-vals nil?))))

(defn format-group-detail
  [detail]
  (->> {:attribute_names     (:attributeNames detail)
        :attribute_values    (:attributeValues detail)
        :composite_type      (:compositeType detail)
        :created_at          (timestamp-to-millis (:createTime detail))
        :created_by          (:createSubjectId detail)
        :has_composite       (string-to-boolean (:hasComposite detail))
        :is_composite_factor (string-to-boolean (:isCompositeFactor detail))
        :left_group          (format-group (:leftGroup detail))
        :modified_at         (timestamp-to-millis (:modifyTime detail))
        :modified_by         (:modifySubjectId detail)
        :right_group         (format-group (:rightGroup detail))
        :type_names          (:typeNames detail)}
       (remove-vals nil?)))

(defn format-group-with-detail
  [group]
  (->> (assoc (format-group group)
         :detail (format-group-detail (:detail group)))
       (remove-vals nil?)))

(defn format-folder
  [folder]
  (when-not (nil? folder)
    (->> {:description       (:description folder)
          :display_extension (:displayExtension folder)
          :display_name      (:displayName folder)
          :extension         (:extension folder)
          :id_index          (:idIndex folder)
          :name              (:name folder)
          :id                (:uuid folder)}
         (remove-vals nil?))))

(defn format-subject
  [attribute-names subject]
  (condp = (:resultCode subject)
    "SUBJECT_NOT_FOUND" (not-found subject)
    (let [known-keys #{"mail" "givenName" "sn" "o"}
          known-mappings (keep-indexed #(if (contains? known-keys %2) [%2 %1]) attribute-names)
          known-key-indexes (into {} known-mappings)]
      (->> {:attribute_values  (keep-indexed #(if (not (contains? (set (map second known-mappings)) %1)) %2)
                                             (:attributeValues subject))
            :id                (:id subject)
            :name              (:name subject)
            :first_name        (nth (:attributeValues subject) (get known-key-indexes "givenName"))
            :last_name         (nth (:attributeValues subject) (get known-key-indexes "sn"))
            :email             (nth (:attributeValues subject) (get known-key-indexes "mail"))
            :institution       (nth (:attributeValues subject) (get known-key-indexes "o"))
            :source_id         (:sourceId subject)}
           (remove-vals nil?)
           (remove-vals empty?)))))

(defn format-privilege
  ([attribute-names privilege subject-key]
   (->> {:name      (:privilegeName privilege)
         :type      (:privilegeType privilege)
         :allowed   (string-to-boolean (:allowed privilege))
         :revokable (string-to-boolean (:revokable privilege))
         :group     (format-group (:wsGroup privilege))
         :folder    (format-folder (:wsStem privilege))
         :subject   (format-subject attribute-names (subject-key privilege))}
        (remove-vals nil?)))
  ([attribute-names privilege]
   (format-privilege attribute-names privilege :ownerSubject)))

(defn format-attribute-name
  [attribute-name]
  (when-not (nil? attribute-name)
    (->> {:description        (:description attribute-name)
          :display_extension  (:displayExtension attribute-name)
          :display_name       (:displayName attribute-name)
          :extension          (:extension attribute-name)
          :id_index           (:idIndex attribute-name)
          :name               (:name attribute-name)
          :id                 (:uuid attribute-name)
          :attribute_definition
            {:id   (:attributeDefId attribute-name)
             :name (:attributeDefName attribute-name)}}
         (remove-vals nil?))))

(defn format-attribute-assign ;; XXX: only supports group and membership attributes for now
                              ;; This is a bit weird because it supports both grouper WsAttributeAssign and WsPermissionAssign which are different for some reason
  [attribute-assign]
  (when-not (nil? attribute-assign)
    (->> {:id          (or (:id attribute-assign) (:attributeAssignId attribute-assign))
          :disallowed  (string-to-boolean (:disallowed attribute-assign))
          :enabled     (string-to-boolean (:enabled attribute-assign))
          :action_id   (:attributeAssignActionId attribute-assign)
          :action_name (or (:attributeAssignActionName attribute-assign) (:action attribute-assign))
          :action_type (:attributeAssignActionType attribute-assign)
          :delegatable (string-to-boolean (:attributeAssignDelegatable attribute-assign))
          :assign_type (:attributeAssignType attribute-assign)
          :created_at  (timestamp-to-millis (:createdOn attribute-assign))
          :modified_at (timestamp-to-millis (:lastUpdated attribute-assign))
          :group       (cond
                         (:ownerGroupId attribute-assign)
                           {:id   (:ownerGroupId attribute-assign)
                            :name (:ownerGroupName attribute-assign)}
                         (and (:roleId attribute-assign) (:roleName attribute-assign))
                           {:id   (:roleId attribute-assign)
                            :name (:roleName attribute-assign)})
          :membership  (cond
                         (:ownerMembershipId attribute-assign)
                           {:id (:ownerMembershipId attribute-assign)}
                         (:membershipId attribute-assign)
                           {:id (:membershipId attribute-assign)})
          :subject     (cond
                         (:ownerMemberSubjectId attribute-assign)
                           {:id (:ownerMemberSubjectId attribute-assign)
                            :source_id (:ownerMemberSourceId attribute-assign)}
                         (and (:subjectId attribute-assign) (:sourceId attribute-assign))
                           {:id (:subjectId attribute-assign)
                            :source_id (:sourceId attribute-assign)})
          :attribute_definition_name
            {:id   (:attributeDefNameId attribute-assign)
             :name (:attributeDefNameName attribute-assign)}
          :attribute_definition
            {:id   (:attributeDefId attribute-assign)
             :name (:attributeDefName attribute-assign)}}
         (remove-vals nil?))))
