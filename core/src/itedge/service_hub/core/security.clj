(ns itedge.service-hub.core.security  
  (:import org.mindrot.jbcrypt.BCrypt))

(defn hash-bcrypt
  "Hashes a given plaintext password using bcrypt and an optional
   :work-factor (defaults to 10 as of this writing).  Should be used to hash
   passwords included in stored user credentials that are to be later verified
   using `bcrypt-credential-fn`."
  [password & {:keys [work-factor]}]
  (BCrypt/hashpw password (if work-factor
                            (BCrypt/gensalt work-factor)
                            (BCrypt/gensalt))))

(defn- checkpw
  "Bcrypt password checking function, returns true if passwords match, false otherwise"
  [encrypted-pw pw]
  (BCrypt/checkpw encrypted-pw pw))

; memoize CPU intensive bcrypt function
(def memoized-checkpw (memoize checkpw))

(defn bcrypt-credential-fn
  "A bcrypt credentials function, returns credential map minus the password key from load-credentials-fn
   if the provided cleartext password matches the encrypted password in credential map, otherwise returns nil."
  [load-credentials-fn {:keys [username password]}]
  (when-let [creds (load-credentials-fn username)]
    (when (memoized-checkpw password (:password creds))
      (dissoc creds :password))))

(defn authorized?
  "Returns the first value in the :roles of the current authentication
   in the given identity map that isa? one of the required roles.
   Returns nil otherwise, indicating that the identity is not authorized
   for the set of required roles."
  [roles auth]
  (let [granted-roles (:roles auth)]
    (first (for [granted granted-roles
                 required roles
                 :when (isa? granted required)]
             granted))))



