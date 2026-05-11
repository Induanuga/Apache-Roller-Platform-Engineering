# PlantUML Codes - Task 1

## SubSystem - I : Weblog and Content
### Core Domain Objects

```
@startuml
' Core Domain Objects - Weblog and Content Subsystem

package "org.apache.roller.weblogger.pojos" {
  
  abstract class WebloggerPojo {
    + getId() : String
    + setId(String)
  }
  
  class User <<Entity>> {
    - String userName
    - String password
    - String emailAddress
    - Date dateCreated
    + getWeblogs() : Set<Weblog>
  }
  
  class Weblog <<Entity>> {
    - String handle
    - String name
    - String tagline
    - String about
    - boolean enabled
    - Date dateCreated
    + getEntries() : Set<WeblogEntry>
    + getCategories() : Set<WeblogCategory>
    + getTemplates() : Set<WeblogTemplate>
    + getOwner() : User
  }
  
  class WeblogEntry <<Entity>> {
    - String title
    - String text
    - String summary
    - String status
    - Date pubTime
    - Date updateTime
    + getComments() : Set<WeblogComment>
    + getTags() : Set<WeblogEntryTag>
    + getWeblog() : Weblog
    + getCategory() : WeblogCategory
  }
  
  class WeblogCategory <<Entity>> {
    - String name
    - String description
    - String image
    + getWeblog() : Weblog
    + getEntries() : Set<WeblogEntry>
    + getParent() : WeblogCategory
    + getChildren() : Set<WeblogCategory>
  }
  
  class WeblogComment <<Entity>> {
    - String name
    - String email
    - String url
    - String content
    - String status
    - Date postTime
    + getWeblogEntry() : WeblogEntry
  }
  
  class WeblogEntryTag <<Entity>> {
    - String name
    - Date time
    + getWeblogEntry() : WeblogEntry
    + getWeblog() : Weblog
  }
  
  class WeblogTemplate <<Entity>> {
    - String name
    - String description
    - String link
    - String role
    - String content
    + getWeblog() : Weblog
  }
  
  class MediaFile <<Entity>> {
    - String name
    - String contentType
    - String directoryPath
    - Long length
    - Date dateUploaded
    + getWeblog() : Weblog
    + getCreator() : User
  }
  
  class WeblogPermission <<Entity>> {
    - String actions
    + getUser() : User
    + getWeblog() : Weblog
  }
  
  ' Inheritance relationships
  WebloggerPojo <|-- User
  WebloggerPojo <|-- Weblog
  WebloggerPojo <|-- WeblogEntry
  WebloggerPojo <|-- WeblogCategory
  WebloggerPojo <|-- WeblogComment
  WebloggerPojo <|-- WeblogEntryTag
  WebloggerPojo <|-- WeblogTemplate
  WebloggerPojo <|-- MediaFile
  WebloggerPojo <|-- WeblogPermission
  
  ' Association relationships
  Weblog "1" *-- "*" WeblogEntry : contains
  Weblog "1" *-- "*" WeblogCategory : organizes into
  Weblog "1" *-- "*" WeblogTemplate : uses
  Weblog "1" *-- "*" MediaFile : stores
  Weblog "1" o-- "1" User : owned by
  
  WeblogEntry "1" *-- "*" WeblogComment : receives
  WeblogEntry "1" *-- "*" WeblogEntryTag : tagged with
  WeblogEntry "1" o-- "1" WeblogCategory : categorized in
  
  WeblogCategory "1" o-- "*" WeblogCategory : hierarchical
  
  WeblogPermission "1" o-- "1" User : granted to
  WeblogPermission "1" o-- "1" Weblog : applies to
  
  WeblogComment "1" o-- "1" WeblogEntry : posted on
  
  MediaFile "1" o-- "1" User : uploaded by
}

@enduml
```

### Business Logic / Content Management Analysis

```
@startuml
' Business Logic Layer - Content Management

package "org.apache.roller.weblogger.business" {
  
  interface WeblogManager {
    + saveWeblog(Weblog) : void
    + removeWeblog(Weblog) : void
    + getWeblog(String handle) : Weblog
    + getWeblogsByUser(User) : List<Weblog>
    + getWeblogCount() : long
    + releaseWeblog(Weblog) : void
  }
  
  interface WeblogEntryManager {
    + saveWeblogEntry(WeblogEntry) : void
    + removeWeblogEntry(WeblogEntry) : void
    + getWeblogEntry(String id) : WeblogEntry
    + getWeblogEntries(WeblogEntrySearchCriteria) : List<WeblogEntry>
    + getNextEntry(WeblogEntry) : WeblogEntry
    + applyEntryTags(WeblogEntry, String[]) : void
  }
  
  interface CommentManager {
    + saveComment(WeblogComment) : void
    + removeComment(WeblogComment) : void
    + getCommentsByEntry(WeblogEntry) : List<WeblogComment>
    + getComments(CommentSearchCriteria) : List<WeblogComment>
    + restrictComments(Weblog) : void
    + getCommentCount() : long
  }
  
  interface MediaFileManager {
    + createMediaFile() : MediaFile
    + storeMediaFile(MediaFile, InputStream) : void
    + getMediaFile(String id) : MediaFile
    + getMediaFiles(Weblog) : List<MediaFile>
    + createThumbnail(MediaFile) : void
  }
  
  interface IndexManager {
    + addEntryIndexOperation(WeblogEntry) : void
    + addEntryReIndexOperation(WeblogEntry) : void
    + removeEntryIndexOperation(WeblogEntry) : void
    + search(String, Weblog) : WeblogHitList
    + rebuildIndex() : void
  }
  
  interface UserManager {
    + saveUser(User) : void
    + removeUser(User) : void
    + getUser(String name) : User
    + getUserByEmail(String email) : User
    + grantRole(String, String) : void
  }
  
  interface ThemeManager {
    + getTheme(String) : Theme
    + getEnabledThemes() : List<Theme>
    + getTemplateByLink(Weblog, String) : WeblogTemplate
  }
  
  class WebloggerFactory {
    - static instance : WebloggerFactory
    + getWeblogger() : WebloggerFactory
    + getWeblogManager() : WeblogManager
    + getWeblogEntryManager() : WeblogEntryManager
    + getCommentManager() : CommentManager
    + getMediaFileManager() : MediaFileManager
    + getIndexManager() : IndexManager
    + getUserManager() : UserManager
    + getThemeManager() : ThemeManager
    + flush() : void
    + release() : void
  }
  
  class PropertiesManager {
    - Properties dbProperties
    + getProperty(String) : String
    + setProperty(String, String) : void
    + save() : void
  }
  
  class URLStrategy {
    + getWeblogURL(Weblog, ...) : String
    + getWeblogEntryURL(WeblogEntry, ...) : String
    + getCommentURL(WeblogComment) : String
    + getMediaFileURL(MediaFile) : String
  }
  
  class TaskLockManager {
    + acquireLock(String) : boolean
    + releaseLock(String) : void
    + isLocked(String) : boolean
  }
  
  ' Factory pattern relationships
  WebloggerFactory ..> WeblogManager : creates
  WebloggerFactory ..> WeblogEntryManager : creates
  WebloggerFactory ..> CommentManager : creates
  WebloggerFactory ..> MediaFileManager : creates
  WebloggerFactory ..> IndexManager : creates
  WebloggerFactory ..> UserManager : creates
  WebloggerFactory ..> ThemeManager : creates
  
  ' Manager interactions with POJOs
  WeblogManager --> "1..*" Weblog : manages
  WeblogEntryManager --> "1..*" WeblogEntry : manages
  CommentManager --> "1..*" WeblogComment : manages
  MediaFileManager --> "1..*" MediaFile : manages
  UserManager --> "1..*" User : manages
  
  ' Cross-manager dependencies
  WeblogEntryManager --> WeblogManager : uses
  CommentManager --> WeblogEntryManager : uses
  IndexManager --> WeblogEntryManager : uses
  
  ' Strategy pattern
  URLStrategy --> Weblog : generates URLs for
  URLStrategy --> WeblogEntry : generates URLs for
  URLStrategy --> WeblogComment : generates URLs for
  URLStrategy --> MediaFile : generates URLs for
  
  ' Configuration
  PropertiesManager --> WebloggerFactory : configures
}

' Connect to Domain Objects from previous diagram
package "org.apache.roller.weblogger.pojos" {
  class Weblog <<Entity>>
  class WeblogEntry <<Entity>>
  class WeblogComment <<Entity>>
  class MediaFile <<Entity>>
  class User <<Entity>>
}

' Cross-package relationships
WeblogManager --> Weblog : manages
WeblogEntryManager --> WeblogEntry : manages
CommentManager --> WeblogComment : manages
MediaFileManager --> MediaFile : manages
UserManager --> User : manages

@enduml
```

### Web Layer (Controllers/Actions) and Rendering Engine

```
@startuml
' Web Layer and Rendering Engine

package "org.apache.roller.weblogger.ui.struts2" {
  
  abstract class BaseAction {
    # Weblog weblog
    # User authenticatedUser
    # Map<String, String> messages
    + execute() : String
    # loadWeblog() : void
    # hasRequiredPermissions() : boolean
  }
  
  class WeblogEntries extends BaseAction {
    - List<WeblogEntry> entries
    - Pager pager
    - Date startDate
    - Date endDate
    + getEntries() : List<WeblogEntry>
    + getPager() : Pager
    + getWeblog() : Weblog
  }
  
  class EntryAdd extends BaseAction {
    - WeblogEntry entry
    - String tags
    - boolean allowComments
    + save() : String
    + preview() : String
    + validate() : void
  }
  
  class EntryEdit extends EntryAdd {
    - String id
    + load() : String
    + update() : String
  }
  
  class CommentManagement extends BaseAction {
    - List<WeblogComment> comments
    - String entryId
    + list() : String
    + approve() : String
    + delete() : String
    + spam() : String
  }
  
  class MediaFileView extends BaseAction {
    - MediaFile mediaFile
    - String fileId
    + execute() : String
    + download() : String
  }
  
  class MediaFileAdd extends BaseAction {
    - File upload
    - String contentType
    - String directory
    + upload() : String
    + validate() : void
  }
  
  class WeblogConfig extends BaseAction {
    - Weblog weblog
    - String theme
    + edit() : String
    + save() : String
  }
}

package "org.apache.roller.weblogger.ui.rendering" {
  
  interface Renderer {
    + render(Map, Writer) : void
    + getContentType() : String
    + getContent() : String
  }
  
  abstract class AbstractRenderer implements Renderer {
    # Map<String, Object> model
    # String contentType
    + render(Map, Writer) : void
    # prepareModel(Map) : void
  }
  
  class WeblogPageRenderer extends AbstractRenderer {
    - Weblog weblog
    - String template
    - ModelLoader modelLoader
    + render(Map, Writer) : void
    - loadModel() : Map
    - getTemplate() : String
  }
  
  class FeedRenderer extends AbstractRenderer {
    - String format
    - List<WeblogEntry> entries
    + render(Map, Writer) : void
    - generateFeed() : SyndFeed
  }
  
  class ModelLoader {
    + loadModel(Weblog, Map) : Map
    + loadEntriesModel(Weblog, Date, Date) : List<WeblogEntry>
    + loadCommentsModel(WeblogEntry) : List<WeblogComment>
  }
  
  class RenderingManager {
    - Map<String, Renderer> renderers
    - Cache cache
    + getRenderer(String) : Renderer
    + getPage(Weblog, String) : String
    + flushCache(Weblog) : void
  }
  
  class CachingContentFilter {
    - Cache cache
    + doFilter(ServletRequest, ServletResponse, FilterChain) : void
    - isCacheable(HttpServletRequest) : boolean
    - storeInCache(String, byte[]) : void
  }
}

package "org.apache.roller.weblogger.business" {
  interface WeblogManager
  interface WeblogEntryManager
  interface CommentManager
  interface MediaFileManager
}

package "org.apache.roller.weblogger.pojos" {
  class Weblog
  class WeblogEntry
  class WeblogComment
  class MediaFile
  class User
}

' Inheritance relationships
BaseAction <|-- WeblogEntries
BaseAction <|-- EntryAdd
EntryAdd <|-- EntryEdit
BaseAction <|-- CommentManagement
BaseAction <|-- MediaFileView
BaseAction <|-- MediaFileAdd
BaseAction <|-- WeblogConfig

Renderer <|.. AbstractRenderer
AbstractRenderer <|-- WeblogPageRenderer
AbstractRenderer <|-- FeedRenderer

' Action dependencies on Business Layer
WeblogEntries --> WeblogEntryManager : uses
EntryAdd --> WeblogEntryManager : uses
CommentManagement --> CommentManager : uses
MediaFileView --> MediaFileManager : uses
MediaFileAdd --> MediaFileManager : uses
WeblogConfig --> WeblogManager : uses

' Rendering dependencies
WeblogPageRenderer --> Weblog : renders
WeblogPageRenderer --> ModelLoader : uses
FeedRenderer --> WeblogEntry : renders
RenderingManager --> Renderer : manages

' Action to POJO relationships
WeblogEntries --> WeblogEntry : displays
EntryAdd --> WeblogEntry : creates/modifies
CommentManagement --> WeblogComment : manages
MediaFileView --> MediaFile : displays
MediaFileAdd --> MediaFile : uploads

' BaseAction dependencies
BaseAction --> Weblog : has context
BaseAction --> User : authenticates

' Rendering to Business Layer
RenderingManager --> WeblogEntryManager : fetches data
ModelLoader --> WeblogEntryManager : loads data

@enduml
```

### COMPLETE THREE-TIER ARCHITECTURE OVERVIEW


```
@startuml
' Complete Three-Tier Architecture Diagram

package "Presentation Layer" {
  [Struts2 Actions] as Actions
  [JSP/Velocity Templates] as Templates
  [Rendering Engine] as Rendering
  [Caching Filter] as Cache
  
  Actions --> Templates : forwards to
  Actions --> Rendering : invokes
  Rendering --> Templates : uses
  Cache --> Actions : intercepts
}

package "Business Logic Layer" {
  [WeblogManager] as WMan
  [WeblogEntryManager] as EMan
  [CommentManager] as CMan
  [MediaFileManager] as MMan
  [IndexManager] as IMan
  [WebloggerFactory] as Factory
  
  Factory --> WMan : creates
  Factory --> EMan : creates
  Factory --> CMan : creates
  Factory --> MMan : creates
  Factory --> IMan : creates
  
  EMan --> WMan : uses
  CMan --> EMan : uses
  IMan --> EMan : uses
}

package "Domain Layer" {
  [Weblog] as W
  [WeblogEntry] as E
  [WeblogComment] as C
  [MediaFile] as M
  [User] as U
  
  W --> "*" E : contains
  E --> "*" C : has
  W --> "*" M : stores
  W --> "1" U : owned by
}

package "Persistence Layer" {
  [JPA/Hibernate] as ORM
  [Database] as DB
  
  ORM --> DB : persists to
}

' Cross-layer dependencies
Actions --> WMan : calls
Actions --> EMan : calls
Actions --> CMan : calls
Actions --> MMan : calls

WMan --> W : manages
EMan --> E : manages
CMan --> C : manages
MMan --> M : manages

W --> ORM : persisted by
E --> ORM : persisted by
C --> ORM : persisted by
M --> ORM : persisted by

' Data flow
note top of Actions
  Request Flow:
  1. HTTP Request → Struts2 Filter
  2. Route to Action → Business Manager
  3. Business Logic → Domain Object
  4. Persist via ORM → Database
  5. Load Model → Renderer
  6. Merge Template → Response
end note

@enduml
```
## SubSystem - II User and Role Management 
```
@startuml User_and_Role_Management_Subsystem
!pragma layout smetana
!pragma graphviz_dot jdot

!define PLANTUML_LIMIT_SIZE 32768

top to bottom direction
skinparam linetype ortho
skinparam dpi 120
skinparam nodesep 90
skinparam ranksep 120

skinparam classAttributeIconSize 0
skinparam classFontSize 12
skinparam classAttributeIconSize 0
skinparam shadowing false
skinparam backgroundColor #FFFFFF
skinparam class {
    BackgroundColor #F8F8F8
    BorderColor #333333
    ArrowColor #333333
}

' PACKAGES
package "org.apache.roller.weblogger.ui.core" {
    
    class RollerContext {
        - {static} log : Log
        - {static} servletContext : ServletContext
        - {static} encoder : DelegatingPasswordEncoder
        + RollerContext()
        + {static} getUIPluginManager() : UIPluginManager
        + {static} getServletContext() : ServletContext
        + {static} getPasswordEncoder() : PasswordEncoder
        + contextInitialized(sce : ServletContextEvent) : void
        + contextDestroyed(sce : ServletContextEvent) : void
        - setupVelocity() : void
        # initializeSecurityFeatures(context : ServletContext) : void
        - createPasswordEncoder() : DelegatingPasswordEncoder
        + {static} flushAuthenticationUserCache(userName : String) : void
        + {static} getAutoProvision() : AutoProvision
    }

    class CmaRollerContext {
        + CmaRollerContext()
        # initializeSecurityFeatures(context : ServletContext) : void
    }

    class RollerSession {
        - {static} serialVersionUID : long = 5890132909166913727L
        - userName : String
        - {static} log : Log
        + {static} ROLLER_SESSION : String = "org.apache.roller.weblogger.rollersession"
        + {static} getRollerSession(request : HttpServletRequest) : RollerSession
        + sessionCreated(se : HttpSessionEvent) : void
        + sessionDestroyed(se : HttpSessionEvent) : void
        + sessionWillPassivate(se : HttpSessionEvent) : void
        + getAuthenticatedUser() : User
        + setAuthenticatedUser(authenticatedUser : User) : void
        - clearSession(se : HttpSessionEvent) : void
    }

    class RollerLoginSessionManager {
        - {static} log : Log
        - {static} CACHE_ID : String = "roller.session.cache"
        - sessionCache : Cache
        + {static} getInstance() : RollerLoginSessionManager
        ~ RollerLoginSessionManager(cache : Cache)
        - RollerLoginSessionManager()
        + register(userName : String, session : RollerSession) : void
        + get(userName : String) : RollerSession
        + invalidate(userName : String) : void
    }

    class SessionCacheHandler {
        + invalidate(user : User) : void
    }

    class SingletonHolder {
        - {static} INSTANCE : RollerLoginSessionManager
    }

    RollerContext <|-- CmaRollerContext
    RollerLoginSessionManager +-- SessionCacheHandler
    RollerLoginSessionManager +-- SingletonHolder
}

package "org.apache.roller.weblogger.ui.core.security" {
    
    interface RollerUserDetails {
        + getTimeZone() : String
        + getLocale() : String
        + getScreenName() : String
        + getFullName() : String
        + getEmailAddress() : String
    }

    class RollerUserDetailsService {
        - {static} log : Log
        + loadUserByUsername(username : String) : UserDetails
        - getAuthorities(user : User, userManager : UserManager) : List<SimpleGrantedAuthority>
    }

    class CustomUserRegistry {
        - {static} LOG : Log
        - {static} DEFAULT_SNAME_LDAP_ATTRIBUTE : String = "displayName"
        - {static} DEFAULT_UID_LDAP_ATTRIBUTE : String = "uid"
        - {static} DEFAULT_NAME_LDAP_ATTRIBUTE : String = "cn"
        - {static} DEFAULT_EMAIL_LDAP_ATTRIBUTE : String = "mail"
        - {static} DEFAULT_LOCALE_LDAP_ATTRIBUTE : String = "preferredLanguage"
        - {static} DEFAULT_TIMEZONE_LDAP_ATTRIBUTE : String = "timeZone"
        - {static} SNAME_LDAP_PROPERTY : String = "roller.ldap.attributename.screenname"
        - {static} UID_LDAP_PROPERTY : String = "roller.ldap.attributename.uid"
        - {static} NAME_LDAP_PROPERTY : String = "roller.ldap.attributename.name"
        - {static} EMAIL_LDAP_PROPERTY : String = "roller.ldap.attributename.email"
        - {static} LOCALE_LDAP_PROPERTY : String = "roller.ldap.attributename.locale"
        - {static} TIMEZONE_LDAP_PROPERTY : String = "roller.ldap.attributename.timezone"
        + {static} getUserDetailsFromAuthentication(request : HttpServletRequest) : User
        - {static} getLdapAttribute(attributes : Attributes, propertyName : String) : String
        - {static} getRequestAttribute(request : HttpServletRequest, propertyName : String) : String
    }

    class AuthoritiesPopulator {
        - defaultRole : GrantedAuthority
        + getGrantedAuthorities(userData : DirContextOperations, username : String) : Collection<GrantedAuthority>
        + setDefaultRole(defaultRole : String) : void
    }

    class RollerRememberMeServices {
        - {static} log : Log
        + RollerRememberMeServices(userDetailsService : UserDetailsService)
        # makeTokenSignature(tokenExpiryTime : long, username : String, password : String) : String
    }

    class RollerRememberMeAuthenticationProvider {
        - {static} log : Log
        + RollerRememberMeAuthenticationProvider()
    }

    class BasicUserAutoProvision {
        - {static} log : Log
        + execute(request : HttpServletRequest) : boolean
        - hasNecessaryFields(user : User) : boolean
    }

    interface AutoProvision {
        + execute(request : HttpServletRequest) : boolean
    }

    BasicUserAutoProvision ..|> AutoProvision
    RollerUserDetailsService ..|> UserDetailsService
    AuthoritiesPopulator ..|> LdapAuthoritiesPopulator
    RollerRememberMeServices --|> TokenBasedRememberMeServices
    RollerRememberMeAuthenticationProvider --|> RememberMeAuthenticationProvider
}

package "org.apache.roller.weblogger.ui.core.filters" {
    
    class RoleAssignmentFilter {
        - {static} log : Log
        + doFilter(request : ServletRequest, response : ServletResponse, chain : FilterChain) : void
        + init(filterConfig : FilterConfig) : void
        + destroy() : void
    }

    class RoleAssignmentRequestWrapper {
        - {static} log : Log
        + RoleAssignmentRequestWrapper(request : HttpServletRequest)
        + isUserInRole(role : String) : boolean
    }

    class IPBanFilter {
        - {static} log : Log
        + init(filterConfig : FilterConfig) : void
        + doFilter(request : ServletRequest, response : ServletResponse, chain : FilterChain) : void
        + destroy() : void
    }

    class CustomOpenIDAuthenticationProcessingFilter {
        - {static} log : Log
        + attemptAuthentication(request : HttpServletRequest, response : HttpServletResponse) : Authentication
        # lookupRealm(identity : String) : String
    }

    RoleAssignmentRequestWrapper --|> HttpServletRequestWrapper
    IPBanFilter ..|> Filter
    RoleAssignmentFilter ..|> Filter
    CustomOpenIDAuthenticationProcessingFilter --|> OpenIDAuthenticationFilter
    CustomOpenIDAuthenticationProcessingFilter ..|> Filter
}

package "org.apache.roller.weblogger.ui.struts2.util" {
    
    class UISecurityInterceptor {
        - {static} serialVersionUID : long = 1L
        - {static} log : Log
        + doIntercept(invocation : ActionInvocation) : String
    }

    interface UISecurityEnforced {
        + isUserRequired() : boolean
        + isWeblogRequired() : boolean
        + requiredWeblogPermissionActions() : List<String>
        + requiredGlobalPermissionActions() : List<String>
    }

    abstract class UIAction {
        + {static} DENIED : String = "access-denied"
        + {static} LIST : String = "list"
        + {static} CANCEL : String = "cancel"
        - authenticatedUser : User
        - actionWeblog : Weblog
        - weblog : String
        # actionName : String
        # desiredMenu : String
        # pageTitle : String
        # salt : String
        + myPrepare() : void
        + setRequest(requestMap : Map<String, Object>) : void
        + getSalt() : String
        + setSalt(salt : String) : void
        + isUserRequired() : boolean
        + isWeblogRequired() : boolean
        + requiredWeblogPermissionActions() : List<String>
        + requiredGlobalPermissionActions() : List<String>
        + isUserIsAdmin() : boolean
        + cancel() : String
        + getSiteURL() : String
        + getAbsoluteSiteURL() : String
        + getProp(key : String) : String
        + getBooleanProp(key : String) : boolean
        + getIntProp(key : String) : int
        + getText(key : String) : String
        + getText(key : String, value : String) : String
        + getText(key : String, value0 : String, value1 : String) : String
        + getText(key : String, values : List<?>) : String
        + getText(key : String, values : String[]) : String
        + getText(key : String, arg0 : String, values : List<?>) : String
        + getText(key : String, arg0 : String, values : String[]) : String
        + addError(key : String) : void
        + addError(key : String, value : String) : void
        + addError(key : String, values : List<?>) : void
        + errorsExist() : boolean
        + addMessage(key : String) : void
        + addMessage(key : String, value : String) : void
        + addMessage(key : String, values : List<?>) : void
        + messagesExist() : boolean
        + getAuthenticatedUser() : User
        + setAuthenticatedUser(authenticatedUser : User) : void
        + getActionWeblog() : Weblog
        + setActionWeblog(actionWeblog : Weblog) : void
        + getWeblog() : String
        + setWeblog(weblog : String) : void
        + getPageTitle() : String
        + setPageTitle(pageTitle : String) : void
        + getActionName() : String
        + setActionName(actionName : String) : void
        + getDesiredMenu() : String
        + setDesiredMenu(desiredMenu : String) : void
        + getMenu() : Menu
        + getShortDateFormat() : String
        + getMediumDateFormat() : String
        + getLocalesList() : List<Locale>
        + getTimeZonesList() : List<String>
        + getHoursList() : List<Integer>
        + getMinutesList() : List<Integer>
        + getSecondsList() : List<Integer>
        + getCommentDaysList() : List<KeyValueObject>
        + {static} cleanTextKey(textKey : String) : String
        + {static} cleanTextArg(textArg : String) : String
        - cleanArgs(args : List<?>) : List<?>
    }

    class UIActionInterceptor {
        - {static} serialVersionUID : long = 1L
        - {static} log : Log
        + doIntercept(invocation : ActionInvocation) : String
    }

    UIAction ..|> UIActionPreparable
    UIAction ..|> UISecurityEnforced
    UIAction ..|> RequestAware
    UISecurityInterceptor --|> MethodFilterInterceptor
    UIActionInterceptor --|> MethodFilterInterceptor
}

package "org.apache.roller.weblogger.ui.struts2.core" {
    
    class Register {
        - {static} log : Log
        - {static} DISABLED_RETURN_CODE : String = "disabled"
        + {static} DEFAULT_ALLOWED_CHARS : String = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-."
        - servletRequest : HttpServletRequest
        - authMethod : AuthMethod
        - activationStatus : String
        - activationCode : String
        - bean : ProfileBean
        + Register()
        + isUserRequired() : boolean
        + isWeblogRequired() : boolean
        + getAuthMethod() : String
        + execute() : String
        + save() : String
        - retryActivationCode(userManager : UserManager, userName : String) : String
        - sendActivationMailIfNeeded(user : User, forceSend : boolean) : void
        + activate() : String
        + myValidate() : void
        - preserveUsernameAndPassword(username : String) : void
        - checkUsername() : void
        - checkOpenID() : void
        + getServletRequest() : HttpServletRequest
        + setServletRequest(request : HttpServletRequest) : void
        + getBean() : ProfileBean
        + setBean(bean : ProfileBean) : void
        + getActivationStatus() : String
        + setActivationStatus(status : String) : void
        + getActivationCode() : String
        + setActivationCode(code : String) : void
    }

    class Login {
        - error : String
        - authMethod : AuthMethod
        + Login()
        + isUserRequired() : boolean
        + isWeblogRequired() : boolean
        + getAuthMethod() : String
        + execute() : String
        + getError() : String
        + setError(error : String) : void
    }

    class Profile {
        - {static} log : Log
        - bean : ProfileBean
        - authMethod : AuthMethod
        + Profile()
        + isWeblogRequired() : boolean
        + execute() : String
        + save() : String
        + myValidate() : void
        + getAuthMethod() : String
        + getBean() : ProfileBean
        + setBean(bean : ProfileBean) : void
    }

    class ProfileBean {
        - id : String
        - userName : String
        - password : String
        - screenName : String
        - fullName : String
        - emailAddress : String
        - locale : String
        - timeZone : String
        - openIdUrl : String
        - passwordText : String
        - passwordConfirm : String
        + getId() : String
        + setId(id : String) : void
        + getUserName() : String
        + setUserName(userName : String) : void
        + getPassword() : String
        + setPassword(password : String) : void
        + getScreenName() : String
        + setScreenName(screenName : String) : void
        + getFullName() : String
        + setFullName(fullName : String) : void
        + getEmailAddress() : String
        + setEmailAddress(emailAddress : String) : void
        + getLocale() : String
        + setLocale(locale : String) : void
        + getTimeZone() : String
        + setTimeZone(timeZone : String) : void
        + getOpenIdUrl() : String
        + setOpenIdUrl(openIdUrl : String) : void
        + getPasswordText() : String
        + setPasswordText(passwordText : String) : void
        + getPasswordConfirm() : String
        + setPasswordConfirm(passwordConfirm : String) : void
        + copyTo(user : User) : void
        + copyFrom(user : User) : void
    }

    class OAuthAuthorize {
        - {static} log : Log
        - appDesc : String
        - token : String
        - callback : String
        + OAuthAuthorize()
        + isWeblogRequired() : boolean
        + execute() : String
        + getAppDesc() : String
        + getToken() : String
        + getCallback() : String
        + getUserName() : String
        + setServletRequest(request : HttpServletRequest) : void
    }

    class OAuthKeys {
        - {static} log : Log
        - userConsumer : OAuthConsumer
        - siteWideConsumer : OAuthConsumer
        + OAuthKeys()
        + isWeblogRequired() : boolean
        + execute() : String
        + getUserConsumer() : OAuthConsumer
        + getSiteWideConsumer() : OAuthConsumer
        + getRequestTokenURL() : String
        + getAuthorizationURL() : String
        + getAccessTokenURL() : String
    }

    class Install {
        - {static} log : Log
        - {static} DATABASE_ERROR : String = "database-error"
        - {static} CREATE_DATABASE : String = "create-database"
        - {static} UPGRADE_DATABASE : String = "upgrade-database"
        - {static} BOOTSTRAP : String = "bootstrap"
        - rootCauseException : Throwable
        - error : boolean
        - success : boolean
        - messages : List<String>
        - databaseName : String
        + isUserRequired() : boolean
        + isWeblogRequired() : boolean
        + execute() : String
        + create() : String
        + upgrade() : String
        + bootstrap() : String
        + getDatabaseProductName() : String
        + getProp(key : String) : String
        + getRootCauseException() : Throwable
        + getRootCauseStackTrace() : String
        + isUpgradeRequired() : boolean
        + isError() : boolean
        + getMessages() : List<String>
        + getDatabaseName() : String
        + isSuccess() : boolean
    }

    class Setup {
        - {static} LOG : Log
        - userCount : long
        - blogCount : long
        - frontpageBlog : String
        - aggregated : Boolean
        - weblogs : Collection<Weblog>
        + Setup()
        + isUserRequired() : boolean
        + isWeblogRequired() : boolean
        + execute() : String
        + save() : String
        + getUserCount() : long
        + setUserCount(userCount : long) : void
        + getBlogCount() : long
        + setBlogCount(blogCount : long) : void
        + getWeblogs() : Collection<Weblog>
        + setWeblogs(weblogs : Collection<Weblog>) : void
        + getFrontpageBlog() : String
        + setFrontpageBlog(frontpageBlog : String) : void
        + getAggregated() : Boolean
        + setAggregated(aggregated : Boolean) : void
    }

    class CreateWeblog {
        - {static} log : Log
        - {static} DISABLED_RETURN_CODE : String = "disabled"
        - bean : CreateWeblogBean
        + CreateWeblog()
        + isWeblogRequired() : boolean
        + execute() : String
        + save() : String
        + myValidate() : void
        + getThemes() : List<SharedTheme>
        + getBean() : CreateWeblogBean
        + setBean(bean : CreateWeblogBean) : void
    }

    class CreateWeblogBean {
        - handle : String
        - name : String
        - description : String
        - emailAddress : String
        - locale : String
        - timeZone : String
        - theme : String
        + getDescription() : String
        + setDescription(description : String) : void
        + getEmailAddress() : String
        + setEmailAddress(emailAddress : String) : void
        + getHandle() : String
        + setHandle(handle : String) : void
        + getLocale() : String
        + setLocale(locale : String) : void
        + getName() : String
        + setName(name : String) : void
        + getTheme() : String
        + setTheme(theme : String) : void
        + getTimeZone() : String
        + setTimeZone(timeZone : String) : void
    }

    Register --|> UIAction
    Login --|> UIAction
    Profile --|> UIAction
    OAuthAuthorize --|> UIAction
    OAuthKeys --|> UIAction
    Install --|> UIAction
    Setup --|> UIAction
    CreateWeblog --|> UIAction
}

package "org.apache.roller.weblogger.ui.struts2.admin" {
    
    class UserAdmin {
        - bean : CreateUserBean
        - authMethod : AuthMethod
        + UserAdmin()
        + requiredGlobalPermissionActions() : List<String>
        + isWeblogRequired() : boolean
        + execute() : String
        + edit() : String
        + getAuthMethod() : String
        + getBean() : CreateUserBean
        + setBean(bean : CreateUserBean) : void
    }

    class UserEdit {
        - {static} log : Log
        - bean : CreateUserBean
        - user : User
        - authMethod : AuthMethod
        + UserEdit()
        + setPageTitle(pageTitle : String) : void
        + requiredGlobalPermissionActions() : List<String>
        + isWeblogRequired() : boolean
        + myPrepare() : void
        + execute() : String
        + firstSave() : String
        + save() : String
        - isAdd() : boolean
        - myValidate() : void
        + getBean() : CreateUserBean
        + setBean(bean : CreateUserBean) : void
        + isUserEditingSelf() : boolean
        + getPermissions() : List<WeblogPermission>
        + getAuthMethod() : String
    }

    class CreateUserBean {
        - id : String
        - userName : String
        - password : String
        - screenName : String
        - fullName : String
        - emailAddress : String
        - locale : String
        - timeZone : String
        - openIdUrl : String
        - enabled : Boolean
        - activationCode : String
        - administrator : boolean
        - list : List<String>
        + getList() : List<String>
        + setList(list : List<String>) : void
        + getId() : String
        + setId(id : String) : void
        + getUserName() : String
        + setUserName(userName : String) : void
        + getPassword() : String
        + setPassword(password : String) : void
        + getScreenName() : String
        + setScreenName(screenName : String) : void
        + getFullName() : String
        + setFullName(fullName : String) : void
        + getEmailAddress() : String
        + setEmailAddress(emailAddress : String) : void
        + getLocale() : String
        + setLocale(locale : String) : void
        + getTimeZone() : String
        + setTimeZone(timeZone : String) : void
        + getOpenIdUrl() : String
        + setOpenIdUrl(openIdUrl : String) : void
        + getEnabled() : Boolean
        + setEnabled(enabled : Boolean) : void
        + getActivationCode() : String
        + setActivationCode(activationCode : String) : void
        + isAdministrator() : boolean
        + setAdministrator(administrator : boolean) : void
        + copyTo(user : User) : void
        + copyFrom(user : User) : void
    }

    class GlobalConfig {
        - {static} log : Log
        - params : HttpParameters
        - properties : Map<String, RuntimeConfigProperty>
        - globalConfigDef : ConfigDef
        - pluginsList : List<WeblogEntryCommentPlugin>
        - commentPlugins : String[]
        - httpMethod : String
        - bundle : ResourceBundle
        - weblogs : Collection<Weblog>
        + GlobalConfig()
        + isWeblogRequired() : boolean
        + requiredGlobalPermissionActions() : List<String>
        + myPrepare() : void
        + execute() : String
        + save() : String
        + setParameters(params : HttpParameters) : void
        - getParameter(name : String) : String
        + getProperties() : Map<String, RuntimeConfigProperty>
        + setProperties(properties : Map<String, RuntimeConfigProperty>) : void
        + getGlobalConfigDef() : ConfigDef
        + setGlobalConfigDef(globalConfigDef : ConfigDef) : void
        + getPluginsList() : List<WeblogEntryCommentPlugin>
        + setPluginsList(pluginsList : List<WeblogEntryCommentPlugin>) : void
        + getCommentPlugins() : String[]
        + setCommentPlugins(commentPlugins : String[]) : void
        + setServletRequest(request : HttpServletRequest) : void
        + getWeblogs() : Collection<Weblog>
        + setWeblogs(weblogs : Collection<Weblog>) : void
    }

    class GlobalCommentManagement {
        - {static} log : Log
        - {static} COUNT : int = 20
        - bean : GlobalCommentManagementBean
        - pager : CommentsPager
        - firstComment : WeblogEntryComment
        - lastComment : WeblogEntryComment
        - bulkDeleteCount : int
        - httpMethod : String
        + GlobalCommentManagement()
        + requiredGlobalPermissionActions() : List<String>
        + isWeblogRequired() : boolean
        + loadComments() : void
        - buildBaseUrl() : String
        + execute() : String
        + query() : String
        + delete() : String
        + update() : String
        + getCommentStatusOptions() : List<KeyValueObject>
        + getBean() : GlobalCommentManagementBean
        + setBean(bean : GlobalCommentManagementBean) : void
        + getBulkDeleteCount() : int
        + setBulkDeleteCount(bulkDeleteCount : int) : void
        + getFirstComment() : WeblogEntryComment
        + setFirstComment(firstComment : WeblogEntryComment) : void
        + getLastComment() : WeblogEntryComment
        + setLastComment(lastComment : WeblogEntryComment) : void
        + getPager() : CommentsPager
        + setPager(pager : CommentsPager) : void
        + setServletRequest(request : HttpServletRequest) : void
    }

    class GlobalCommentManagementBean {
        - searchString : String
        - startDateString : String
        - endDateString : String
        - approvedString : String
        - page : int
        - spamComments : String[]
        - deleteComments : String[]
        - ids : String
        + loadCheckboxes(comments : List<WeblogEntryComment>) : void
        + getStatus() : ApprovalStatus
        + getStartDate() : Date
        + getEndDate() : Date
        + getPendingString() : String
        + setPendingString(pendingString : String) : void
        + getIds() : String
        + setIds(ids : String) : void
        + getSearchString() : String
        + setSearchString(searchString : String) : void
        + getSpamComments() : String[]
        + setSpamComments(spamComments : String[]) : void
        + getDeleteComments() : String[]
        + setDeleteComments(deleteComments : String[]) : void
        + getApprovedString() : String
        + setApprovedString(approvedString : String) : void
        + getPage() : int
        + setPage(page : int) : void
        + getStartDateString() : String
        + setStartDateString(startDateString : String) : void
        + getEndDateString() : String
        + setEndDateString(endDateString : String) : void
    }

    UserAdmin --|> UIAction
    UserEdit --|> UIAction
    GlobalConfig --|> UIAction
    GlobalCommentManagement --|> UIAction
}

package "org.apache.roller.weblogger.ui.struts2.editor" {
    
    class Members {
        - {static} log : Log
        - parameters : HttpParameters
        + Members()
        + execute() : String
        + save() : String
        - getParameter(name : String) : String
        + getParameters() : Map<String, Parameter>
        + setParameters(parameters : HttpParameters) : void
        + getWeblogPermissions() : List<WeblogPermission>
    }

    class MembersInvite {
        - {static} log : Log
        - userName : String
        - permissionString : String
        + MembersInvite()
        + execute() : String
        + save() : String
        + cancel() : String
        + getUserName() : String
        + setUserName(userName : String) : void
        + getPermissionString() : String
        + setPermissionString(permissionString : String) : void
    }

    class MemberResign {
        - {static} log : Log
        + MemberResign()
        + requiredWeblogPermissionActions() : List<String>
        + isWeblogRequired() : boolean
        + execute() : String
        + resign() : String
    }

    Members --|> UIAction
    MembersInvite --|> UIAction
    MemberResign --|> UIAction
}

package "org.apache.roller.weblogger.util" {
    
    class PasswordUtility {
        + {static} main(args : String[]) : void
        + {static} createConnection(props : Properties, dbUrl : String) : Connection
        - {static} savePasswords(conn : Connection, dbUrl : String) : void
        - {static} encryptionOn(conn : Connection, dbUrl : String) : void
        - {static} encryptionOff(conn : Connection, dbUrl : String) : void
        - {static} resetPassword(conn : Connection, dbUrl : String, userName : String, password : String) : void
        - {static} grantAdmin(conn : Connection, dbUrl : String) : void
        - {static} revokeAdmin(conn : Connection, dbUrl : String) : void
    }

    class WSSEUtilities {
        + {static} generateDigest(secret : byte[], nonce : byte[], created : byte[]) : String
        + {static} base64Decode(data : String) : byte[]
        + {static} base64Encode(data : byte[]) : String
        + {static} generateWSSEHeader(username : String, password : String) : String
    }

    class IPBanList {
        - {static} log : Log
        - bannedIps : Set<String>
        - bannedIpsFile : ModifiedFile
        - {static} instance : IPBanList
        ~ IPBanList(fileSupplier : Supplier<String>)
        + {static} getInstance() : IPBanList
        + isBanned(ip : String) : boolean
        + addBannedIp(ip : String) : void
        - loadBannedIpsIfNeeded() : void
        - loadBannedIps() : void
    }

    class ModifiedFile {
        - file : Path
        - lastModified : long
        + ModifiedFile(file : Path)
        + isModified() : boolean
        + updateLastModified() : void
    }

    IPBanList +-- ModifiedFile
}

package "org.apache.roller.weblogger.pojos" {
    
    class User {
        - {static} serialVersionUID : long = 542051327534800514L
        - id : String
        - userName : String
        - password : String
        - openIdUrl : String
        - screenName : String
        - fullName : String
        - emailAddress : String
        - dateCreated : Date
        - locale : String
        - timeZone : String
        - enabled : Boolean
        - activationCode : String
        + User()
        + User(id : String, userName : String, password : String, openIdUrl : String, screenName : String, fullName : String, emailAddress : String, dateCreated : Date, enabled : Boolean)
        + getId() : String
        + setId(id : String) : void
        + getUserName() : String
        + setUserName(userName : String) : void
        + getPassword() : String
        + setPassword(password : String) : void
        + resetPassword(newPassword : String) : void
        + getOpenIdUrl() : String
        + setOpenIdUrl(openIdUrl : String) : void
        + getScreenName() : String
        + setScreenName(screenName : String) : void
        + getFullName() : String
        + setFullName(fullName : String) : void
        + getEmailAddress() : String
        + setEmailAddress(emailAddress : String) : void
        + getDateCreated() : Date
        + setDateCreated(dateCreated : Date) : void
        + getLocale() : String
        + setLocale(locale : String) : void
        + getTimeZone() : String
        + setTimeZone(timeZone : String) : void
        + getEnabled() : Boolean
        + setEnabled(enabled : Boolean) : void
        + getActivationCode() : String
        + setActivationCode(activationCode : String) : void
        + hasGlobalPermission(action : String) : boolean
        + hasGlobalPermissions(actions : List<String>) : boolean
        + toString() : String
        + equals(obj : Object) : boolean
        + hashCode() : int
    }

    class UserRole {
        + {static} serialVersionUID : long = -2268627359589433880L
        - id : String
        - userName : String
        - role : String
        + UserRole()
        + UserRole(id : String, role : String)
        + getId() : String
        + setId(id : String) : void
        + getUserName() : String
        + setUserName(userName : String) : void
        + getRole() : String
        + setRole(role : String) : void
        + toString() : String
        + equals(obj : Object) : boolean
        + hashCode() : int
    }

    abstract class RollerPermission {
        + RollerPermission(name : String)
        + {abstract} setActions(actions : String) : void
        + {abstract} getActions() : String
        + getActionsAsList() : List<String>
        + setActionsAsList(actions : List<String>) : void
        + hasAction(action : String) : boolean
        + hasActions(actions : List<String>) : boolean
        + addActions(permission : ObjectPermission) : void
        + addActions(actions : List<String>) : void
        + removeActions(actions : List<String>) : void
        + isEmpty() : boolean
    }

    class GlobalPermission {
        + {static} LOGIN : String = "login"
        + {static} WEBLOG : String = "weblog"
        + {static} ADMIN : String = "admin"
        # actions : String
        + GlobalPermission(user : User)
        + GlobalPermission(actions : List<String>)
        + GlobalPermission(user : User, actions : List<String>)
        + implies(permission : Permission) : boolean
        - actionImplies(impliedAction : String, testAction : String) : boolean
        + toString() : String
        + setActions(actions : String) : void
        + getActions() : String
        + equals(obj : Object) : boolean
        + hashCode() : int
    }

    class WeblogPermission {
        + {static} EDIT_DRAFT : String = "edit_draft"
        + {static} POST : String = "post"
        + {static} ADMIN : String = "admin"
        + {static} ALL_ACTIONS : List<String>
        + WeblogPermission()
        + WeblogPermission(weblog : Weblog, user : User, actions : String)
        + WeblogPermission(weblog : Weblog, user : User, actions : List<String>)
        + WeblogPermission(weblog : Weblog, actions : List<String>)
        + getWeblog() : Weblog
        + getUser() : User
        + implies(permission : Permission) : boolean
        + toString() : String
        + equals(obj : Object) : boolean
        + hashCode() : int
    }

    abstract class ObjectPermission {
        # objectId : String
        # objectType : String
        # userName : String
        # pending : boolean
        + ObjectPermission(name : String)
        + getObjectId() : String
        + setObjectId(objectId : String) : void
        + getObjectType() : String
        + setObjectType(objectType : String) : void
        + getUserName() : String
        + setUserName(userName : String) : void
        + isPending() : boolean
        + setPending(pending : boolean) : void
    }

    class Weblog {
        + {static} serialVersionUID : long = 2723587098422409561L
        - {static} log : Log
        - {static} MAX_ENTRIES : int = 65535
        - id : String
        - handle : String
        - name : String
        - tagline : String
        - enableBloggerApi : Boolean
        - editorPage : String
        - bannedwordslist : String
        - allowComments : Boolean
        - emailComments : Boolean
        - emailAddress : String
        - editorTheme : String
        - locale : String
        - timeZone : String
        - defaultPlugins : String
        - visible : Boolean
        - active : Boolean
        - dateCreated : Date
        - defaultAllowComments : Boolean
        - defaultCommentDays : int
        - moderateComments : Boolean
        - entryDisplayCount : int
        - lastModified : Date
        - enableMultiLang : boolean
        - showAllLangs : boolean
        - iconPath : String
        - about : String
        - creator : String
        - analyticsCode : String
        - bloggerCategory : WeblogCategory
        - initializedPlugins : Map<String, WeblogEntryPlugin>
        - weblogCategories : List<WeblogCategory>
        - bookmarkFolders : List<WeblogBookmarkFolder>
        - mediaFileDirectories : List<MediaFileDirectory>
        + Weblog()
        + Weblog(id : String, handle : String, name : String, tagline : String, emailAddress : String, editorTheme : String, locale : String, timeZone : String)
        + toString() : String
        + equals(obj : Object) : boolean
        + hashCode() : int
        + getTheme() : WeblogTheme
        + getId() : String
        + setId(id : String) : void
        + getHandle() : String
        + setHandle(handle : String) : void
        + getName() : String
        + setName(name : String) : void
        + getTagline() : String
        + setTagline(tagline : String) : void
        + getCreator() : User
        + getCreatorUserName() : String
        + setCreatorUserName(creatorUserName : String) : void
        + getEnableBloggerApi() : Boolean
        + setEnableBloggerApi(enableBloggerApi : Boolean) : void
        + getBloggerCategory() : WeblogCategory
        + setBloggerCategory(bloggerCategory : WeblogCategory) : void
        + getEditorPage() : String
        + setEditorPage(editorPage : String) : void
        + getBannedwordslist() : String
        + setBannedwordslist(bannedwordslist : String) : void
        + getAllowComments() : Boolean
        + setAllowComments(allowComments : Boolean) : void
        + getDefaultAllowComments() : Boolean
        + setDefaultAllowComments(defaultAllowComments : Boolean) : void
        + getDefaultCommentDays() : int
        + setDefaultCommentDays(defaultCommentDays : int) : void
        + getModerateComments() : Boolean
        + setModerateComments(moderateComments : Boolean) : void
        + getEmailComments() : Boolean
        + setEmailComments(emailComments : Boolean) : void
        + getEmailAddress() : String
        + setEmailAddress(emailAddress : String) : void
        + getEditorTheme() : String
        + setEditorTheme(editorTheme : String) : void
        + getLocale() : String
        + setLocale(locale : String) : void
        + getTimeZone() : String
        + setTimeZone(timeZone : String) : void
        + getDateCreated() : Date
        + setDateCreated(dateCreated : Date) : void
        + getDefaultPlugins() : String
        + setDefaultPlugins(defaultPlugins : String) : void
        + setData(weblog : Weblog) : void
        + getLocaleInstance() : Locale
        + getTimeZoneInstance() : TimeZone
        + hasUserPermission(user : User, action : String) : boolean
        + hasUserPermissions(user : User, actions : List<String>) : boolean
        + getEntryDisplayCount() : int
        + setEntryDisplayCount(entryDisplayCount : int) : void
        + getVisible() : Boolean
        + setVisible(visible : Boolean) : void
        + getActive() : Boolean
        + setActive(active : Boolean) : void
        + getCommentModerationRequired() : boolean
        + setCommentModerationRequired(commentModerationRequired : boolean) : void
        + getLastModified() : Date
        + setLastModified(lastModified : Date) : void
        + isEnableMultiLang() : boolean
        + setEnableMultiLang(enableMultiLang : boolean) : void
        + isShowAllLangs() : boolean
        + setShowAllLangs(showAllLangs : boolean) : void
        + getURL() : String
        + getAbsoluteURL() : String
        + getIconPath() : String
        + setIconPath(iconPath : String) : void
        + getAnalyticsCode() : String
        + setAnalyticsCode(analyticsCode : String) : void
        + getAbout() : String
        + setAbout(about : String) : void
        + getInitializedPlugins() : Map<String, WeblogEntryPlugin>
        + getWeblogEntry(anchor : String) : WeblogEntry
        + getWeblogCategory(name : String) : WeblogCategory
        + getRecentWeblogEntries(categoryName : String, maxEntries : int) : List<WeblogEntry>
        + getRecentWeblogEntriesByTag(tagName : String, maxEntries : int) : List<WeblogEntry>
        + getRecentComments(maxComments : int) : List<WeblogEntryComment>
        + getBookmarkFolder(name : String) : WeblogBookmarkFolder
        + getTodaysHits() : int
        + getPopularTags(length : int, offset : int) : List<TagStat>
        + getCommentCount() : long
        + getEntryCount() : long
        + addCategory(category : WeblogCategory) : void
        + getWeblogCategories() : List<WeblogCategory>
        + setWeblogCategories(weblogCategories : List<WeblogCategory>) : void
        + hasCategory(name : String) : boolean
        + getBookmarkFolders() : List<WeblogBookmarkFolder>
        + setBookmarkFolders(bookmarkFolders : List<WeblogBookmarkFolder>) : void
        + getMediaFileDirectories() : List<MediaFileDirectory>
        + setMediaFileDirectories(mediaFileDirectories : List<MediaFileDirectory>) : void
        + addBookmarkFolder(folder : WeblogBookmarkFolder) : void
        + hasBookmarkFolder(name : String) : boolean
        + hasMediaFileDirectory(name : String) : boolean
        + getMediaFileDirectory(name : String) : MediaFileDirectory
    }

    RollerPermission <|-- GlobalPermission
    RollerPermission <|-- ObjectPermission
    ObjectPermission <|-- WeblogPermission
}

package "org.apache.roller.weblogger.pojos.wrapper" {
    
    class UserWrapper {
        - pojo : User
        - UserWrapper(user : User)
        + {static} wrap(user : User) : UserWrapper
        + getUserName() : String
        + getScreenName() : String
        + getFullName() : String
        + getEmailAddress() : String
        + getDateCreated() : Date
        + getLocale() : String
        + getTimeZone() : String
    }
}

package "org.apache.roller.weblogger.business" {
    
    interface UserManager {
        + addUser(user : User) : void
        + saveUser(user : User) : void
        + removeUser(user : User) : void
        + getUserCount() : long
        + getUserByActivationCode(activationCode : String) : User
        + getUser(id : String) : User
        + getUserByUserName(userName : String) : User
        + getUserByUserName(userName : String, enabledOnly : Boolean) : User
        + getUserByOpenIdUrl(openIdUrl : String) : User
        + getUsers(enabledOnly : Boolean, startDate : Date, endDate : Date, offset : int, length : int) : List<User>
        + getUsersStartingWith(startsWith : String, enabledOnly : Boolean, offset : int, length : int) : List<User>
        + getUserNameLetterMap() : Map<String, Long>
        + getUsersByLetter(letter : char, offset : int, length : int) : List<User>
        + checkPermission(permission : RollerPermission, user : User) : boolean
        + grantWeblogPermission(weblog : Weblog, user : User, actions : List<String>) : void
        + grantWeblogPermissionPending(weblog : Weblog, user : User, actions : List<String>) : void
        + confirmWeblogPermission(weblog : Weblog, user : User) : void
        + declineWeblogPermission(weblog : Weblog, user : User) : void
        + revokeWeblogPermission(weblog : Weblog, user : User, actions : List<String>) : void
        + getWeblogPermissions(user : User) : List<WeblogPermission>
        + getPendingWeblogPermissions(user : User) : List<WeblogPermission>
        + getWeblogPermissions(weblog : Weblog) : List<WeblogPermission>
        + getPendingWeblogPermissions(weblog : Weblog) : List<WeblogPermission>
        + getWeblogPermissionsIncludingPending(weblog : Weblog) : List<WeblogPermission>
        + getWeblogPermission(weblog : Weblog, user : User) : WeblogPermission
        + getWeblogPermissionIncludingPending(weblog : Weblog, user : User) : WeblogPermission
        + grantRole(role : String, user : User) : void
        + revokeRole(role : String, user : User) : void
        + hasRole(role : String, user : User) : boolean
        + getRoles(user : User) : List<String>
        + release() : void
    }

    interface OAuthManager {
        + getServiceProvider() : OAuthServiceProvider
        + getValidator() : OAuthValidator
        + getConsumer() : OAuthConsumer
        + getConsumerByUsername(username : String) : OAuthConsumer
        + getConsumer(message : OAuthMessage) : OAuthConsumer
        + addConsumer(username : String) : OAuthConsumer
        + addConsumer(username : String, consumerKey : String) : OAuthConsumer
        + getAccessor(message : OAuthMessage) : OAuthAccessor
        + markAsAuthorized(accessor : OAuthAccessor, username : String) : void
        + generateRequestToken(accessor : OAuthAccessor) : void
        + generateAccessToken(accessor : OAuthAccessor) : void
    }

    interface PropertiesManager {
        + initialize() : void
        + release() : void
        + saveProperty(property : RuntimeConfigProperty) : void
        + saveProperties(properties : Map<String, RuntimeConfigProperty>) : void
        + getProperty(key : String) : RuntimeConfigProperty
        + getProperties() : Map<String, RuntimeConfigProperty>
    }

    class WebloggerFactory {
        - {static} LOG : Log
        - {static} webloggerProvider : WebloggerProvider
        - WebloggerFactory()
        + {static} isBootstrapped() : boolean
        + {static} getWeblogger() : Weblogger
        + {static} bootstrap() : void
        + {static} bootstrap(provider : WebloggerProvider) : void
    }

    interface Weblogger {
        + getUserManager() : UserManager
        + getBookmarkManager() : BookmarkManager
        + getOAuthManager() : OAuthManager
        + getWeblogManager() : WeblogManager
        + getWeblogEntryManager() : WeblogEntryManager
        + getAutopingManager() : AutoPingManager
        + getPingTargetManager() : PingTargetManager
        + getPingQueueManager() : PingQueueManager
        + getPropertiesManager() : PropertiesManager
        + getThreadManager() : ThreadManager
        + getIndexManager() : IndexManager
        + getThemeManager() : ThemeManager
        + getPluginManager() : PluginManager
        + getMediaFileManager() : MediaFileManager
        + getFileContentManager() : FileContentManager
        + getUrlStrategy() : URLStrategy
        + flush() : void
        + release() : void
        + initialize() : void
        + shutdown() : void
        + getVersion() : String
        + getRevision() : String
        + getBuildTime() : String
        + getBuildUser() : String
        + getFeedFetcher() : FeedFetcher
        + getPlanetManager() : PlanetManager
        + getPlanetURLStrategy() : PlanetURLStrategy
    }
}

package "org.apache.roller.weblogger.business.jpa" {
    
    class JPAUserManagerImpl {
        - {static} log : Log
        - strategy : JPAPersistenceStrategy
        - userNameToIdMap : Map<String, String>
        # JPAUserManagerImpl(strategy : JPAPersistenceStrategy)
        + release() : void
        + saveUser(user : User) : void
        + removeUser(user : User) : void
        + addUser(user : User) : void
        + getUser(id : String) : User
        + getUserByUserName(userName : String) : User
        + getUserByOpenIdUrl(openIdUrl : String) : User
        + getUserByUserName(userName : String, enabledOnly : Boolean) : User
        + getUsers(enabledOnly : Boolean, startDate : Date, endDate : Date, offset : int, length : int) : List<User>
        + getUsersStartingWith(startsWith : String, enabledOnly : Boolean, offset : int, length : int) : List<User>
        + getUserNameLetterMap() : Map<String, Long>
        + getUsersByLetter(letter : char, offset : int, length : int) : List<User>
        + getUserCount() : long
        + getUserByActivationCode(activationCode : String) : User
        + checkPermission(permission : RollerPermission, user : User) : boolean
        + getWeblogPermission(weblog : Weblog, user : User) : WeblogPermission
        + getWeblogPermissionIncludingPending(weblog : Weblog, user : User) : WeblogPermission
        + grantWeblogPermission(weblog : Weblog, user : User, actions : List<String>) : void
        + grantWeblogPermissionPending(weblog : Weblog, user : User, actions : List<String>) : void
        + confirmWeblogPermission(weblog : Weblog, user : User) : void
        + declineWeblogPermission(weblog : Weblog, user : User) : void
        + revokeWeblogPermission(weblog : Weblog, user : User, actions : List<String>) : void
        + getWeblogPermissions(user : User) : List<WeblogPermission>
        + getWeblogPermissions(weblog : Weblog) : List<WeblogPermission>
        + getWeblogPermissionsIncludingPending(weblog : Weblog) : List<WeblogPermission>
        + getPendingWeblogPermissions(user : User) : List<WeblogPermission>
        + getPendingWeblogPermissions(weblog : Weblog) : List<WeblogPermission>
        + hasRole(role : String, user : User) : boolean
        + getRoles(user : User) : List<String>
        + grantRole(role : String, user : User) : void
        + revokeRole(role : String, user : User) : void
    }

    class JPAOAuthManagerImpl {
        - roller : Weblogger
        - strategy : JPAPersistenceStrategy
        - validator : OAuthValidator
        - {static} log : Log
        + JPAOAuthManagerImpl(roller : Weblogger, strategy : JPAPersistenceStrategy, validator : OAuthValidator)
        + getServiceProvider() : OAuthServiceProvider
        + getValidator() : OAuthValidator
        + getConsumer(message : OAuthMessage) : OAuthConsumer
        + getAccessor(message : OAuthMessage) : OAuthAccessor
        + markAsAuthorized(accessor : OAuthAccessor, username : String) : void
        + generateRequestToken(accessor : OAuthAccessor) : void
        + generateAccessToken(accessor : OAuthAccessor) : void
        + addConsumer(username : String, consumerKey : String) : OAuthConsumer
        + addConsumer(username : String) : OAuthConsumer
        + getConsumer() : OAuthConsumer
        + getConsumerByUsername(username : String) : OAuthConsumer
        ~ consumerFromRecord(record : OAuthConsumerRecord) : OAuthConsumer
        ~ accessorFromRecord(record : OAuthAccessorRecord) : OAuthAccessor
        ~ getConsumerByKey(key : String) : OAuthConsumer
        ~ addAccessor(accessor : OAuthAccessor) : void
        ~ getAccessorByKey(key : String) : OAuthAccessor
        ~ getAccessorByToken(token : String) : OAuthAccessor
        ~ removeConsumer(consumer : OAuthConsumer) : void
        ~ removeAccessor(accessor : OAuthAccessor) : void
    }

    JPAUserManagerImpl ..|> UserManager
    JPAOAuthManagerImpl ..|> OAuthManager
}

package "org.apache.roller.weblogger.webservices.oauth" {
    
    class AccessTokenServlet {
        # {static} log : Log
        + doGet(request : HttpServletRequest, response : HttpServletResponse) : void
        + doPost(request : HttpServletRequest, response : HttpServletResponse) : void
        + processRequest(request : HttpServletRequest, response : HttpServletResponse) : void
        + handleException(e : Exception, request : HttpServletRequest, response : HttpServletResponse, sendBody : boolean) : void
    }

    class AuthorizationServlet {
        # {static} log : Log
        + doGet(request : HttpServletRequest, response : HttpServletResponse) : void
        + doPost(request : HttpServletRequest, response : HttpServletResponse) : void
        - sendToAuthorizePage(request : HttpServletRequest, response : HttpServletResponse, accessor : OAuthAccessor) : void
        - returnToConsumer(request : HttpServletRequest, response : HttpServletResponse, accessor : OAuthAccessor) : void
        + handleException(e : Exception, request : HttpServletRequest, response : HttpServletResponse, sendBody : boolean) : void
    }

    class RequestTokenServlet {
        # {static} log : Log
        + doGet(request : HttpServletRequest, response : HttpServletResponse) : void
        + doPost(request : HttpServletRequest, response : HttpServletResponse) : void
        + processRequest(request : HttpServletRequest, response : HttpServletResponse) : void
        + handleException(e : Exception, request : HttpServletRequest, response : HttpServletResponse, sendBody : boolean) : void
    }

    AccessTokenServlet --|> HttpServlet
    AuthorizationServlet --|> HttpServlet
    RequestTokenServlet --|> HttpServlet
}

' RELATIONSHIPS BETWEEN PACKAGES

User "1" -- "0..*" UserRole : has >
User "1" -- "0..*" WeblogPermission : has >
User "1" -- "1" GlobalPermission : has >
Weblog "1" -- "0..*" WeblogPermission : has >
UserManager -- User : manages >
UserManager -- WeblogPermission : manages >
UserManager -- UserRole : manages >
JPAUserManagerImpl -- User : implements management >
JPAUserManagerImpl -- WeblogPermission : implements management >
JPAUserManagerImpl -- UserRole : implements management >
OAuthManager -- User : uses >
JPAOAuthManagerImpl -- User : implements OAuth for >
RollerSession -- User : contains >
RollerLoginSessionManager -- RollerSession : manages >
RollerContext -- RollerLoginSessionManager : uses >
Register -- ProfileBean : uses >
Register -- User : creates >
Profile -- ProfileBean : uses >
Profile -- User : edits >
UserEdit -- CreateUserBean : uses >
UserEdit -- User : manages >
UserAdmin -- CreateUserBean : uses >
CreateWeblog -- CreateWeblogBean : uses >
CreateWeblog -- Weblog : creates >
Members -- WeblogPermission : manages >
MembersInvite -- User : invites >
MembersInvite -- Weblog : to >
MemberResign -- Weblog : resigns from >
GlobalConfig -- User : configured by >
GlobalCommentManagement -- GlobalCommentManagementBean : uses >
OAuthAuthorize -- User : authorizes >
OAuthKeys -- User : shows keys for >
CustomUserRegistry -- User : creates from LDAP >
BasicUserAutoProvision -- User : auto-provisions >
RollerUserDetailsService -- User : loads >
AuthoritiesPopulator -- User : populates authorities for >
RoleAssignmentFilter -- RoleAssignmentRequestWrapper : uses >
RoleAssignmentRequestWrapper -- User : checks roles for >
UISecurityInterceptor -- UISecurityEnforced : enforces >
UIAction -- User : operates on >
UIAction -- Weblog : operates on >
UIActionInterceptor -- UIAction : intercepts >
UserWrapper -- User : wraps >
Weblog -- User : created by >
WebloggerFactory -- Weblogger : provides >
Weblogger -- UserManager : provides >
Weblogger -- OAuthManager : provides >
AccessTokenServlet -- OAuthManager : uses >
AuthorizationServlet -- OAuthManager : uses >
RequestTokenServlet -- OAuthManager : uses >
Install -- User : sets up system for >
Setup -- User : configures system for >
Setup -- Weblog : displays >
RollerUserDetails --|> UserDetails

@enduml
```
## SubSystem - III
### UML Class Diagram – Search and Indexing Subsystem (Lucene)


The following PlantUML source was used to generate the class diagram:

```

@startuml Lucene_Search_Indexing

' Styling
skinparam classAttributeIconSize 0
skinparam shadowing false
skinparam backgroundColor #FFFFFF
skinparam class {
    BackgroundColor #F8F8F8
    BorderColor #333333
    ArrowColor #333333
}

' ====================================
' INTERFACES
' ====================================

interface IndexManager <<interface>> {
    +void initialize()
    +void rebuildWeblogIndex()
    +void rebuildWeblogIndex(Weblog website)
    +void removeWeblogIndex(Weblog website)
    +void addEntryIndexOperation(WeblogEntry entry)
    +void addEntryReIndexOperation(WeblogEntry entry)
    +void removeEntryIndexOperation(WeblogEntry entry)
    +SearchResultList search(String term, String weblogHandle, String category, String locale, int pageNum, int entryCount, URLStrategy urlStrategy)
    +boolean isInconsistentAtStartup()
    +void release()
    +void shutdown()
}

interface Runnable <<interface>> {
    +void run()
}

' ====================================
' MAIN MANAGER CLASS
' ====================================

class LuceneIndexManager {
    -IndexReader reader
    -Weblogger roller
    -boolean searchEnabled
    -String indexDir
    -File indexConsistencyMarker
    -boolean inconsistentAtStartup
    -ReadWriteLock rwl
    -{static} Log logger
    
    #LuceneIndexManager(Weblogger roller)
    +void initialize()
    +void rebuildWeblogIndex()
    +void rebuildWeblogIndex(Weblog website)
    +void removeWeblogIndex(Weblog website)
    +void addEntryIndexOperation(WeblogEntry entry)
    +void addEntryReIndexOperation(WeblogEntry entry)
    +void removeEntryIndexOperation(WeblogEntry entry)
    +SearchResultList search(String term, String weblogHandle, String category, String locale, int pageNum, int entryCount, URLStrategy urlStrategy)
    +ReadWriteLock getReadWriteLock()
    +boolean isInconsistentAtStartup()
    +{static} Analyzer getAnalyzer()
    +void release()
    +void shutdown()
    +synchronized void resetSharedReader()
    +synchronized IndexReader getSharedIndexReader()
    +Directory getIndexDirectory()
    -void scheduleIndexOperation(IndexOperation op)
    -void executeIndexOperationNow(IndexOperation op)
    -boolean indexExists()
    -void deleteIndex()
    -void createIndex(Directory dir)
    -{static} Analyzer instantiateAnalyzer()
    -{static} Analyzer instantiateDefaultAnalyzer()
    -{static} SearchResultList convertHitsToEntryList(ScoreDoc[] hits, SearchOperation search, int pageNum, int entryCount, String weblogHandle, boolean websiteSpecificSearch, URLStrategy urlStrategy)
}

' ====================================
' ABSTRACT BASE CLASSES
' ====================================

abstract class IndexOperation {
    #{static} Log logger
    #LuceneIndexManager manager
    -IndexWriter writer
    
    +IndexOperation(LuceneIndexManager manager)
    +void run()
    #Document getDocument(WeblogEntry data)
    #IndexWriter beginWriting()
    #void endWriting()
    #{abstract} void doRun()
}

abstract class ReadFromIndexOperation {
    -{static} Log logger
    
    +ReadFromIndexOperation(LuceneIndexManager mgr)
    +final void run()
}
'
abstract class WriteToIndexOperation {
    -{static} Log logger
    
    +WriteToIndexOperation(LuceneIndexManager mgr)
    +void run()
}

' ====================================
' CONCRETE WRITE OPERATIONS
' ====================================

class AddEntryOperation {
    -{static} Log logger
    -WeblogEntry data
    -Weblogger roller
    
    +AddEntryOperation(Weblogger roller, LuceneIndexManager mgr, WeblogEntry data)
    +void doRun()
}

class ReIndexEntryOperation {
    -{static} Log logger
    -WeblogEntry data
    -Weblogger roller
    
    +ReIndexEntryOperation(Weblogger roller, LuceneIndexManager mgr, WeblogEntry data)
    +void doRun()
}

class RemoveEntryOperation {
    -{static} Log logger
    -WeblogEntry data
    -Weblogger roller
    
    +RemoveEntryOperation(Weblogger roller, LuceneIndexManager mgr, WeblogEntry data)
    +void doRun()
}

class RebuildWebsiteIndexOperation {
    -{static} Log logger
    -Weblog website
    -Weblogger roller
    
    +RebuildWebsiteIndexOperation(Weblogger roller, LuceneIndexManager mgr, Weblog website)
    +void doRun()
}

class RemoveWebsiteIndexOperation {
    -{static} Log logger
    -Weblog website
    -Weblogger roller
    
    +RemoveWebsiteIndexOperation(Weblogger roller, LuceneIndexManager mgr, Weblog website)
    +void doRun()
}

' ====================================
' CONCRETE READ OPERATIONS
' ====================================

class SearchOperation {
    -{static} Log logger
    -{static} String[] SEARCH_FIELDS
    -{static} Sort SORTER
    -IndexSearcher searcher
    -TopFieldDocs searchresults
    -String term
    -String weblogHandle
    -String category
    -String locale
    -String parseError
    
    +SearchOperation(IndexManager mgr)
    +void setTerm(String term)
    +void setWeblogHandle(String weblogHandle)
    +void setCategory(String category)
    +void setLocale(String locale)
    +void doRun()
    +IndexSearcher getSearcher()
    +void setSearcher(IndexSearcher searcher)
    +TopFieldDocs getResults()
    +int getResultsCount()
    +String getParseError()
}

' ====================================
' UTILITY CLASSES
' ====================================

class IndexUtil {
    -IndexUtil()
    +{static} Term getTerm(String field, String input)
}

class FieldConstants {
    +{static} final String ANCHOR
    +{static} final String UPDATED
    +{static} final String ID
    +{static} final String USERNAME
    +{static} final String CATEGORY
    +{static} final String TITLE
    +{static} final String PUBLISHED
    +{static} final String CONTENT
    +{static} final String CONTENT_STORED
    +{static} final String C_CONTENT
    +{static} final String C_EMAIL
    +{static} final String C_NAME
    +{static} final String CONSTANT
    +{static} final String CONSTANT_V
    +{static} final String WEBSITE_HANDLE
    +{static} final String LOCALE
}

' ====================================
' EXTERNAL DOMAIN CLASSES (SIMPLIFIED)
' ====================================

class Weblogger <<external>> {
    +WeblogEntryManager getWeblogEntryManager()
    +WeblogManager getWeblogManager()
    +ThreadManager getThreadManager()
    +void release()
}

class WeblogEntry <<external>> {
    +String getId()
    +String getTitle()
    +String getText()
    +Weblog getWebsite()
    +User getCreator()
    +WeblogCategory getCategory()
    +List<WeblogEntryComment> getComments()
    +Timestamp getPubTime()
    +Timestamp getUpdateTime()
    +String getLocale()
}

class Weblog <<external>> {
    +String getId()
    +String getHandle()
    +String getName()
}

class WeblogEntryComment <<external>> {
    +String getContent()
    +String getEmail()
    +String getName()
}

class WeblogCategory <<external>> {
    +String getName()
}

class User <<external>> {
    +String getUserName()
}

class WeblogEntryManager <<external>> {
    +WeblogEntry getWeblogEntry(String id)
    +List<WeblogEntry> getWeblogEntries(WeblogEntrySearchCriteria criteria)
}

class WeblogManager <<external>> {
    +Weblog getWeblog(String id)
}

class ThreadManager <<external>> {
    +void executeInBackground(Runnable task)
    +void executeInForeground(Runnable task)
}

class URLStrategy <<external>> {
}

class SearchResultList <<external>> {
    +SearchResultList(List results, Set categories, int limit, int offset)
}

class WeblogEntryWrapper <<external>> {
    +{static} WeblogEntryWrapper wrap(WeblogEntry entry, URLStrategy strategy)
}

class WeblogEntrySearchCriteria <<external>> {
    +void setWeblog(Weblog weblog)
    +void setStatus(PubStatus status)
}

class WebloggerConfig <<external>> {
    +{static} String getProperty(String key)
    +{static} boolean getBooleanProperty(String key, boolean defaultValue)
    +{static} int getIntProperty(String key)
}

class WebloggerRuntimeConfig <<external>> {
    +{static} boolean isSiteWideWeblog(String handle)
}

class WebloggerException <<external>> {
}

class InitializationException <<external>> {
}
enum PubStatus <<external>> {
    PUBLISHED
}

class RollerConstants <<external>> {
    +{static} final long SEC_IN_MS
}

' ====================================
' LUCENE LIBRARY CLASSES
' ====================================

class IndexWriter <<Lucene>> {
    +void addDocument(Document doc)
    +void deleteDocuments(Term term)
    +void close()
}

class IndexReader <<Lucene>> {
    +{static} DirectoryReader open(Directory dir)
    +void close()
}

class DirectoryReader <<Lucene>> {
    +{static} DirectoryReader open(Directory dir)
    +{static} boolean indexExists(Directory dir)
}

class IndexSearcher <<Lucene>> {
    +IndexSearcher(IndexReader reader)
    +TopFieldDocs search(Query query, int n, Sort sort)
    +Document doc(int docID)
}

class Document <<Lucene>> {
    +void add(Field field)
    +Field getField(String name)
}

class Field <<Lucene>> {
}

class StringField <<Lucene>> {
    +StringField(String name, String value, Store store)
}

class TextField <<Lucene>> {
    +TextField(String name, String value, Store store)
}

class SortedDocValuesField <<Lucene>> {
    +SortedDocValuesField(String name, BytesRef value)
}

class Term <<Lucene>> {
    +Term(String field, String text)
}

class Query <<Lucene>> {
}

class TermQuery <<Lucene>> {
    +TermQuery(Term term)
}

class BooleanQuery <<Lucene>> {
}

class BooleanQueryBuilder <<Lucene>> {
    +BooleanQuery.Builder add(Query query, BooleanClause.Occur occur)
    +BooleanQuery build()
}

class MultiFieldQueryParser <<Lucene>> {
    +MultiFieldQueryParser(String[] fields, Analyzer analyzer)
    +void setDefaultOperator(Operator op)
    +Query parse(String query)
}

class Analyzer <<Lucene>> {
    +TokenStream tokenStream(String field, Reader reader)
}

class StandardAnalyzer <<Lucene>> {
    +StandardAnalyzer()
}

class LimitTokenCountAnalyzer <<Lucene>> {
    +LimitTokenCountAnalyzer(Analyzer delegate, int maxTokenCount)
}

class TokenStream <<Lucene>> {
    +void reset()
    +boolean incrementToken()
    +CharTermAttribute addAttribute(Class)
}

class CharTermAttribute <<Lucene>> {
    +String toString()
}

class Directory <<Lucene>> {
    +String[] listAll()
}

class FSDirectory <<Lucene>> {
    +{static} FSDirectory open(Path path)
    +String[] listAll()
    +void close()
}

class IndexWriterConfig <<Lucene>> {
    +IndexWriterConfig(Analyzer analyzer)
}

class TopFieldDocs <<Lucene>> {
    +ScoreDoc[] scoreDocs
    +TotalHits totalHits
}

class ScoreDoc <<Lucene>> {
    +int doc
    +float score
}

class Sort <<Lucene>> {
    +Sort(SortField field)
}

class SortField <<Lucene>> {
    +SortField(String field, Type type, boolean reverse)
}

class BytesRef <<Lucene>> {
    +BytesRef(String text)
}

class ReadWriteLock <<Java>> {
    +Lock readLock()
    +Lock writeLock()
}

class ReentrantReadWriteLock <<Java>> {
    +ReentrantReadWriteLock()
    +Lock readLock()
    +Lock writeLock()
}

class File <<Java>> {
    +boolean exists()
    +boolean createNewFile()
    +boolean delete()
}

class Path <<Java>> {
    +{static} Path of(String path)
}

class Files <<Java>> {
    +{static} void delete(Path path)
}

' ====================================
' RELATIONSHIPS - INHERITANCE
' ====================================

IndexManager <|.. LuceneIndexManager : implements
Runnable <|.. IndexOperation : implements

IndexOperation <|-- ReadFromIndexOperation : extends
IndexOperation <|-- WriteToIndexOperation : extends

ReadFromIndexOperation <|-- SearchOperation : extends

WriteToIndexOperation <|-- AddEntryOperation : extends
WriteToIndexOperation <|-- ReIndexEntryOperation : extends
WriteToIndexOperation <|-- RemoveEntryOperation : extends
WriteToIndexOperation <|-- RebuildWebsiteIndexOperation : extends
WriteToIndexOperation <|-- RemoveWebsiteIndexOperation : extends

ReadWriteLock <|.. ReentrantReadWriteLock : implements
Directory <|-- FSDirectory : extends
IndexReader <|-- DirectoryReader : extends
Query <|-- TermQuery : extends
Query <|-- BooleanQuery : extends
Analyzer <|-- StandardAnalyzer : extends
Analyzer <|-- LimitTokenCountAnalyzer : extends
Field <|-- StringField : extends
Field <|-- TextField : extends
Field <|-- SortedDocValuesField : extends

' ====================================
' RELATIONSHIPS - COMPOSITION
' ====================================

LuceneIndexManager *-- "1" ReadWriteLock : rwl
LuceneIndexManager *-- "1" File : indexConsistencyMarker
IndexOperation *-- "1" LuceneIndexManager : manager

' ====================================
' RELATIONSHIPS - AGGREGATION
' ====================================

LuceneIndexManager o-- "0..1" IndexReader : reader
LuceneIndexManager o-- "1" Weblogger : roller
IndexOperation o-- "0..1" IndexWriter : writer

AddEntryOperation o-- "1" WeblogEntry : data
AddEntryOperation o-- "1" Weblogger : roller

ReIndexEntryOperation o-- "1" WeblogEntry : data
ReIndexEntryOperation o-- "1" Weblogger : roller

RemoveEntryOperation o-- "1" WeblogEntry : data
RemoveEntryOperation o-- "1" Weblogger : roller

RebuildWebsiteIndexOperation o-- "0..1" Weblog : website
RebuildWebsiteIndexOperation o-- "1" Weblogger : roller

RemoveWebsiteIndexOperation o-- "1" Weblog : website
RemoveWebsiteIndexOperation o-- "1" Weblogger : roller

SearchOperation o-- "0..1" IndexSearcher : searcher
SearchOperation o-- "0..1" TopFieldDocs : searchresults

' ====================================
' RELATIONSHIPS - ASSOCIATIONS
' ====================================

WeblogEntry "1" --> "1" Weblog : website
WeblogEntry "1" --> "0..1" User : creator
WeblogEntry "1" --> "0..1" WeblogCategory : category
WeblogEntry "1" --> "0..*" WeblogEntryComment : comments

Weblogger "1" --> "1" WeblogEntryManager : manages
Weblogger "1" --> "1" WeblogManager : manages
Weblogger "1" --> "1" ThreadManager : manages

TopFieldDocs "1" --> "0..*" ScoreDoc : scoreDocs

' ====================================
' RELATIONSHIPS - DEPENDENCIES
' ====================================

LuceneIndexManager ..> IndexOperation : creates/schedules
LuceneIndexManager ..> AddEntryOperation : creates
LuceneIndexManager ..> ReIndexEntryOperation : creates
LuceneIndexManager ..> RemoveEntryOperation : creates
LuceneIndexManager ..> RebuildWebsiteIndexOperation : creates
LuceneIndexManager ..> RemoveWebsiteIndexOperation : creates
LuceneIndexManager ..> SearchOperation : creates
LuceneIndexManager ..> Directory : uses
LuceneIndexManager ..> FSDirectory : uses
LuceneIndexManager ..> DirectoryReader : uses
LuceneIndexManager ..> IndexWriter : uses
LuceneIndexManager ..> IndexWriterConfig : uses
LuceneIndexManager ..> Analyzer : uses
LuceneIndexManager ..> StandardAnalyzer : uses
LuceneIndexManager ..> LimitTokenCountAnalyzer : uses
LuceneIndexManager ..> WebloggerConfig : uses
LuceneIndexManager ..> WebloggerRuntimeConfig : uses
LuceneIndexManager ..> SearchResultList : creates
LuceneIndexManager ..> WeblogEntryWrapper : uses
LuceneIndexManager ..> URLStrategy : uses
LuceneIndexManager ..> WebloggerException : throws
LuceneIndexManager ..> InitializationException : throws
LuceneIndexManager ..> Path : uses
LuceneIndexManager ..> Files : uses

IndexOperation ..> Document : creates
IndexOperation ..> Field : uses
IndexOperation ..> StringField : uses
IndexOperation ..> TextField : uses
IndexOperation ..> SortedDocValuesField : uses
IndexOperation ..> IndexWriter : uses
IndexOperation ..> IndexWriterConfig : uses
IndexOperation ..> Analyzer : uses
IndexOperation ..> LimitTokenCountAnalyzer : uses
IndexOperation ..> WeblogEntry : processes
IndexOperation ..> WeblogEntryComment : processes
IndexOperation ..> WeblogCategory : processes
IndexOperation ..> User : processes
IndexOperation ..> Weblog : processes
IndexOperation ..> WebloggerConfig : uses
IndexOperation ..> FieldConstants : uses
IndexOperation ..> BytesRef : uses
IndexOperation ..> Directory : uses

ReadFromIndexOperation ..> LuceneIndexManager : uses
ReadFromIndexOperation ..> ReadWriteLock : uses

WriteToIndexOperation ..> LuceneIndexManager : uses
WriteToIndexOperation ..> ReadWriteLock : uses

AddEntryOperation ..> IndexWriter : uses
AddEntryOperation ..> Document : uses
AddEntryOperation ..> WeblogEntryManager : uses
AddEntryOperation ..> WebloggerException : catches

ReIndexEntryOperation ..> IndexWriter : uses
ReIndexEntryOperation ..> Document : uses
ReIndexEntryOperation ..> Term : uses
ReIndexEntryOperation ..> WeblogEntryManager : uses
ReIndexEntryOperation ..> WebloggerException : catches
ReIndexEntryOperation ..> FieldConstants : uses

RemoveEntryOperation ..> IndexWriter : uses
RemoveEntryOperation ..> Term : uses
RemoveEntryOperation ..> WeblogEntryManager : uses
RemoveEntryOperation ..> WebloggerException : catches
RemoveEntryOperation ..> FieldConstants : uses

RebuildWebsiteIndexOperation ..> IndexWriter : uses
RebuildWebsiteIndexOperation ..> Term : uses
RebuildWebsiteIndexOperation ..> Document : uses
RebuildWebsiteIndexOperation ..> WeblogEntryManager : uses
RebuildWebsiteIndexOperation ..> WeblogManager : uses
RebuildWebsiteIndexOperation ..> WeblogEntrySearchCriteria : uses
RebuildWebsiteIndexOperation ..> WebloggerException : catches
RebuildWebsiteIndexOperation ..> FieldConstants : uses
RebuildWebsiteIndexOperation ..> IndexUtil : uses
RebuildWebsiteIndexOperation ..> PubStatus : uses
RebuildWebsiteIndexOperation ..> RollerConstants : uses

RemoveWebsiteIndexOperation ..> IndexWriter : uses
RemoveWebsiteIndexOperation ..> Term : uses
RemoveWebsiteIndexOperation ..> WeblogManager : uses
RemoveWebsiteIndexOperation ..> WebloggerException : catches
RemoveWebsiteIndexOperation ..> FieldConstants : uses
RemoveWebsiteIndexOperation ..> IndexUtil : uses

SearchOperation ..> IndexReader : uses
SearchOperation ..> IndexSearcher : uses
SearchOperation ..> MultiFieldQueryParser : uses
SearchOperation ..> Query : uses
SearchOperation ..> TermQuery : uses
SearchOperation ..> BooleanQuery : uses
SearchOperation ..> BooleanQueryBuilder : uses
SearchOperation ..> Term : uses
SearchOperation ..> TopFieldDocs : uses
SearchOperation ..> Sort : uses
SearchOperation ..> SortField : uses
SearchOperation ..> Analyzer : uses
SearchOperation ..> FieldConstants : uses
SearchOperation ..> IndexUtil : uses
SearchOperation ..> IndexManager : uses

IndexUtil ..> Term : creates
IndexUtil ..> Analyzer : uses
IndexUtil ..> TokenStream : uses
IndexUtil ..> CharTermAttribute : uses

' ====================================
' NOTES
' ====================================

note right of LuceneIndexManager
  Singleton - Central coordinator for all
  Lucene indexing and search operations.
  Manages index lifecycle, consistency,
  and thread-safe access.
end note

note right of IndexOperation
  Template Method Pattern:
  - getDocument() is common logic
  - doRun() is extension point
  - run() orchestrates execution
end note

note right of ReadFromIndexOperation
  Acquires READ lock before execution.
  Allows concurrent read operations.
  Never modifies index.
end note

note right of WriteToIndexOperation
  Acquires WRITE lock before execution.
  Ensures exclusive access during writes.
  Resets shared reader after completion.
end note

note right of FieldConstants
  Centralized field name constants.
  Prevents magic strings.
  Single source of truth.
end note

note bottom of SearchOperation
  Searches across CONTENT, TITLE, C_CONTENT fields.
  Supports filtering by weblog, category, locale.
  Results sorted by PUBLISHED date (descending).
  Limited to 500 documents.
end note

@enduml
```