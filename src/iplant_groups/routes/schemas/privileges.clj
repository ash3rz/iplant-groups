(ns iplant_groups.routes.schemas.privileges
  (:use [common-swagger-api.schema :only [describe]])
  (:require [iplant_groups.routes.schemas.subject :as subject]
            [iplant_groups.routes.schemas.group :as group]
            [iplant_groups.routes.schemas.folder :as folder]
            [schema.core :as s]))

(def ValidFolderPrivileges (s/enum "create" "stem" "stemAdmin" "stemAttrRead" "stemAttrUpdate"))
(def ValidGroupPrivileges (s/enum "view" "read" "update" "admin" "optin" "optout" "groupAttrRead" "groupAttrUpdate"))

(s/defschema Privilege
  {:type
   (describe String "The general type of privilege.")

   :name
   (describe String "The privilege name, under the type")

   (s/optional-key :allowed)
   (describe Boolean "Whether the privilege is marked allowed.")

   (s/optional-key :revokable)
   (describe Boolean "Whether the privilege is marked revokable.")

   :subject
   (describe subject/Subject "The subject/user with the privilege.")})

(s/defschema GroupPrivilege
  (assoc Privilege
         :group (describe group/Group "The group the permission applies to.")))

(s/defschema FolderPrivilege
  (assoc Privilege
         :folder (describe folder/Folder "The folder the permission applies to.")))

(s/defschema GroupPrivileges
  {:privileges (describe [GroupPrivilege] "A list of group-centric privileges")})

(s/defschema FolderPrivileges
  {:privileges (describe [FolderPrivilege] "A list of folder-centric privileges")})
