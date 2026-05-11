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